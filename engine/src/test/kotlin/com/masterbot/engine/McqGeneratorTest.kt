package com.masterbot.engine

import com.masterbot.engine.model.Card
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class McqGeneratorTest {

    private fun card(id: String, answer: String, type: String = "qa", options: List<String> = emptyList()) =
        Card(id = id, type = type, question = "q-$id", answer = answer, options = options, weightSeed = 1.0)

    @Test
    fun `already-authored mcq with real options is returned unchanged`() {
        val original = card("x-001", "B", type = "mcq", options = listOf("A", "B", "C", "D"))
        val result = McqGenerator.toMcq(original, distractorPool = emptyList())
        assertEquals(original, result)
    }

    @Test
    fun `qa card gets converted to mcq with the correct answer included`() {
        val target = card("elec-circ-ohmkirch-001", "V = I x R")
        val pool = listOf(
            target,
            card("elec-circ-ohmkirch-002", "P = I^2 x R"),
            card("mech-act-dcmotor-001", "Stall torque is maximum"),
            card("it-prog-ros2-001", "Topics are async, services are sync"),
        )
        val result = McqGenerator.toMcq(target, pool, random = Random(42))

        assertEquals("mcq", result.type)
        assertTrue(target.answer in result.options, "correct answer must always be an option")
        assertEquals(result.options.size, result.options.toSet().size, "no duplicate options")
        assertEquals(4, result.options.size)
    }

    @Test
    fun `options never exceed 1 plus the number of distinct distractor answers available`() {
        val target = card("solo-001", "Only answer")
        // Pool has no other distinct answers -- everything shares the same answer as target.
        val pool = listOf(target, card("solo-002", "Only answer"), card("solo-003", "Only answer"))
        val result = McqGenerator.toMcq(target, pool, random = Random(1))

        assertEquals(listOf("Only answer"), result.options)
    }

    @Test
    fun `a single distinct distractor yields exactly two options`() {
        val target = card("a-001", "Correct")
        val pool = listOf(target, card("a-002", "Wrong"), card("a-003", "Wrong"))
        val result = McqGenerator.toMcq(target, pool, random = Random(7))

        assertEquals(setOf("Correct", "Wrong"), result.options.toSet())
        assertEquals(2, result.options.size)
    }

    @Test
    fun `distractors prefer cards from the same topic family when available`() {
        val target = card("elec-circ-ohmkirch-001", "V = I x R")
        val sameTopic = listOf(
            card("elec-circ-ohmkirch-002", "P = I^2 x R"),
            card("elec-circ-ohmkirch-003", "Series adds resistance"),
            card("elec-circ-ohmkirch-004", "Parallel reduces resistance"),
        )
        val otherTopic = listOf(card("it-prog-ros2-001", "Unrelated answer"))
        val result = McqGenerator.toMcq(target, listOf(target) + sameTopic + otherTopic, random = Random(3))

        val distractorsUsed = result.options.filter { it != target.answer }
        assertTrue(
            distractorsUsed.all { it in sameTopic.map(Card::answer) },
            "with 3 same-topic distractors available, none should come from the unrelated topic",
        )
    }
}
