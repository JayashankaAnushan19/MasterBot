package com.masterbot.engine

import com.masterbot.engine.model.AdaptationRules
import com.masterbot.engine.model.Card
import kotlin.math.ceil

/** Per-card SRS/weighting state. Persisted by the app (Room) between review sessions. */
data class CardReviewState(
    val cardId: String,
    val easeFactor: Double,
    val intervalDays: Int,
    val weight: Double,
    val repetitions: Int,
    val lastReviewedEpochDay: Long?,
    val dueEpochDay: Long,
)

/** One answered card event, as logged locally by the review screen. */
data class AnswerEvent(
    val cardId: String,
    val correct: Boolean,
    val responseTimeMs: Long,
    val answeredEpochDay: Long,
)

/**
 * Implements rules/adaptation_rules.yaml's `spaced_repetition` + `weighting` sections
 * ("sm2_modified"): every number here is read from [AdaptationRules], never hardcoded.
 *
 * Interpretation of "modified": the ease factor is applied multiplicatively to the
 * interval starting from the *second* correct review onward (the first correct review
 * after a card is new, or after a lapse, always uses initial_interval_days). An
 * incorrect answer resets the card into relearning: interval back to
 * initial_interval_days, repetitions back to 0, and the ease factor is still penalized
 * per on_incorrect so the card comes back weighted as harder from then on.
 */
class SrsEngine(private val rules: AdaptationRules) {

    /** State for a card that has never been reviewed: due immediately, seeded from cards.yaml. */
    fun initialState(card: Card, todayEpochDay: Long): CardReviewState =
        CardReviewState(
            cardId = card.id,
            easeFactor = rules.spacedRepetition.easeFactorDefault,
            intervalDays = 0,
            weight = card.weightSeed,
            repetitions = 0,
            lastReviewedEpochDay = null,
            dueEpochDay = todayEpochDay,
        )

    fun applyAnswer(state: CardReviewState, event: AnswerEvent): CardReviewState {
        require(event.cardId == state.cardId) {
            "AnswerEvent for '${event.cardId}' does not match state for '${state.cardId}'"
        }

        val adjustment = when {
            !event.correct -> rules.weighting.onIncorrect
            event.responseTimeMs < rules.weighting.slowResponseThresholdMs -> rules.weighting.onCorrectFast
            else -> rules.weighting.onCorrectSlow
        }

        val newEase = (state.easeFactor + adjustment.easeDelta)
            .coerceAtLeast(rules.spacedRepetition.easeFactorMin)
        val newWeight = (state.weight + adjustment.weightDelta).coerceAtLeast(0.0)

        val newInterval: Int
        val newReps: Int
        if (!event.correct) {
            newInterval = rules.spacedRepetition.initialIntervalDays
            newReps = 0
        } else {
            newReps = state.repetitions + 1
            newInterval = if (state.repetitions == 0) {
                rules.spacedRepetition.initialIntervalDays
            } else {
                ceil(state.intervalDays * newEase).toInt().coerceAtLeast(1)
            }
        }

        return state.copy(
            easeFactor = newEase,
            weight = newWeight,
            intervalDays = newInterval,
            repetitions = newReps,
            lastReviewedEpochDay = event.answeredEpochDay,
            dueEpochDay = event.answeredEpochDay + newInterval,
        )
    }

    fun isDue(state: CardReviewState, todayEpochDay: Long): Boolean = state.dueEpochDay <= todayEpochDay
}
