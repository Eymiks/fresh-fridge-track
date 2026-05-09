package com.freshtrack.domain.ocr

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpirationDateParserTest {
    private val today = LocalDate(2026, 4, 30)

    @Test
    fun parsesFrenchDayMonthYearFormats() {
        assertEquals("2026-08-14", parseExpirationDate("14/08/2026", today))
        assertEquals("2026-08-14", parseExpirationDate("14/08/26", today))
        assertEquals("2026-08-14", parseExpirationDate("BB 14 08 26", today))
        assertEquals("2026-08-14", parseExpirationDate("140826", today))
    }

    @Test
    fun parsesDayMonthAsNextCalendarOccurrence() {
        assertEquals("2026-08-14", parseExpirationDate("14/08", today))
        assertEquals("2027-01-01", parseExpirationDate("01/01", today))
    }

    @Test
    fun parsesMonthOnlyAsLastDayOfMonth() {
        assertEquals("2026-08-31", parseExpirationDate("08/2026", today))
        assertEquals("2026-08-31", parseExpirationDate("AOÛT 2026", today))
    }

    @Test
    fun rejectsImpossibleDates() {
        assertNull(parseExpirationDate("32/08/2026", today))
        assertNull(parseExpirationDate("14/13/2026", today))
        assertNull(parseExpirationDate("31/02", today))
    }

    @Test
    fun rejectsDatesOutsideUsefulProductRange() {
        assertNull(parseExpirationDate("14/08/1986", today))
        assertNull(parseExpirationDate("14/08/2045", today))
    }
}
