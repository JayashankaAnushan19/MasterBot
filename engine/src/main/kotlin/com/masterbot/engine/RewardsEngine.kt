package com.masterbot.engine

import com.masterbot.engine.model.RewardsRules
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Implements rules/adaptation_rules.yaml's `rewards` section: coins per
 * answer and the streak day-count/multiplier that scales them. Every number
 * is read from [RewardsRules], never hardcoded.
 */
object RewardsEngine {

    /** Coins earned for one answer. Incorrect answers earn nothing. */
    fun coinsForAnswer(correct: Boolean, fast: Boolean, streakMultiplier: Double, rules: RewardsRules): Int {
        if (!correct) return 0
        val base = rules.coinsPerCorrect + if (fast) rules.coinsPerFastCorrectBonus else 0
        return (base * streakMultiplier).roundToInt()
    }

    /** Multiplier applied to coin rewards, growing with streak length, capped per rules. */
    fun streakMultiplier(currentStreakDays: Int, rules: RewardsRules): Double {
        val weeks = floor(currentStreakDays / 7.0)
        val multiplier = 1.0 + weeks * rules.streakMultiplierPerWeek
        return multiplier.coerceAtMost(rules.streakMultiplierCap)
    }

    /**
     * Streak after today's daily goal is met. Meeting the goal on the same
     * day it was already recorded is idempotent; meeting it the day right
     * after the last recorded day extends the streak; any bigger gap (or a
     * first-ever goal) restarts the streak at 1.
     */
    fun updatedStreak(previousStreak: Int, lastGoalMetEpochDay: Long?, todayEpochDay: Long): Int = when (lastGoalMetEpochDay) {
        todayEpochDay -> previousStreak
        todayEpochDay - 1 -> previousStreak + 1
        else -> 1
    }
}
