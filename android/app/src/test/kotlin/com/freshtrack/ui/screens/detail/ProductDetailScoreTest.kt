package com.freshtrack.ui.screens.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductDetailScoreTest {

    @Test
    fun mapsDaysLeftToFreshnessGaugeProgress() {
        assertEquals(0f, freshnessGaugeProgress(-2))
        assertEquals(0.08f, freshnessGaugeProgress(0))
        assertEquals(0.25f, freshnessGaugeProgress(1))
        assertEquals(0.75f, freshnessGaugeProgress(3))
        assertEquals(1f, freshnessGaugeProgress(4))
        assertEquals(1f, freshnessGaugeProgress(30))
    }

    @Test
    fun normalizesLetterScoresOnlyWhenTheyAreRealGrades() {
        assertEquals("A", normalizeVisibleScore("a", VisibleScoreKind.LETTER))
        assertEquals("E", normalizeVisibleScore(" e ", VisibleScoreKind.LETTER))

        assertNull(normalizeVisibleScore(null, VisibleScoreKind.LETTER))
        assertNull(normalizeVisibleScore("", VisibleScoreKind.LETTER))
        assertNull(normalizeVisibleScore("-", VisibleScoreKind.LETTER))
        assertNull(normalizeVisibleScore("N/A", VisibleScoreKind.LETTER))
        assertNull(normalizeVisibleScore("not-applicable", VisibleScoreKind.LETTER))
        assertNull(normalizeVisibleScore("unknown", VisibleScoreKind.LETTER))
        assertNull(normalizeVisibleScore("F", VisibleScoreKind.LETTER))
    }

    @Test
    fun normalizesNovaScoresOnlyWhenTheyAreInRange() {
        assertEquals("1", normalizeVisibleScore("1", VisibleScoreKind.NOVA))
        assertEquals("4", normalizeVisibleScore(" 4 ", VisibleScoreKind.NOVA))

        assertNull(normalizeVisibleScore(null, VisibleScoreKind.NOVA))
        assertNull(normalizeVisibleScore("-", VisibleScoreKind.NOVA))
        assertNull(normalizeVisibleScore("N/A", VisibleScoreKind.NOVA))
        assertNull(normalizeVisibleScore("0", VisibleScoreKind.NOVA))
        assertNull(normalizeVisibleScore("5", VisibleScoreKind.NOVA))
        assertNull(normalizeVisibleScore("A", VisibleScoreKind.NOVA))
    }
}
