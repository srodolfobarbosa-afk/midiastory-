package com.rdf112.sweep.presentation.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProWordSearchToolsTest {

    @Test
    fun dailyPuzzleIsDeterministicOfflineAndAllPlacedWordsMatchGrid() {
        val fixedNow = 1794643200000L
        val p1 = ProWordSearchTools.generateDailyPuzzle(isPortuguese = true, nowMillis = fixedNow)
        val p2 = ProWordSearchTools.generateDailyPuzzle(isPortuguese = true, nowMillis = fixedNow)

        assertEquals(9, p1.size)
        assertEquals(p1.grid, p2.grid)
        assertEquals(p1.placements, p2.placements)
        assertTrue("Expected at least 5 words placed offline", p1.placements.size >= 5)

        for (placement in p1.placements) {
            val len = maxOf(
                kotlin.math.abs(placement.endRow - placement.startRow),
                kotlin.math.abs(placement.endCol - placement.startCol),
            ) + 1
            val dR = Integer.compare(placement.endRow, placement.startRow)
            val dC = Integer.compare(placement.endCol, placement.startCol)
            val reconstructed = buildString {
                for (i in 0 until len) {
                    append(p1.grid[placement.startRow + dR * i][placement.startCol + dC * i])
                }
            }
            assertEquals(placement.word, reconstructed)
        }
    }

    @Test
    fun findMatchingWordSupportsForwardAndReverseEndpointTaps() {
        val puzzle = ProWordSearchTools.generateRandomPuzzle(isPortuguese = false, seed = 42L)
        val target = puzzle.placements.first()

        val forward = ProWordSearchTools.findMatchingWord(
            puzzle = puzzle,
            r1 = target.startRow,
            c1 = target.startCol,
            r2 = target.endRow,
            c2 = target.endCol,
            alreadyFoundWords = emptySet(),
        )
        assertNotNull(forward)
        assertEquals(target.word, forward!!.word)

        val reverse = ProWordSearchTools.findMatchingWord(
            puzzle = puzzle,
            r1 = target.endRow,
            c1 = target.endCol,
            r2 = target.startRow,
            c2 = target.startCol,
            alreadyFoundWords = emptySet(),
        )
        assertNotNull(reverse)
        assertEquals(target.word, reverse!!.word)
        assertEquals(target.word.length, ProWordSearchTools.cellsForPlacement(target).size)
    }
}
