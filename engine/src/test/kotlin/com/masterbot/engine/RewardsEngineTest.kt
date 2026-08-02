package com.masterbot.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class RewardsEngineTest {

    // rewards: coins_per_correct=10, coins_per_fast_correct_bonus=5,
    // streak_multiplier_per_week=0.1, streak_multiplier_cap=2.0
    private val rules = TestFixtures.loadRealRules().rewards

    @Test
    fun `incorrect answers earn nothing`() {
        assertEquals(0, RewardsEngine.coinsForAnswer(correct = false, fast = true, streakMultiplier = 1.0, rules = rules))
        assertEquals(0, RewardsEngine.coinsForAnswer(correct = false, fast = false, streakMultiplier = 2.0, rules = rules))
    }

    @Test
    fun `correct fast answer earns base plus fast bonus`() {
        assertEquals(15, RewardsEngine.coinsForAnswer(correct = true, fast = true, streakMultiplier = 1.0, rules = rules))
    }

    @Test
    fun `correct slow answer earns base only`() {
        assertEquals(10, RewardsEngine.coinsForAnswer(correct = true, fast = false, streakMultiplier = 1.0, rules = rules))
    }

    @Test
    fun `streak multiplier scales and rounds coin totals`() {
        // 15 * 1.5 = 22.5 -> rounds to 23
        assertEquals(23, RewardsEngine.coinsForAnswer(correct = true, fast = true, streakMultiplier = 1.5, rules = rules))
    }

    @Test
    fun `streak multiplier is 1x with no streak`() {
        assertEquals(1.0, RewardsEngine.streakMultiplier(currentStreakDays = 0, rules = rules))
        assertEquals(1.0, RewardsEngine.streakMultiplier(currentStreakDays = 6, rules = rules))
    }

    @Test
    fun `streak multiplier increases per full week`() {
        assertEquals(1.1, RewardsEngine.streakMultiplier(currentStreakDays = 7, rules = rules), absoluteTolerance = 1e-9)
        assertEquals(1.1, RewardsEngine.streakMultiplier(currentStreakDays = 13, rules = rules), absoluteTolerance = 1e-9)
        assertEquals(1.2, RewardsEngine.streakMultiplier(currentStreakDays = 14, rules = rules), absoluteTolerance = 1e-9)
    }

    @Test
    fun `streak multiplier never exceeds the configured cap`() {
        assertEquals(rules.streakMultiplierCap, RewardsEngine.streakMultiplier(currentStreakDays = 700, rules = rules))
    }

    @Test
    fun `streak is unchanged when the goal was already recorded today`() {
        assertEquals(5, RewardsEngine.updatedStreak(previousStreak = 5, lastGoalMetEpochDay = 100L, todayEpochDay = 100L))
    }

    @Test
    fun `streak extends when the goal was met exactly yesterday`() {
        assertEquals(6, RewardsEngine.updatedStreak(previousStreak = 5, lastGoalMetEpochDay = 99L, todayEpochDay = 100L))
    }

    @Test
    fun `streak resets after a gap of more than one day`() {
        assertEquals(1, RewardsEngine.updatedStreak(previousStreak = 5, lastGoalMetEpochDay = 90L, todayEpochDay = 100L))
    }

    @Test
    fun `first ever goal starts the streak at 1`() {
        assertEquals(1, RewardsEngine.updatedStreak(previousStreak = 0, lastGoalMetEpochDay = null, todayEpochDay = 100L))
    }
}

private fun assertEquals(expected: Double, actual: Double, absoluteTolerance: Double) {
    kotlin.test.assertTrue(
        kotlin.math.abs(expected - actual) <= absoluteTolerance,
        "expected $expected but was $actual (tolerance $absoluteTolerance)",
    )
}
