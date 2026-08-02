package com.masterbot.engine.model

import com.masterbot.engine.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MasterBotIndexTest {

    @Test
    fun `parses the real index json produced by build_index py`() {
        val index = TestFixtures.loadRealIndex()

        assertEquals(1, index.version)
        assertEquals(6, index.topicCount, "expected the 6 Stage 1 seed topics")
        assertEquals(index.topics.size, index.topicCount)
        assertEquals(index.topics.sumOf { it.cards.size }, index.cardCount)

        val ros2Topic = index.topics.find { it.id == "it/programming/ros2-nodes-topics" }
        assertTrue(ros2Topic != null, "expected the ros2-nodes-topics seed topic to be present")
        assertEquals("it", ros2Topic.pillar)
        assertEquals("programming", ros2Topic.module)
        assertTrue(ros2Topic.cards.any { it.id == "it-prog-ros2-001" })

        val allCardIds = index.topics.flatMap { it.cards }.map { it.id }
        assertEquals(allCardIds.size, allCardIds.toSet().size, "card ids must be globally unique")
    }
}
