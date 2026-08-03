package com.masterbot.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TopicEntity::class,
        CardEntity::class,
        CardStateEntity::class,
        UserProgressEntity::class,
        UserProfileEntity::class,
        GiftRedemptionEntity::class,
        QuizStageProgressEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): MasterBotDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "masterbot.db",
                )
                    // No shipped release yet, so no real user data to protect across
                    // schema bumps -- once Stage 5 ships, replace with real migrations.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
