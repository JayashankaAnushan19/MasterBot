package com.masterbot.engine.model

import org.yaml.snakeyaml.Yaml

data class WeightAdjustment(
    val easeDelta: Double,
    val weightDelta: Double,
)

data class SpacedRepetitionRules(
    val algorithm: String,
    val initialIntervalDays: Int,
    val easeFactorDefault: Double,
    val easeFactorMin: Double,
)

data class WeightingRules(
    val onCorrectFast: WeightAdjustment,
    val onCorrectSlow: WeightAdjustment,
    val onIncorrect: WeightAdjustment,
    val slowResponseThresholdMs: Long,
)

data class MasteryTier(
    val avgWeightBelow: Double,
    val minCardsReviewed: Int,
)

data class WeakModuleTrigger(
    /** Threshold extracted from the declarative `condition` string, e.g. "avg_weight_over_last_n_days >= 0.6". */
    val avgWeightAtLeast: Double,
    val windowDays: Int,
    val boostMultiplier: Double,
)

data class ModuleHealthRules(
    val weakModuleTrigger: WeakModuleTrigger,
    val bronze: MasteryTier,
    val silver: MasteryTier,
    val gold: MasteryTier,
)

data class DailyTaskGenerationRules(
    val reviewCardsCount: Int,
    val newConceptCount: Int,
    val bonusTaskEnabled: Boolean,
    val listeningSessionEnabled: Boolean,
)

/** Parsed but not yet consumed by the engine — reserved for Stage 6 (rewards/streaks). */
data class RewardsRules(
    val coinsPerCorrect: Int,
    val coinsPerFastCorrectBonus: Int,
    val streakMultiplierPerWeek: Double,
    val streakMultiplierCap: Double,
)

data class AdaptationRules(
    val version: Int,
    val spacedRepetition: SpacedRepetitionRules,
    val weighting: WeightingRules,
    val moduleHealth: ModuleHealthRules,
    val dailyTaskGeneration: DailyTaskGenerationRules,
    val rewards: RewardsRules,
) {
    companion object {
        private val CONDITION_THRESHOLD_RE = Regex(""">=\s*([0-9]*\.?[0-9]+)""")

        /**
         * Parses rules/adaptation_rules.yaml. This is the ONLY place SRS/weighting
         * numbers should ever be read from — never hardcode these values elsewhere.
         */
        @Suppress("UNCHECKED_CAST")
        fun parse(yamlText: String): AdaptationRules {
            val root = Yaml().load<Map<String, Any>>(yamlText)

            val sr = root["spaced_repetition"] as Map<String, Any>
            val spacedRepetition = SpacedRepetitionRules(
                algorithm = sr["algorithm"] as String,
                initialIntervalDays = (sr["initial_interval_days"] as Number).toInt(),
                easeFactorDefault = (sr["ease_factor_default"] as Number).toDouble(),
                easeFactorMin = (sr["ease_factor_min"] as Number).toDouble(),
            )

            val w = root["weighting"] as Map<String, Any>
            fun adjustment(key: String): WeightAdjustment {
                val entry = w[key] as Map<String, Any>
                return WeightAdjustment(
                    easeDelta = (entry["ease_delta"] as Number).toDouble(),
                    weightDelta = (entry["weight_delta"] as Number).toDouble(),
                )
            }
            val weighting = WeightingRules(
                onCorrectFast = adjustment("on_correct_fast"),
                onCorrectSlow = adjustment("on_correct_slow"),
                onIncorrect = adjustment("on_incorrect"),
                slowResponseThresholdMs = (w["slow_response_threshold_ms"] as Number).toLong(),
            )

            val mh = root["module_health"] as Map<String, Any>
            val trigger = mh["weak_module_trigger"] as Map<String, Any>
            val condition = trigger["condition"] as String
            val threshold = CONDITION_THRESHOLD_RE.find(condition)?.groupValues?.get(1)?.toDouble()
                ?: error("module_health.weak_module_trigger.condition must contain a '>= <number>' clause: $condition")
            val weakModuleTrigger = WeakModuleTrigger(
                avgWeightAtLeast = threshold,
                windowDays = (trigger["window_days"] as Number).toInt(),
                boostMultiplier = (trigger["boost_multiplier"] as Number).toDouble(),
            )
            val thresholds = mh["mastery_thresholds"] as Map<String, Any>
            fun tier(key: String): MasteryTier {
                val entry = thresholds[key] as Map<String, Any>
                return MasteryTier(
                    avgWeightBelow = (entry["avg_weight_below"] as Number).toDouble(),
                    minCardsReviewed = (entry["min_cards_reviewed"] as Number).toInt(),
                )
            }
            val moduleHealth = ModuleHealthRules(
                weakModuleTrigger = weakModuleTrigger,
                bronze = tier("bronze"),
                silver = tier("silver"),
                gold = tier("gold"),
            )

            val dtg = root["daily_task_generation"] as Map<String, Any>
            val dailyTaskGeneration = DailyTaskGenerationRules(
                reviewCardsCount = (dtg["review_cards_count"] as Number).toInt(),
                newConceptCount = (dtg["new_concept_count"] as Number).toInt(),
                bonusTaskEnabled = dtg["bonus_task_enabled"] as Boolean,
                listeningSessionEnabled = dtg["listening_session_enabled"] as Boolean,
            )

            val r = root["rewards"] as Map<String, Any>
            val rewards = RewardsRules(
                coinsPerCorrect = (r["coins_per_correct"] as Number).toInt(),
                coinsPerFastCorrectBonus = (r["coins_per_fast_correct_bonus"] as Number).toInt(),
                streakMultiplierPerWeek = (r["streak_multiplier_per_week"] as Number).toDouble(),
                streakMultiplierCap = (r["streak_multiplier_cap"] as Number).toDouble(),
            )

            return AdaptationRules(
                version = (root["version"] as Number).toInt(),
                spacedRepetition = spacedRepetition,
                weighting = weighting,
                moduleHealth = moduleHealth,
                dailyTaskGeneration = dailyTaskGeneration,
                rewards = rewards,
            )
        }
    }
}
