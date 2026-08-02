package com.masterbot.engine.model

import com.masterbot.engine.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptationRulesTest {

    // Every assertion here is a literal transcription of rules/adaptation_rules.yaml
    // (spec section 5). If this test fails, either the file changed (update the test)
    // or the parser broke (fix the parser) -- it must never fail silently either way.
    @Test
    fun `parses the real adaptation_rules yaml exactly`() {
        val rules = TestFixtures.loadRealRules()

        assertEquals(1, rules.version)

        assertEquals("sm2_modified", rules.spacedRepetition.algorithm)
        assertEquals(1, rules.spacedRepetition.initialIntervalDays)
        assertEquals(2.5, rules.spacedRepetition.easeFactorDefault)
        assertEquals(1.3, rules.spacedRepetition.easeFactorMin)

        assertEquals(0.15, rules.weighting.onCorrectFast.easeDelta)
        assertEquals(-0.2, rules.weighting.onCorrectFast.weightDelta)
        assertEquals(0.05, rules.weighting.onCorrectSlow.easeDelta)
        assertEquals(-0.05, rules.weighting.onCorrectSlow.weightDelta)
        assertEquals(-0.2, rules.weighting.onIncorrect.easeDelta)
        assertEquals(0.4, rules.weighting.onIncorrect.weightDelta)
        assertEquals(6000L, rules.weighting.slowResponseThresholdMs)

        assertEquals(0.6, rules.moduleHealth.weakModuleTrigger.avgWeightAtLeast)
        assertEquals(7, rules.moduleHealth.weakModuleTrigger.windowDays)
        assertEquals(1.5, rules.moduleHealth.weakModuleTrigger.boostMultiplier)

        assertEquals(0.5, rules.moduleHealth.bronze.avgWeightBelow)
        assertEquals(10, rules.moduleHealth.bronze.minCardsReviewed)
        assertEquals(0.3, rules.moduleHealth.silver.avgWeightBelow)
        assertEquals(25, rules.moduleHealth.silver.minCardsReviewed)
        assertEquals(0.15, rules.moduleHealth.gold.avgWeightBelow)
        assertEquals(50, rules.moduleHealth.gold.minCardsReviewed)

        assertEquals(8, rules.dailyTaskGeneration.reviewCardsCount)
        assertEquals(1, rules.dailyTaskGeneration.newConceptCount)
        assertEquals(true, rules.dailyTaskGeneration.bonusTaskEnabled)
        assertEquals(true, rules.dailyTaskGeneration.listeningSessionEnabled)

        assertEquals(10, rules.rewards.coinsPerCorrect)
        assertEquals(5, rules.rewards.coinsPerFastCorrectBonus)
        assertEquals(0.1, rules.rewards.streakMultiplierPerWeek)
        assertEquals(2.0, rules.rewards.streakMultiplierCap)
    }
}
