package com.masterbot.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.masterbot.app.MasterBotApplication
import com.masterbot.app.data.db.AppDatabase
import com.masterbot.app.data.db.CardStateEntity
import com.masterbot.app.data.sync.RepoSync
import com.masterbot.app.data.sync.SyncState
import com.masterbot.engine.CardReviewState
import com.masterbot.engine.MasteryTier
import com.masterbot.engine.ModuleHealth
import com.masterbot.engine.model.ModuleHealthRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TopicNode(
    val topicId: String,
    val title: String,
    val tier: MasteryTier,
    val completed: Boolean,
    val locked: Boolean,
    val answeredCount: Int,
    val totalCount: Int,
)
data class PillarSection(val pillar: String, val topics: List<TopicNode>)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class SyncFailed(val message: String) : HomeUiState
    data class Ready(
        val totalCoins: Int,
        val currentStreak: Int,
        val longestStreak: Int,
        val avatarTier: Int, // 1..5
        val pillars: List<PillarSection>,
    ) : HomeUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.get(application).dao()
    private val repoSync = RepoSync(application)
    private val syncCoordinator = (application as MasterBotApplication).syncCoordinator

    /** Home renders its own update-available dialog off this; Loading/Error also come from here. */
    val syncState: StateFlow<SyncState> = syncCoordinator.state

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            syncCoordinator.state.collect { state ->
                when (state) {
                    is SyncState.Syncing -> _uiState.value = HomeUiState.Loading
                    is SyncState.Error -> _uiState.value = HomeUiState.SyncFailed(state.message)
                    // Ready or UpdateAvailable: local content is present either way, load it.
                    // (UpdateAvailable just means the *dialog* should also show, handled separately.)
                    else -> loadDashboard()
                }
            }
        }
    }

    /** Re-reads local data only -- no network. Safe/cheap to call on every screen resume. */
    fun refresh() {
        if (syncCoordinator.state.value !is SyncState.Syncing) {
            viewModelScope.launch { loadDashboard() }
        }
    }

    fun retrySync() = syncCoordinator.applyPendingUpdate()
    fun pullUpdate() = syncCoordinator.applyPendingUpdate()
    fun dismissUpdate() = syncCoordinator.dismissUpdate()

    private suspend fun loadDashboard() {
        val rules = repoSync.currentRules().moduleHealth
        val progress = dao.userProgress()
        val topics = dao.allTopics()
        val cards = dao.allCards()
        val states = dao.allCardStates().associateBy { it.cardId }

        val cardsByTopic = cards.groupBy { it.topicId }
        val topicsByModule = topics.groupBy { it.module }

        val tierByModule = topicsByModule.mapValues { (_, topicsInModule) ->
            val moduleCardIds = topicsInModule.flatMap { t -> cardsByTopic[t.id].orEmpty().map { it.id } }
            val moduleStates = moduleCardIds.mapNotNull { states[it] }
            val reviewedCount = moduleStates.count { it.repetitions > 0 }
            masteryTierFor(moduleStates, reviewedCount, rules)
        }

        // "Complete" (per the user's spec, right or wrong doesn't matter): every card in
        // the topic has been answered at least once.
        fun answeredCount(topicId: String): Int =
            cardsByTopic[topicId].orEmpty().count { (states[it.id]?.repetitions ?: 0) > 0 }
        fun totalCount(topicId: String): Int = cardsByTopic[topicId].orEmpty().size
        fun isTopicComplete(topicId: String): Boolean {
            val total = totalCount(topicId)
            return total > 0 && answeredCount(topicId) == total
        }

        val pillars = topics
            .groupBy { it.pillar }
            .toSortedMap()
            .map { (pillar, topicsInPillar) ->
                val sortedTopics = topicsInPillar.sortedBy { it.id }
                var previousComplete = true // first topic in a pillar is always unlocked
                val nodes = sortedTopics.map { topic ->
                    val completed = isTopicComplete(topic.id)
                    val locked = !previousComplete
                    previousComplete = completed
                    TopicNode(
                        topicId = topic.id,
                        title = prettifySlug(topic.topic),
                        tier = tierByModule[topic.module] ?: MasteryTier.NONE,
                        completed = completed,
                        locked = locked,
                        answeredCount = answeredCount(topic.id),
                        totalCount = totalCount(topic.id),
                    )
                }
                PillarSection(pillar = pillar, topics = nodes)
            }

        val moduleTiers = tierByModule.values.toList()
        val badgeScore = moduleTiers.sumOf { it.weight() }
        val maxScore = (moduleTiers.size * 3).coerceAtLeast(1)
        val avatarTier = avatarTierFor(badgeScore.toDouble() / maxScore)

        _uiState.value = HomeUiState.Ready(
            totalCoins = progress?.totalCoins ?: 0,
            currentStreak = progress?.currentStreak ?: 0,
            longestStreak = progress?.longestStreak ?: 0,
            avatarTier = avatarTier,
            pillars = pillars,
        )
    }

    private fun masteryTierFor(
        states: List<CardStateEntity>,
        reviewedCount: Int,
        rules: ModuleHealthRules,
    ): MasteryTier {
        val engineStates = states.map {
            CardReviewState(
                cardId = it.cardId,
                easeFactor = it.easeFactor,
                intervalDays = it.intervalDays,
                weight = it.weight,
                repetitions = it.repetitions,
                lastReviewedEpochDay = it.lastReviewedEpochDay,
                dueEpochDay = it.dueEpochDay,
            )
        }
        return ModuleHealth.masteryTier(engineStates, reviewedCount, rules)
    }
}

private fun MasteryTier.weight(): Int = when (this) {
    MasteryTier.NONE -> 0
    MasteryTier.BRONZE -> 1
    MasteryTier.SILVER -> 2
    MasteryTier.GOLD -> 3
}

private fun avatarTierFor(ratio: Double): Int = when {
    ratio <= 0.0 -> 1
    ratio <= 0.25 -> 2
    ratio <= 0.5 -> 3
    ratio <= 0.75 -> 4
    else -> 5
}

private fun prettifySlug(slug: String): String =
    slug.split("-").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
