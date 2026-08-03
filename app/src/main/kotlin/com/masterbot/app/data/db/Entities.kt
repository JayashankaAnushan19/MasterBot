package com.masterbot.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mirrors one topic entry from index.json (subjects/<pillar>/<module>/<topic>/). */
@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val pillar: String,
    val module: String,
    val topic: String,
    val difficultyBase: Int,
    val notesPath: String,
    val hasAudio: Boolean,
)

/** Mirrors one card entry from index.json. Options/tags are stored as JSON text. */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("topicId")],
)
data class CardEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val type: String,
    val question: String,
    val answer: String,
    val optionsJson: String,
    val tagsJson: String,
    val weightSeed: Double,
    /** Optional Android drawable resource name (see schema/card.schema.json's `image` field). */
    val imageResourceName: String? = null,
)

/**
 * Per-card SRS/weighting state, evolved by [com.masterbot.engine.SrsEngine]. Row is
 * created the first time a card is seen (see RepoSync) and updated after every answer.
 */
@Entity(
    tableName = "card_states",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CardStateEntity(
    @PrimaryKey val cardId: String,
    val easeFactor: Double,
    val intervalDays: Int,
    val weight: Double,
    val repetitions: Int,
    val lastReviewedEpochDay: Long?,
    val dueEpochDay: Long,
)

/**
 * Single-row table (always id=0) tracking coins/streak, per
 * [com.masterbot.engine.RewardsEngine]. Updated by ReviewViewModel after
 * each answer and once per day when the daily goal is first met.
 */
@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 0,
    val totalCoins: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastGoalMetEpochDay: Long?,
)

/** Single-row table (always id=0): display name + notification preference. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val notificationsEnabled: Boolean,
)

/**
 * One redemption of a gift from rules/gift_catalog.yaml. `usedEpochDay == null` means
 * still pending -- redeemed with coins but not yet marked as bought/used in real life.
 * Once set, that redemption is final; the catalog item itself can be redeemed again later.
 */
@Entity(tableName = "gift_redemptions")
data class GiftRedemptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val giftId: String,
    val giftName: String,
    val giftEmoji: String,
    val coinCost: Int,
    val claimedEpochDay: Long,
    val usedEpochDay: Long?,
)

/**
 * One row per Quiz Challenges stage (see [com.masterbot.engine.McqGenerator]). A stage
 * unlocks the next once completed at least once, right or wrong doesn't matter -- same
 * philosophy as topic completion on Home.
 */
@Entity(tableName = "quiz_stage_progress")
data class QuizStageProgressEntity(
    @PrimaryKey val stageIndex: Int,
    val timesCompleted: Int,
    val lastCompletedEpochDay: Long?,
)
