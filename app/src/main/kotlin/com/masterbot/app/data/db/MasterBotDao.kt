package com.masterbot.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface MasterBotDao {

    @Upsert
    suspend fun upsertTopics(topics: List<TopicEntity>)

    @Upsert
    suspend fun upsertCards(cards: List<CardEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCardStatesIfAbsent(states: List<CardStateEntity>)

    @Upsert
    suspend fun upsertCardState(state: CardStateEntity)

    @Query("SELECT id FROM cards")
    suspend fun allCardIds(): List<String>

    @Query("SELECT * FROM topics ORDER BY id")
    suspend fun allTopics(): List<TopicEntity>

    @Query("SELECT * FROM cards")
    suspend fun allCards(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE topicId = :topicId")
    suspend fun cardsForTopic(topicId: String): List<CardEntity>

    @Query("SELECT * FROM card_states")
    suspend fun allCardStates(): List<CardStateEntity>

    // Every read path in this app already treats a missing CardStateEntity row as
    // "never reviewed" (DailyQueueBuilder, ReviewViewModel's initialState() fallback,
    // HomeViewModel's repetitions ?: 0), so deleting rows is a complete, correct reset.
    @Query("DELETE FROM card_states")
    suspend fun deleteAllCardStates()

    @Query("DELETE FROM card_states WHERE cardId IN (SELECT id FROM cards WHERE topicId = :topicId)")
    suspend fun deleteCardStatesForTopic(topicId: String)

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun cardCount(): Int

    @Query("SELECT * FROM user_progress WHERE id = 0")
    suspend fun userProgress(): UserProgressEntity?

    @Upsert
    suspend fun upsertUserProgress(progress: UserProgressEntity)

    @Query("SELECT * FROM user_profile WHERE id = 0")
    suspend fun userProfile(): UserProfileEntity?

    @Upsert
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    @Transaction
    suspend fun replaceContent(topics: List<TopicEntity>, cards: List<CardEntity>, seedStates: List<CardStateEntity>) {
        upsertTopics(topics)
        upsertCards(cards)
        // IGNORE-on-conflict: never clobbers existing review progress for a card that
        // already has state, only seeds state for genuinely new cards from a pull.
        insertCardStatesIfAbsent(seedStates)
    }
}
