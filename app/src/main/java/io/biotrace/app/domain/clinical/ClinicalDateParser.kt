package io.biotrace.app.domain.clinical

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object ClinicalDateParser {

    private val DATE_FORMATTERS = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE, // 2026-10-28
        DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ROOT),
        DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("d/M/yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.US),
        DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.US),
        DateTimeFormatter.ofPattern("dd-MMMM-yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MMMM dd yyyy", Locale.US),
        DateTimeFormatter.ofPattern("dd/MMM/yyyy", Locale.US)
    )

    private val DATETIME_FORMATTERS = listOf(
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.US),
        DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a", Locale.US),
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", Locale.ROOT),
        DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a", Locale.US),
        DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a", Locale.US)
    )

    /**
     * Parses a raw date string extracted from a diagnostic report into an Instant.
     * Normalizes date-only strings to 12:00:00 UTC on that day to avoid timezone date shifts.
     * If parsing fails or input is null/blank, returns fallbackInstant.
     */
    fun parseToInstant(rawDate: String?, fallbackInstant: Instant = Instant.now()): Instant {
        if (rawDate.isNullOrBlank()) return fallbackInstant
        val cleaned = rawDate.trim()

        // 1. Try ISO instant directly (e.g. 2026-10-28T09:30:00Z)
        try {
            return Instant.parse(cleaned)
        } catch (_: Exception) {}

        // 2. Try date-time formatters
        for (formatter in DATETIME_FORMATTERS) {
            try {
                val ldt = LocalDateTime.parse(cleaned, formatter)
                return ldt.toInstant(ZoneOffset.UTC)
            } catch (_: DateTimeParseException) {}
        }

        // 3. Try date-only formatters
        for (formatter in DATE_FORMATTERS) {
            try {
                val ld = LocalDate.parse(cleaned, formatter)
                return ld.atTime(12, 0).toInstant(ZoneOffset.UTC)
            } catch (_: DateTimeParseException) {}
        }

        // 4. Try regex extraction for embedded dates
        val isoRegex = Regex("\\b(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})\\b")
        isoRegex.find(cleaned)?.let { match ->
            val year = match.groupValues[1].toInt()
            val month = match.groupValues[2].toInt()
            val day = match.groupValues[3].toInt()
            try {
                val ld = LocalDate.of(year, month, day)
                return ld.atTime(12, 0).toInstant(ZoneOffset.UTC)
            } catch (_: Exception) {}
        }

        val dmyRegex = Regex("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4})\\b")
        dmyRegex.find(cleaned)?.let { match ->
            val first = match.groupValues[1].toInt()
            val second = match.groupValues[2].toInt()
            val year = match.groupValues[3].toInt()
            try {
                // If second > 12, first is month; if first > 12, second is month; default first=day, second=month
                val (day, month) = if (second > 12 && first <= 12) {
                    Pair(second, first)
                } else {
                    Pair(first, second)
                }
                val ld = LocalDate.of(year, month, day)
                return ld.atTime(12, 0).toInstant(ZoneOffset.UTC)
            } catch (_: Exception) {}
        }

        return fallbackInstant
    }

    /**
     * Converts an Instant to an ISO date string (YYYY-MM-DD) in UTC.
     */
    fun toIsoDateString(instant: Instant): String {
        return instant.atZone(ZoneOffset.UTC).toLocalDate().toString()
    }

    /**
     * Formats an Instant into human-readable date (e.g. "Oct 28, 2026").
     */
    fun formatDisplayDate(instant: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US).withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
}
