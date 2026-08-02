package com.masterbot.engine

import com.masterbot.engine.model.Card
import com.masterbot.engine.model.DailyTaskGenerationRules
import kotlin.math.ceil

data class DailyQueue(
    val reviewCards: List<Card>,
    val newCards: List<Card>,
)

/**
 * Implements rules/adaptation_rules.yaml's `daily_task_generation` section: picks the
 * day's review cards (due, most-overdue and highest-weight first) and new concept
 * cards (never reviewed), boosting the new-concept share for modules flagged weak by
 * [ModuleHealth.isWeakModule]. `bonus_task_enabled` / `listening_session_enabled` are
 * carried through as config for later stages, not acted on here.
 */
object DailyQueueBuilder {

    fun build(
        allCards: List<Card>,
        cardModule: Map<String, String>,
        states: Map<String, CardReviewState>,
        todayEpochDay: Long,
        weakModules: Set<String>,
        rules: DailyTaskGenerationRules,
        weakBoostMultiplier: Double,
    ): DailyQueue {
        val dueReviewCards = allCards
            .filter { card ->
                val state = states[card.id] ?: return@filter false
                state.repetitions > 0 && state.dueEpochDay <= todayEpochDay
            }
            .sortedWith(
                compareBy<Card> { states.getValue(it.id).dueEpochDay }
                    .thenByDescending { states.getValue(it.id).weight }
            )
            .take(rules.reviewCardsCount)

        val neverReviewed = allCards.filter { card ->
            val state = states[card.id]
            state == null || state.repetitions == 0
        }

        val effectiveNewConceptCount = if (weakModules.isNotEmpty()) {
            ceil(rules.newConceptCount * weakBoostMultiplier).toInt()
        } else {
            rules.newConceptCount
        }

        val (fromWeakModules, fromOtherModules) = neverReviewed.partition { card ->
            cardModule[card.id] in weakModules
        }

        val newCards = (fromWeakModules + fromOtherModules).take(effectiveNewConceptCount)

        return DailyQueue(reviewCards = dueReviewCards, newCards = newCards)
    }
}
