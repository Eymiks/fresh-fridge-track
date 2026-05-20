package com.freshtrack.domain.format

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val frenchDateRegex = Regex("""^(\d{1,2})[\/.-](\d{1,2})[\/.-](\d{4})$""")
private val isoDateRegex = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""")

private val frenchMonthsLong = arrayOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre"
)

fun formatDate(date: LocalDate?): String? =
    date?.let { "%02d/%02d/%04d".format(it.dayOfMonth, it.monthNumber, it.year) }

fun formatDateLong(date: LocalDate?): String? =
    date?.let { "${it.dayOfMonth} ${frenchMonthsLong[it.monthNumber - 1]} ${it.year}" }

fun formatInstantDate(instant: Instant?): String? =
    instant?.toLocalDateTime(TimeZone.currentSystemDefault())?.date?.let(::formatDate)

fun formatInstantDateLong(instant: Instant?): String? =
    instant?.toLocalDateTime(TimeZone.currentSystemDefault())?.date?.let(::formatDateLong)

fun parseUserDate(raw: String): LocalDate? {
    val value = raw.trim()
    if (value.isBlank()) return null

    frenchDateRegex.matchEntire(value)?.let { match ->
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val year = match.groupValues[3].toIntOrNull() ?: return null
        return buildDate(year, month, day)
    }

    isoDateRegex.matchEntire(value)?.let { match ->
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val day = match.groupValues[3].toIntOrNull() ?: return null
        return buildDate(year, month, day)
    }

    return null
}

fun normalizeDateInput(raw: String): String =
    parseUserDate(raw)?.let { formatDate(it) } ?: raw

private fun buildDate(year: Int, month: Int, day: Int): LocalDate? =
    runCatching { LocalDate(year, month, day) }.getOrNull()
