package com.masterbot.app.ui.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masterbot.app.data.db.AppDatabase
import com.masterbot.app.data.db.CardEntity
import com.masterbot.app.data.db.CardStateEntity
import com.masterbot.app.data.db.UserProgressEntity
import com.masterbot.app.data.sync.RepoSync
import com.masterbot.engine.AnswerEvent
import com.masterbot.engine.CardReviewState
import com.masterbot.engine.DailyQueueBuilder
import com.masterbot.engine.ModuleHealth
import com.masterbot.engine.RewardsEngine
import com.masterbot.engine.SrsEngine
import com.masterbot.engine.model.AdaptationRules
import com.masterbot.engine.model.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDate

sealed interface ReviewUiState {
    data object Syncing : ReviewUiState
    data class SyncFailed(val message: String) : ReviewUiState
    data class Ready(
        val queue: List<Card>,
        val index: Int,
        val revealed: Boolean,
        val lastCoinsEarned: Int? = null,
    ) : ReviewUiState

    /** Today's goal (rules.daily_task_generation) is met, but more cards are available. */
    data class GoalReached(val reviewedToday: Int) : ReviewUiState

    /** Nothing left to practice right now (whole deck for "today", or this one topic). */
    data class AllCaughtUp(val reviewedToday: Int) : ReviewUiState
}

/**
 * @param topicId null = today's goal-based queue (daily review flow). Set = practice
 *   every card in that one topic, ignoring due-dates/goal caps (deliberate practice).
 */
class ReviewViewModel(
    application: Application,
    private val topicId: String? = null,
) : AndroidViewModel(application) {

    private val dao = AppDatabase.get(application).dao()
    private val repoSync = RepoSync(application)

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Syncing)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private lateinit var engine: SrsEngine
    private lateinit var rules: AdaptationRules
    private lateinit var progress: UserProgressEntity
    private var cardShownAtMillis: Long = 0L
    private var reviewedToday: Int = 0
    private var originalGoalSize: Int = 0
    private var goalCreditedThisSession: Boolean = false
    private val answeredIdsToday = mutableSetOf<String>()

    init {
        runSync()
    }

    fun retry() {
        _uiState.value = ReviewUiState.Syncing
        runSync()
    }

    private fun runSync() {
        viewModelScope.launch {
            when (val result = repoSync.syncAndLoad()) {
                is RepoSync.Result.Failure -> _uiState.value = ReviewUiState.SyncFailed(result.message)
                is RepoSync.Result.Success -> {
                    rules = repoSync.currentRules()
                    engine = SrsEngine(rules)
                    progress = dao.userProgress() ?: UserProgressEntity(
                        totalCoins = 0,
                        currentStreak = 0,
                        longestStreak = 0,
                        lastGoalMetEpochDay = null,
                    )
                    if (topicId != null) loadTopicQueue(topicId) else loadTodaysQueue()
                }
            }
        }
    }

    private suspend fun loadTodaysQueue() {
        val cards = dao.allCards()
        val topics = dao.allTopics().associateBy { it.id }
        val states = dao.allCardStates().associateBy { it.cardId }
        val cardModule = cards.associate { it.id to (topics[it.topicId]?.module ?: "") }

        val today = LocalDate.now().toEpochDay()
        val engineStates = states.mapValues { (_, e) -> e.toEngineState() }

        val windowStart = today - rules.moduleHealth.weakModuleTrigger.windowDays
        val weakModules = engineStates.values
            .filter { state -> (state.lastReviewedEpochDay ?: Long.MIN_VALUE) >= windowStart }
            .groupBy { state -> cardModule[state.cardId] ?: "" }
            .filterValues { ModuleHealth.isWeakModule(it, rules.moduleHealth) }
            .keys

        val engineCards = cards.map { it.toEngineCard() }
        val queue = DailyQueueBuilder.build(
            allCards = engineCards,
            cardModule = cardModule,
            states = engineStates,
            todayEpochDay = today,
            weakModules = weakModules,
            rules = rules.dailyTaskGeneration,
            weakBoostMultiplier = rules.moduleHealth.weakModuleTrigger.boostMultiplier,
        )

        val combined = queue.reviewCards + queue.newCards
        originalGoalSize = combined.size
        showQueueOrFinish(combined)
    }

    /** Deliberate single-topic practice: every card in the topic not already answered today. */
    private suspend fun loadTopicQueue(topicId: String) {
        val cards = dao.cardsForTopic(topicId)
            .filter { it.id !in answeredIdsToday }
            .map { it.toEngineCard() }
        originalGoalSize = 0 // topic practice never counts toward the daily-goal streak
        showQueueOrFinish(cards)
    }

    private fun showQueueOrFinish(queue: List<Card>) {
        if (queue.isEmpty()) {
            finishSession()
        } else {
            cardShownAtMillis = System.currentTimeMillis()
            _uiState.value = ReviewUiState.Ready(queue = queue, index = 0, revealed = false)
        }
    }

    /** Called when a queue (today's goal, a topic practice set, or a "keep practicing" batch) runs out. */
    private fun finishSession() {
        viewModelScope.launch {
            val remaining = if (topicId != null) {
                dao.cardsForTopic(topicId).filter { it.id !in answeredIdsToday }.map { it.toEngineCard() }
            } else {
                remainingEligibleCards()
            }
            _uiState.value = if (remaining.isEmpty()) {
                ReviewUiState.AllCaughtUp(reviewedToday)
            } else {
                ReviewUiState.GoalReached(reviewedToday)
            }
        }
    }

    /** More practice beyond today's goal: every due-review or never-reviewed card not already answered today, no cap. */
    fun keepPracticing() {
        viewModelScope.launch {
            val more = if (topicId != null) {
                dao.cardsForTopic(topicId).filter { it.id !in answeredIdsToday }.map { it.toEngineCard() }
            } else {
                remainingEligibleCards()
            }
            showQueueOrFinish(more)
        }
    }

    private suspend fun remainingEligibleCards(): List<Card> {
        val cards = dao.allCards()
        val states = dao.allCardStates().associateBy { it.cardId }
        val today = LocalDate.now().toEpochDay()

        return cards
            .filter { it.id !in answeredIdsToday }
            .filter { card ->
                val state = states[card.id]
                state == null || state.repetitions == 0 || state.dueEpochDay <= today
            }
            .sortedWith(
                compareBy<CardEntity> { states[it.id]?.dueEpochDay ?: today }
                    .thenByDescending { states[it.id]?.weight ?: it.weightSeed }
            )
            .map { it.toEngineCard() }
    }

    fun reveal() {
        val state = _uiState.value as? ReviewUiState.Ready ?: return
        _uiState.value = state.copy(revealed = true, lastCoinsEarned = null)
    }

    fun answer(correct: Boolean) {
        val state = _uiState.value as? ReviewUiState.Ready ?: return
        val card = state.queue[state.index]
        val responseTimeMs = System.currentTimeMillis() - cardShownAtMillis
        val today = LocalDate.now().toEpochDay()

        viewModelScope.launch {
            val previous = dao.allCardStates().find { it.cardId == card.id }?.toEngineState()
                ?: engine.initialState(card, today)
            val updated = engine.applyAnswer(
                previous,
                AnswerEvent(card.id, correct = correct, responseTimeMs = responseTimeMs, answeredEpochDay = today),
            )
            dao.upsertCardState(updated.toEntity())
            answeredIdsToday += card.id
            reviewedToday += 1

            val fast = responseTimeMs < rules.weighting.slowResponseThresholdMs
            val multiplier = RewardsEngine.streakMultiplier(progress.currentStreak, rules.rewards)
            val coinsEarned = RewardsEngine.coinsForAnswer(correct, fast, multiplier, rules.rewards)
            if (coinsEarned > 0) {
                progress = progress.copy(totalCoins = progress.totalCoins + coinsEarned)
                dao.upsertUserProgress(progress)
            }

            if (topicId == null && !goalCreditedThisSession && reviewedToday >= originalGoalSize && originalGoalSize > 0) {
                goalCreditedThisSession = true
                val newStreak = RewardsEngine.updatedStreak(progress.currentStreak, progress.lastGoalMetEpochDay, today)
                progress = progress.copy(
                    currentStreak = newStreak,
                    longestStreak = maxOf(progress.longestStreak, newStreak),
                    lastGoalMetEpochDay = today,
                )
                dao.upsertUserProgress(progress)
            }

            val nextIndex = state.index + 1
            if (nextIndex >= state.queue.size) {
                finishSession()
            } else {
                cardShownAtMillis = System.currentTimeMillis()
                _uiState.value = state.copy(index = nextIndex, revealed = false, lastCoinsEarned = coinsEarned.takeIf { it > 0 })
            }
        }
    }

    class Factory(
        private val application: Application,
        private val topicId: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReviewViewModel(application, topicId) as T
    }
}

private fun CardStateEntity.toEngineState() = CardReviewState(
    cardId = cardId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    weight = weight,
    repetitions = repetitions,
    lastReviewedEpochDay = lastReviewedEpochDay,
    dueEpochDay = dueEpochDay,
)

private fun CardReviewState.toEntity() = CardStateEntity(
    cardId = cardId,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    weight = weight,
    repetitions = repetitions,
    lastReviewedEpochDay = lastReviewedEpochDay,
    dueEpochDay = dueEpochDay,
)

private fun CardEntity.toEngineCard() = Card(
    id = id,
    type = type,
    question = question,
    answer = answer,
    options = Json.decodeFromString(optionsJson),
    tags = Json.decodeFromString(tagsJson),
    weightSeed = weightSeed,
)
