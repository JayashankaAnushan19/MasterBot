package com.masterbot.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class ModuleHealthTest {

    private val rules = TestFixtures.loadRealRules().moduleHealth

    private fun stateWithWeight(weight: Double, cardId: String = "c") = CardReviewState(
        cardId = cardId,
        easeFactor = 2.5,
        intervalDays = 1,
        weight = weight,
        repetitions = 1,
        lastReviewedEpochDay = 0L,
        dueEpochDay = 1L,
    )

    @Test
    fun `module is weak when average weight meets the configured threshold`() {
        // rules threshold is avg_weight_over_last_n_days >= 0.6
        val states = listOf(stateWithWeight(0.7), stateWithWeight(0.5))
        // average = 0.6, boundary should count as weak (>=)
        assertEquals(true, ModuleHealth.isWeakModule(states, rules))
    }

    @Test
    fun `module is not weak when average weight is below the threshold`() {
        val states = listOf(stateWithWeight(0.4), stateWithWeight(0.5))
        assertEquals(false, ModuleHealth.isWeakModule(states, rules))
    }

    @Test
    fun `module with no recent reviews is never flagged weak`() {
        assertEquals(false, ModuleHealth.isWeakModule(emptyList(), rules))
    }

    @Test
    fun `mastery tier requires both low weight and enough cards reviewed`() {
        val goldWeightStates = List(5) { stateWithWeight(0.1, "c$it") } // avg 0.1 < 0.15

        // not enough cards reviewed yet -> no tier, even though weight already qualifies for gold
        assertEquals(MasteryTier.NONE, ModuleHealth.masteryTier(goldWeightStates, totalCardsReviewedInModule = 5, rules = rules))

        // bronze: avg < 0.5, >= 10 reviewed
        assertEquals(MasteryTier.BRONZE, ModuleHealth.masteryTier(goldWeightStates, totalCardsReviewedInModule = 10, rules = rules))

        // silver: avg < 0.3, >= 25 reviewed
        assertEquals(MasteryTier.SILVER, ModuleHealth.masteryTier(goldWeightStates, totalCardsReviewedInModule = 25, rules = rules))

        // gold: avg < 0.15, >= 50 reviewed
        assertEquals(MasteryTier.GOLD, ModuleHealth.masteryTier(goldWeightStates, totalCardsReviewedInModule = 50, rules = rules))
    }

    @Test
    fun `higher average weight caps out at a lower tier even with plenty of reviews`() {
        val mediumWeightStates = List(5) { stateWithWeight(0.35, "c$it") } // < 0.5 but not < 0.3
        assertEquals(MasteryTier.BRONZE, ModuleHealth.masteryTier(mediumWeightStates, totalCardsReviewedInModule = 100, rules = rules))
    }
}
