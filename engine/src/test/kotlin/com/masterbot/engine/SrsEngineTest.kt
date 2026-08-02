package com.masterbot.engine

import com.masterbot.engine.model.Card
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SrsEngineTest {

    private val rules = TestFixtures.loadRealRules()
    private val engine = SrsEngine(rules)

    private val card = Card(
        id = "test-card-1",
        type = "qa",
        question = "q",
        answer = "a",
        weightSeed = 1.0,
    )

    @Test
    fun `initial state is due immediately and seeded from weight_seed`() {
        val state = engine.initialState(card, todayEpochDay = 100L)

        assertEquals(2.5, state.easeFactor)
        assertEquals(1.0, state.weight)
        assertEquals(0, state.repetitions)
        assertEquals(100L, state.dueEpochDay)
        assertTrue(engine.isDue(state, todayEpochDay = 100L))
    }

    @Test
    fun `first correct answer schedules initial_interval_days out regardless of speed`() {
        val state = engine.initialState(card, todayEpochDay = 100L)
        val next = engine.applyAnswer(state, AnswerEvent(card.id, correct = true, responseTimeMs = 2000, answeredEpochDay = 100L))

        assertEquals(1, next.intervalDays)
        assertEquals(101L, next.dueEpochDay)
        assertEquals(1, next.repetitions)
        // fast correct: ease +0.15, weight -0.2
        assertEquals(2.65, next.easeFactor, absoluteTolerance = 1e-9)
        assertEquals(0.8, next.weight, absoluteTolerance = 1e-9)
    }

    @Test
    fun `correct slow answer applies the slower, smaller adjustment`() {
        val state = engine.initialState(card, todayEpochDay = 0L)
        val next = engine.applyAnswer(state, AnswerEvent(card.id, correct = true, responseTimeMs = 6000, answeredEpochDay = 0L))

        // >= slow_response_threshold_ms (6000) counts as slow
        assertEquals(2.55, next.easeFactor, absoluteTolerance = 1e-9)
        assertEquals(0.95, next.weight, absoluteTolerance = 1e-9)
    }

    @Test
    fun `interval grows multiplicatively by ease factor after the second correct review`() {
        var state = engine.initialState(card, todayEpochDay = 0L)
        // rep 1: correct fast -> interval = 1, ease = 2.65
        state = engine.applyAnswer(state, AnswerEvent(card.id, true, 1000, 0L))
        assertEquals(1, state.intervalDays)

        // rep 2: correct fast again -> interval = ceil(1 * newEase)
        state = engine.applyAnswer(state, AnswerEvent(card.id, true, 1000, 1L))
        val easeAfterRep2 = 2.80 // 2.65 + 0.15
        assertEquals(easeAfterRep2, state.easeFactor, absoluteTolerance = 1e-9)
        assertEquals(3, state.intervalDays) // ceil(1 * 2.80) = 3

        // rep 3: correct fast again -> interval = ceil(3 * newEase)
        state = engine.applyAnswer(state, AnswerEvent(card.id, true, 1000, 4L))
        val easeAfterRep3 = 2.95
        assertEquals(easeAfterRep3, state.easeFactor, absoluteTolerance = 1e-9)
        assertEquals(9, state.intervalDays) // ceil(3 * 2.95) = 9
    }

    @Test
    fun `incorrect answer resets to relearning and penalizes ease and weight`() {
        var state = engine.initialState(card, todayEpochDay = 0L)
        state = engine.applyAnswer(state, AnswerEvent(card.id, true, 1000, 0L)) // rep 1
        state = engine.applyAnswer(state, AnswerEvent(card.id, true, 1000, 1L)) // rep 2, interval grows

        val easeBeforeLapse = state.easeFactor
        val lapsed = engine.applyAnswer(state, AnswerEvent(card.id, correct = false, responseTimeMs = 3000, answeredEpochDay = 10L))

        assertEquals(0, lapsed.repetitions)
        assertEquals(rules.spacedRepetition.initialIntervalDays, lapsed.intervalDays)
        assertEquals(10L + rules.spacedRepetition.initialIntervalDays, lapsed.dueEpochDay)
        assertEquals(easeBeforeLapse - 0.2, lapsed.easeFactor, absoluteTolerance = 1e-9)
    }

    @Test
    fun `ease factor never drops below the configured floor`() {
        var state = engine.initialState(card, todayEpochDay = 0L)
        // Hammer it with incorrect answers; on_incorrect ease_delta is -0.2 each time.
        repeat(20) { i ->
            state = engine.applyAnswer(state, AnswerEvent(card.id, correct = false, responseTimeMs = 1000, answeredEpochDay = i.toLong()))
        }
        assertEquals(rules.spacedRepetition.easeFactorMin, state.easeFactor)
    }

    @Test
    fun `weight never drops below zero`() {
        var state = engine.initialState(card.copy(weightSeed = 0.1), todayEpochDay = 0L)
        repeat(10) { i ->
            state = engine.applyAnswer(state, AnswerEvent(card.id, correct = true, responseTimeMs = 100, answeredEpochDay = i.toLong()))
        }
        assertEquals(0.0, state.weight)
    }
}

private fun assertEquals(expected: Double, actual: Double, absoluteTolerance: Double) {
    assertTrue(
        kotlin.math.abs(expected - actual) <= absoluteTolerance,
        "expected $expected but was $actual (tolerance $absoluteTolerance)",
    )
}
