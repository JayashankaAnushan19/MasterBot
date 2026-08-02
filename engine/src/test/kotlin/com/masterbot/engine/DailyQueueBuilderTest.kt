package com.masterbot.engine

import com.masterbot.engine.model.Card
import com.masterbot.engine.model.DailyTaskGenerationRules
import kotlin.test.Test
import kotlin.test.assertEquals

class DailyQueueBuilderTest {

    private val rules = DailyTaskGenerationRules(
        reviewCardsCount = 2,
        newConceptCount = 1,
        bonusTaskEnabled = true,
        listeningSessionEnabled = true,
    )

    private fun card(id: String, weightSeed: Double = 1.0) =
        Card(id = id, type = "qa", question = "q-$id", answer = "a-$id", weightSeed = weightSeed)

    private fun reviewedState(id: String, dueEpochDay: Long, weight: Double) = CardReviewState(
        cardId = id,
        easeFactor = 2.5,
        intervalDays = 1,
        weight = weight,
        repetitions = 1,
        lastReviewedEpochDay = dueEpochDay - 1,
        dueEpochDay = dueEpochDay,
    )

    @Test
    fun `review queue picks due cards, most overdue first, ties broken by weight`() {
        val cards = listOf(card("a"), card("b"), card("c"), card("notdue"))
        val states = mapOf(
            "a" to reviewedState("a", dueEpochDay = 5, weight = 0.9), // most overdue
            "b" to reviewedState("b", dueEpochDay = 8, weight = 0.5),
            "c" to reviewedState("c", dueEpochDay = 8, weight = 0.9), // same due day as b, higher weight
            "notdue" to reviewedState("notdue", dueEpochDay = 20, weight = 1.0), // not due yet
        )

        val queue = DailyQueueBuilder.build(
            allCards = cards,
            cardModule = cards.associate { it.id to "mod" },
            states = states,
            todayEpochDay = 10,
            weakModules = emptySet(),
            rules = rules.copy(reviewCardsCount = 3),
            weakBoostMultiplier = 1.5,
        )

        assertEquals(listOf("a", "c", "b"), queue.reviewCards.map { it.id })
    }

    @Test
    fun `never-reviewed cards are treated as new concepts, not due reviews`() {
        val cards = listOf(card("new1"))
        val queue = DailyQueueBuilder.build(
            allCards = cards,
            cardModule = mapOf("new1" to "mod"),
            states = emptyMap(),
            todayEpochDay = 0,
            weakModules = emptySet(),
            rules = rules,
            weakBoostMultiplier = 1.5,
        )

        assertEquals(emptyList(), queue.reviewCards)
        assertEquals(listOf("new1"), queue.newCards.map { it.id })
    }

    @Test
    fun `new concept count is boosted and prioritized toward weak modules`() {
        val cards = listOf(card("weak1"), card("weak2"), card("strong1"), card("strong2"))
        val cardModule = mapOf("weak1" to "kinematics", "weak2" to "kinematics", "strong1" to "sensors", "strong2" to "sensors")

        val queue = DailyQueueBuilder.build(
            allCards = cards,
            cardModule = cardModule,
            states = emptyMap(),
            todayEpochDay = 0,
            weakModules = setOf("kinematics"),
            rules = rules, // newConceptCount = 1
            weakBoostMultiplier = 1.5, // ceil(1 * 1.5) = 2
        )

        assertEquals(2, queue.newCards.size)
        assertEquals(setOf("weak1", "weak2"), queue.newCards.map { it.id }.toSet())
    }

    @Test
    fun `no weak modules means no boost applied`() {
        val cards = listOf(card("a"), card("b"), card("c"))
        val queue = DailyQueueBuilder.build(
            allCards = cards,
            cardModule = cards.associate { it.id to "mod" },
            states = emptyMap(),
            todayEpochDay = 0,
            weakModules = emptySet(),
            rules = rules, // newConceptCount = 1
            weakBoostMultiplier = 1.5,
        )

        assertEquals(1, queue.newCards.size)
    }
}
