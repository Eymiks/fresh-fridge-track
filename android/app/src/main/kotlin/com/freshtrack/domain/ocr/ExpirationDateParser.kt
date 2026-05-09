package com.freshtrack.domain.ocr

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.text.Normalizer

private enum class DatePrecision { DAY, MONTH }

private data class DateCandidate(
    val date: String,
    val precision: DatePrecision
)

private val MONTH_WORDS = mapOf(
    "JANVIER" to 1, "JANV" to 1, "JAN" to 1, "JANUARY" to 1,
    "FEVRIER" to 2, "FEVR" to 2, "FEV" to 2, "FEBRUARY" to 2, "FEB" to 2,
    "MARS" to 3, "MAR" to 3, "MARCH" to 3,
    "AVRIL" to 4, "AVR" to 4, "APRIL" to 4, "APR" to 4,
    "MAI" to 5, "MAY" to 5,
    "JUIN" to 6, "JUN" to 6, "JUNE" to 6,
    "JUILLET" to 7, "JUIL" to 7, "JULY" to 7, "JUL" to 7,
    "AOUT" to 8, "AOU" to 8, "AUGUST" to 8, "AUG" to 8,
    "SEPTEMBRE" to 9, "SEPT" to 9, "SEP" to 9, "SEPTEMBER" to 9,
    "OCTOBRE" to 10, "OCT" to 10, "OCTOBER" to 10,
    "NOVEMBRE" to 11, "NOV" to 11, "NOVEMBER" to 11,
    "DECEMBRE" to 12, "DEC" to 12, "DECEMBER" to 12
)

fun parseExpirationDate(
    rawText: String,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
): String? {
    val normalized = normalizeText(rawText)
    if (normalized.isBlank()) return null

    val corrected = normalized
        .split(Regex("([^A-Z0-9/.\\-\\s])"))
        .joinToString("") { part ->
            if (part.any { it.isDigit() || it in "OoQIl|B" }) correctDigits(part) else part
        }

    val candidates = parseFromText(normalized, today) + parseFromText(corrected, today)

    return candidates
        .distinctBy { it.date }
        .sortedWith(compareBy<DateCandidate> { it.precision }.thenBy { it.date })
        .firstOrNull()
        ?.date
}

private fun parseFromText(text: String, today: LocalDate): List<DateCandidate> {
    val candidates = mutableListOf<DateCandidate>()
    val monthAlt = MONTH_WORDS.keys.sortedByDescending { it.length }.joinToString("|")

    fun addCandidate(year: Int, month: Int, day: Int, precision: DatePrecision = DatePrecision.DAY) {
        if (isValidDate(year, month, day, today)) {
            candidates.add(DateCandidate("%04d-%02d-%02d".format(year, month, day), precision))
        }
    }

    fun addMonthOnly(year: Int, month: Int) {
        if (month in 1..12) addCandidate(year, month, lastDayOfMonth(year, month), DatePrecision.MONTH)
    }

    fun addDayMonth(day: Int, month: Int) {
        val year = nextOccurrenceYear(day, month, today) ?: return
        addCandidate(year, month, day)
    }

    Regex("""\b(\d{1,2})\s*($monthAlt)\s*(\d{2,4})\b""").findAll(text).forEach { m ->
        val day = m.groupValues[1].toIntOrNull() ?: return@forEach
        val month = MONTH_WORDS[m.groupValues[2]] ?: return@forEach
        val year = resolveYear(m.groupValues[3]) ?: return@forEach
        addCandidate(year, month, day)
    }

    Regex("""\b($monthAlt)\s*(\d{2,4})\b""").findAll(text).forEach { m ->
        val month = MONTH_WORDS[m.groupValues[1]] ?: return@forEach
        val year = resolveYear(m.groupValues[2]) ?: return@forEach
        addMonthOnly(year, month)
    }

    Regex("""\b(\d{4})[/.\-\s](\d{1,2})[/.\-\s](\d{1,2})\b""").findAll(text).forEach { m ->
        val year = m.groupValues[1].toIntOrNull() ?: return@forEach
        val month = m.groupValues[2].toIntOrNull() ?: return@forEach
        val day = m.groupValues[3].toIntOrNull() ?: return@forEach
        addCandidate(year, month, day)
    }

    Regex("""\b(\d{1,2})[/.\-\s](\d{1,2})[/.\-\s](\d{2,4})\b""").findAll(text).forEach { m ->
        val day = m.groupValues[1].toIntOrNull() ?: return@forEach
        val month = m.groupValues[2].toIntOrNull() ?: return@forEach
        val year = resolveYear(m.groupValues[3]) ?: return@forEach
        addCandidate(year, month, day)
    }

    Regex("""\b(\d{4})(\d{2})(\d{2})\b""").findAll(text).forEach { m ->
        val year = m.groupValues[1].toIntOrNull() ?: return@forEach
        val month = m.groupValues[2].toIntOrNull() ?: return@forEach
        val day = m.groupValues[3].toIntOrNull() ?: return@forEach
        addCandidate(year, month, day)
    }

    Regex("""\b(?:EXP|DLC|DDM|BB|BEST\s*BEFORE|USE\s*BY)?[:\s]*(\d{2})(\d{2})(\d{2}|\d{4})\b""")
        .findAll(text).forEach { m ->
            val day = m.groupValues[1].toIntOrNull() ?: return@forEach
            val month = m.groupValues[2].toIntOrNull() ?: return@forEach
            val year = resolveYear(m.groupValues[3]) ?: return@forEach
            addCandidate(year, month, day)
        }

    Regex("""(?<![/.\-\d])(?:EXP|DLC|DDM|BB|BEST\s*BEFORE|USE\s*BY)?[:\s]*(\d{1,2})[/.\-\s](\d{1,2})(?!\d)(?![/.\-\s]\d)""")
        .findAll(text).forEach { m ->
            val day = m.groupValues[1].toIntOrNull() ?: return@forEach
            val month = m.groupValues[2].toIntOrNull() ?: return@forEach
            addDayMonth(day, month)
        }

    Regex("""(?<![/.\-\d])(\d{1,2})[/.\-](\d{4})\b""").findAll(text).forEach { m ->
        val month = m.groupValues[1].toIntOrNull() ?: return@forEach
        val year = m.groupValues[2].toIntOrNull() ?: return@forEach
        addMonthOnly(year, month)
    }

    Regex("""\b(\d{4})[/.\-](\d{1,2})\b""").findAll(text).forEach { m ->
        val year = m.groupValues[1].toIntOrNull() ?: return@forEach
        val month = m.groupValues[2].toIntOrNull() ?: return@forEach
        addMonthOnly(year, month)
    }

    return candidates
}

private fun normalizeText(value: String): String =
    stripAccents(value)
        .uppercase()
        .replace("'", " ")
        .replace("’", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun stripAccents(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

private fun correctDigits(value: String): String = value
    .replace(Regex("[OoQ]"), "0")
    .replace(Regex("[Il|]"), "1")
    .replace("B", "8")

private fun resolveYear(raw: String): Int? {
    val year = raw.toIntOrNull() ?: return null
    return if (year < 100) {
        if (year <= 79) 2000 + year else 1900 + year
    } else {
        year
    }
}

private fun nextOccurrenceYear(day: Int, month: Int, today: LocalDate): Int? {
    if (!isValidDayMonth(month, day, today.year)) return null
    val candidate = LocalDate(today.year, month, day)
    return if (candidate >= today) today.year else today.year + 1
}

private fun isValidDate(year: Int, month: Int, day: Int, today: LocalDate): Boolean {
    if (!isValidDayMonth(month, day, year)) return false
    return year in (today.year - 2)..(today.year + 10)
}

private fun isValidDayMonth(month: Int, day: Int, year: Int): Boolean =
    month in 1..12 && day in 1..lastDayOfMonth(year, month)

private fun lastDayOfMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else -> 30
}
