package com.masterbot.engine

import com.masterbot.engine.model.Card
import kotlin.random.Random

/**
 * Converts any card into an MCQ presentation by generating multiple-choice options,
 * for Quiz Challenges stages (reinforcement quizzes mixing already-completed cards
 * across pillars -- see rules/adaptation_rules.yaml's daily_task_generation for the
 * SRS-driven flow this complements, not replaces).
 */
object McqGenerator {

    /**
     * If [card] is already an authored mcq with real options, returns it unchanged.
     * Otherwise builds options from the real answer plus up to 3 distractors pulled
     * from [distractorPool]'s distinct answers (excluding the card itself), preferring
     * same-topic distractors when there are enough of them, and copies the result with
     * `type = "mcq"`. Degenerate pools (few/no distinct distractors) just yield fewer
     * options rather than failing -- never crashes on a small pool.
     */
    fun toMcq(card: Card, distractorPool: List<Card>, random: Random = Random.Default): Card {
        if (card.type == "mcq" && card.options.size >= 2) return card

        val candidates = distractorPool.filter { it.id != card.id && it.answer != card.answer }
        val sameTopic = candidates.filter { sameTopicSlug(it.id) == sameTopicSlug(card.id) }

        // Same-topic distractors first (more plausible wrong answers), then fill any
        // remaining slots from the wider pool -- not just a shuffle of everything
        // together, which would give no actual preference.
        val sameTopicAnswers = sameTopic.map { it.answer }.distinct().shuffled(random)
        val otherAnswers = candidates.map { it.answer }.distinct()
            .filterNot { it in sameTopicAnswers }
            .shuffled(random)
        val distractors = (sameTopicAnswers + otherAnswers).take(3)

        val options = (distractors + card.answer).shuffled(random)
        return card.copy(type = "mcq", options = options)
    }

    // Card ids are "<prefix>-<...>-<NNN>", e.g. "elec-circ-ohmkirch-001" -- comparing the
    // id minus its trailing numeric suffix is a cheap same-topic-family heuristic without
    // needing the topic object plumbed all the way through.
    private fun sameTopicSlug(cardId: String): String = cardId.substringBeforeLast('-')
}
