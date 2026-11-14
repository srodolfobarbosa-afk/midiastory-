package com.rdf112.sweep.presentation.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagicRiddlesToolsTest {

    @Test
    fun `21-card trick mathematically identifies EVERY single one of the 21 possible cards`() {
        for (secretCard in TwentyOneCardTrickEngine.DEFAULT_DECK) {
            val engine = TwentyOneCardTrickEngine()
            engine.reset(shuffleOrder = false)

            for (step in 1..3) {
                val cols = engine.dealColumns()
                val colWithCard = cols.indexOfFirst { it.contains(secretCard) }
                assertTrue("Card $secretCard missing in round $step", colWithCard in 0..2)
                val revealed = engine.chooseColumn(colWithCard)
                if (step < 3) {
                    assertNull(revealed)
                } else {
                    assertNotNull(revealed)
                    assertEquals("Trick must reveal $secretCard", secretCard, revealed)
                }
            }
        }
    }

    @Test
    fun `magic dice respects secret illusionist force target and fair random bounds`() {
        repeat(50) {
            val (d1, d2) = MagicDiceEngine.rollTwoDice(secretTargetSum = 7)
            assertTrue(d1 in 1..6)
            assertTrue(d2 in 1..6)
            assertEquals(7, d1 + d2)

            val (r1, r2) = MagicDiceEngine.rollTwoDice(null)
            assertTrue(r1 in 1..6 && r2 in 1..6)
        }
    }

    @Test
    fun `riddle matcher normalizes accents punctuation and casing cleanly`() {
        assertTrue(MagicRiddlesTools.matchesRiddle(0, "O Mapa!!"))
        assertTrue(MagicRiddlesTools.matchesRiddle(0, "Map"))
        assertTrue(MagicRiddlesTools.matchesRiddle(1, "Espaço Livre"))
        assertTrue(MagicRiddlesTools.matchesRiddle(2, "CELULAR"))
        assertFalse(MagicRiddlesTools.matchesRiddle(0, "banana"))
    }
}
