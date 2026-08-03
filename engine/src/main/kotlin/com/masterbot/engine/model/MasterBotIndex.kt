package com.masterbot.engine.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Mirrors a single card entry inside a topic's cards.yaml / index.json. */
@Serializable
data class Card(
    val id: String,
    val type: String,
    val question: String,
    val answer: String,
    val options: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val weightSeed: Double,
    /** Optional Android drawable resource name shown above the question. Not all cards have one. */
    val image: String? = null,
)

/** Mirrors one topic entry in index.json (subjects/<pillar>/<module>/<topic>/). */
@Serializable
data class Topic(
    val id: String,
    val pillar: String,
    val module: String,
    val topic: String,
    val difficultyBase: Int,
    val notesPath: String,
    val hasAudio: Boolean,
    val cards: List<Card>,
)

/** The full, flattened content index the app pulls via JGit and reads for sync. */
@Serializable
data class MasterBotIndex(
    val version: Int,
    val generatedAt: String,
    val topicCount: Int,
    val cardCount: Int,
    val topics: List<Topic>,
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            ignoreUnknownKeys = true
            namingStrategy = kotlinx.serialization.json.JsonNamingStrategy.SnakeCase
        }

        /** Parses the raw index.json text produced by tools/build_index.py. */
        fun parse(indexJsonText: String): MasterBotIndex = json.decodeFromString(indexJsonText)
    }
}
