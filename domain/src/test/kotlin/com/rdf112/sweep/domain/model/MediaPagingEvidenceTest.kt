package com.rdf112.sweep.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPagingEvidenceTest {
    @Test
    fun completeCoverageRequiresAllPagingAndSortArgumentsToBeReportedAndHonored() {
        val evidence = MediaPagingEvidence(true, true, true, true, true)
        assertTrue(evidence.isFullyHonored)
        assertTrue(evidence.canAdvanceToNextOffset)
        assertFalse(evidence.canUseCursorOffsetFallback)
    }

    @Test
    fun cursorFallbackRequiresProviderToExplicitlyIgnoreBothLimitAndOffset() {
        val evidence = MediaPagingEvidence(true, false, false, false, false)
        assertFalse(evidence.isFullyHonored)
        assertTrue(evidence.canUseCursorOffsetFallback)
        assertTrue(evidence.canAdvanceToNextOffset)
    }

    @Test
    fun missingHonoredArgsEvidenceNeverEnablesCursorOffsetGuessing() {
        val evidence = MediaPagingEvidence(false, false, false, false, false)
        assertFalse(evidence.isFullyHonored)
        assertFalse(evidence.canUseCursorOffsetFallback)
        assertFalse(evidence.canAdvanceToNextOffset)
    }

    @Test
    fun confirmedOffsetCanAdvanceButUnconfirmedSortStillPreventsFullCoverage() {
        val evidence = MediaPagingEvidence(true, true, true, false, true)
        assertFalse(evidence.isFullyHonored)
        assertTrue(evidence.canAdvanceToNextOffset)
    }
}
