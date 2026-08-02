package com.masterbot.engine

import com.masterbot.engine.model.ModuleHealthRules

enum class MasteryTier { NONE, BRONZE, SILVER, GOLD }

/**
 * Implements rules/adaptation_rules.yaml's `module_health` section: the weak-module
 * trigger (drives daily-queue boosting) and mastery badge tiers.
 */
object ModuleHealth {

    /**
     * True if this module should be flagged "weak" this cycle, per
     * `weak_module_trigger`: average weight, over cards reviewed within the
     * configured window, at or above the configured threshold.
     *
     * @param recentlyReviewedStates states for cards in this module reviewed within
     *   `window_days` of "today" (the caller filters by [CardReviewState.lastReviewedEpochDay]).
     */
    fun isWeakModule(
        recentlyReviewedStates: List<CardReviewState>,
        rules: ModuleHealthRules,
    ): Boolean {
        if (recentlyReviewedStates.isEmpty()) return false
        val avgWeight = recentlyReviewedStates.map { it.weight }.average()
        return avgWeight >= rules.weakModuleTrigger.avgWeightAtLeast
    }

    /**
     * Highest mastery tier earned by a module, given all of its cards' current state
     * and how many total review events have been logged against it.
     */
    fun masteryTier(
        allStatesInModule: List<CardReviewState>,
        totalCardsReviewedInModule: Int,
        rules: ModuleHealthRules,
    ): MasteryTier {
        if (allStatesInModule.isEmpty()) return MasteryTier.NONE
        val avgWeight = allStatesInModule.map { it.weight }.average()
        return when {
            avgWeight < rules.gold.avgWeightBelow && totalCardsReviewedInModule >= rules.gold.minCardsReviewed ->
                MasteryTier.GOLD
            avgWeight < rules.silver.avgWeightBelow && totalCardsReviewedInModule >= rules.silver.minCardsReviewed ->
                MasteryTier.SILVER
            avgWeight < rules.bronze.avgWeightBelow && totalCardsReviewedInModule >= rules.bronze.minCardsReviewed ->
                MasteryTier.BRONZE
            else -> MasteryTier.NONE
        }
    }
}
