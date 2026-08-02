package com.masterbot.app.data.sync

import android.content.Context
import com.masterbot.app.data.db.AppDatabase
import com.masterbot.app.data.db.CardEntity
import com.masterbot.app.data.db.CardStateEntity
import com.masterbot.app.data.db.TopicEntity
import com.masterbot.engine.SrsEngine
import com.masterbot.engine.model.AdaptationRules
import com.masterbot.engine.model.MasterBotIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jgit.api.Git
import java.io.File
import java.time.LocalDate

/**
 * Stage 4 scope: read-only clone/pull of MasterBot_Repo, straight into Room.
 *
 * This is intentionally NOT the offline-safe sync described in spec section 6 (staging
 * table, merge-on-boundary, push-before-pull) -- that's Stage 5. Nothing here pushes
 * local data anywhere, and there is no in-progress-session protection yet.
 */
class RepoSync(private val context: Context) {

    private val repoDir: File
        get() = File(context.filesDir, "masterbot_repo")

    sealed interface Result {
        data class Success(val topicCount: Int, val cardCount: Int) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun syncAndLoad(): Result = withContext(Dispatchers.IO) {
        try {
            cloneOrPull()

            val indexFile = File(repoDir, "index.json")
            val rulesFile = File(repoDir, "rules/adaptation_rules.yaml")
            val index = MasterBotIndex.parse(indexFile.readText())
            val rules = AdaptationRules.parse(rulesFile.readText())
            val engine = SrsEngine(rules)

            val topics = index.topics.map {
                TopicEntity(
                    id = it.id,
                    pillar = it.pillar,
                    module = it.module,
                    topic = it.topic,
                    difficultyBase = it.difficultyBase,
                    notesPath = it.notesPath,
                    hasAudio = it.hasAudio,
                )
            }

            val cards = index.topics.flatMap { topic ->
                topic.cards.map { card ->
                    CardEntity(
                        id = card.id,
                        topicId = topic.id,
                        type = card.type,
                        question = card.question,
                        answer = card.answer,
                        optionsJson = Json.encodeToString(card.options),
                        tagsJson = Json.encodeToString(card.tags),
                        weightSeed = card.weightSeed,
                    )
                }
            }

            val today = LocalDate.now().toEpochDay()
            val seedStates = index.topics.flatMap { it.cards }.map { card ->
                val initial = engine.initialState(card, today)
                CardStateEntity(
                    cardId = initial.cardId,
                    easeFactor = initial.easeFactor,
                    intervalDays = initial.intervalDays,
                    weight = initial.weight,
                    repetitions = initial.repetitions,
                    lastReviewedEpochDay = initial.lastReviewedEpochDay,
                    dueEpochDay = initial.dueEpochDay,
                )
            }

            AppDatabase.get(context).dao().replaceContent(topics, cards, seedStates)

            Result.Success(topicCount = topics.size, cardCount = cards.size)
        } catch (e: Exception) {
            Result.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    /** Re-reads and re-parses rules/adaptation_rules.yaml from the last synced clone. */
    fun currentRules(): AdaptationRules =
        AdaptationRules.parse(File(repoDir, "rules/adaptation_rules.yaml").readText())

    private fun cloneOrPull() {
        if (File(repoDir, ".git").exists()) {
            Git.open(repoDir).use { git -> git.pull().call() }
        } else {
            repoDir.mkdirs()
            Git.cloneRepository()
                .setURI(REPO_URL)
                .setDirectory(repoDir)
                .call()
                .close()
        }
    }

    companion object {
        // Public repo, zero-auth HTTPS clone per spec section 1/6.
        private const val REPO_URL = "https://github.com/JayashankaAnushan19/MasterBot.git"
    }
}
