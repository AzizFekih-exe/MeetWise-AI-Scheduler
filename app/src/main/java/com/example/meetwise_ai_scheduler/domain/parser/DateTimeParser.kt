package com.example.meetwise_ai_scheduler.domain.parser

import com.example.meetwise_ai_scheduler.domain.model.MeetingIntent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class DateTimeParser {

    private val meetWiseZone = ZoneOffset.ofHours(1)

    fun parse(input: String): MeetingIntent? {
        val cleanInput = input.lowercase(Locale.ROOT).trim()
        
        val dateRange = parseDateRange(cleanInput)
        val explicitTime = parseExplicitTime(cleanInput)
        val timeWindow = if (explicitTime == null) parseTimeWindow(cleanInput) else null
        val time = explicitTime ?: timeWindow?.start
        val duration = parseDuration(cleanInput)
        val date = dateRange?.first
        val endDate = dateRange?.second
        
        // If we couldn't find a date AND a time, we consider it a parse failure
        // so the UI can fall back to the manual structured form.
        if (date == null && time == null) return null

        return MeetingIntent(
            title = extractTitle(cleanInput),
            date = date,
            endDate = endDate,
            time = time,
            timeWindow = timeWindow,
            durationMinutes = duration ?: 60, // Default to 60 minutes if not specified
            rawInput = input
        )
    }

    private fun parseDateRange(input: String): Pair<LocalDate, LocalDate?>? {
        val today = LocalDate.now(meetWiseZone)
        
        return when {
            Regex("""\b(after today|from tomorrow|starting tomorrow)\b""").containsMatchIn(input) -> {
                val startDate = today.plusDays(1)
                startDate to startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            }
            input.contains("after tomorrow") || input.contains("day after tomorrow") -> {
                val startDate = today.plusDays(2)
                val endOfWeek = startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                startDate to endOfWeek
            }
            Regex("""\b(next\s+future|next|future)\s+week\s*days\b|\bnext\s+future\s+weekdays\b|\bnext\s+weekdays\b|\bfuture\s+weekdays\b""").containsMatchIn(input) -> {
                val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                nextMonday to nextMonday.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
            }
            Regex("""\bnext\s+week\b""").containsMatchIn(input) -> {
                val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                nextMonday to nextMonday.plusDays(6)
            }
            Regex("""\b(next|future)\s+month\b""").containsMatchIn(input) -> {
                val firstDay = today.plusMonths(1).withDayOfMonth(1)
                firstDay to firstDay.withDayOfMonth(firstDay.lengthOfMonth())
            }
            Regex("""\bthis\s+month\b""").containsMatchIn(input) -> {
                today to today.withDayOfMonth(today.lengthOfMonth())
            }
            input.contains("today") -> today to null
            input.contains("tomorrow") -> today.plusDays(1) to null
            else -> parseNamedMonth(input, today) ?: parseWeekdayRange(input, today)
        }
    }

    private fun parseNamedMonth(input: String, today: LocalDate): Pair<LocalDate, LocalDate?>? {
        val months = listOf(
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
        )
        val index = months.indexOfFirst { month ->
            Regex("""\b(in\s+|next\s+)?$month\b""").containsMatchIn(input)
        }
        if (index == -1) return null

        val monthNumber = index + 1
        val year = if (monthNumber < today.monthValue) today.year + 1 else today.year
        val firstDay = LocalDate.of(year, monthNumber, 1)
        val startDate = if (firstDay.month == today.month && firstDay.year == today.year) today else firstDay
        return startDate to firstDay.withDayOfMonth(firstDay.lengthOfMonth())
    }

    private fun parseWeekdayRange(input: String, today: LocalDate): Pair<LocalDate, LocalDate?>? {
        val days = mapOf(
            "monday" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY,
            "sunday" to DayOfWeek.SUNDAY
        )

        val matched = days.entries.firstOrNull { (name, _) ->
            Regex("""\b(next\s+|after\s+)?$name\b""").containsMatchIn(input)
        } ?: return null

        val dayName = matched.key
        val day = matched.value

        if (Regex("""\bafter\s+$dayName\b""").containsMatchIn(input)) {
            val baseDate = today.with(TemporalAdjusters.nextOrSame(day))
            val startDate = baseDate.plusDays(1)
            val endOfWeek = baseDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            val endDate = if (startDate <= endOfWeek) endOfWeek else startDate
            return startDate to endDate
        }

        val date = if (Regex("""\bnext\s+$dayName\b""").containsMatchIn(input)) {
            today.with(TemporalAdjusters.next(day))
        } else {
            today.with(TemporalAdjusters.nextOrSame(day))
        }

        return date to null
    }

    private fun parseExplicitTime(input: String): LocalTime? {
        // Regex for: "at 2pm", "at 2:30pm", "at 14:00"
        val timeRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""")
        val match = timeRegex.find(input) ?: return null
        
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 0
        val amPm = match.groupValues[3].lowercase(Locale.ROOT)

        if (amPm.isNotEmpty()) {
            when (amPm) {
                "pm" -> if (hour < 12) hour += 12 // 2pm -> 14, 12pm stays 12
                "am" -> if (hour == 12) hour = 0  // 12am -> 0
            }
        }
        
        return try {
            LocalTime.of(hour, minute)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTimeWindow(input: String): ClosedRange<LocalTime>? {
        return when {
            Regex("""\bnext\s+hour\b|\bin\s+an\s+hour\b""").containsMatchIn(input) -> {
                val start = java.time.LocalDateTime.now(meetWiseZone).plusHours(1).withMinute(0).withSecond(0).withNano(0).toLocalTime()
                start..start
            }
            Regex("""\b(this\s+)?morning\b|\bin\s+the\s+morning\b""").containsMatchIn(input) -> LocalTime.of(8, 0)..LocalTime.of(11, 0)
            Regex("""\bnoon\b""").containsMatchIn(input) -> LocalTime.of(12, 0)..LocalTime.of(13, 0)
            Regex("""\b(this\s+)?afternoon\b|\bin\s+the\s+afternoon\b""").containsMatchIn(input) -> LocalTime.of(13, 0)..LocalTime.of(17, 0)
            Regex("""\b(this\s+)?evening\b|\bin\s+the\s+evening\b""").containsMatchIn(input) -> LocalTime.of(17, 0)..LocalTime.of(20, 0)
            Regex("""\btonight\b|\b(this\s+)?night\b|\bat\s+night\b|\bin\s+the\s+night\b""").containsMatchIn(input) -> LocalTime.of(20, 0)..LocalTime.of(23, 0)
            else -> null
        }
    }

    private fun parseDuration(input: String): Int? {
        // Regex for: "for 1 hour", "for 30 mins"
        val durationRegex = Regex("""for\s+(\d+)\s*(hour|hr|min|minute)s?""")
        val match = durationRegex.find(input) ?: return null
        
        val value = match.groupValues[1].toInt()
        val unit = match.groupValues[2]
        
        return if (unit.startsWith("hour") || unit == "hr") {
            value * 60
        } else {
            value
        }
    }

    private fun extractTitle(input: String): String? {
        // Very basic title extraction: everything before "today", "tomorrow", or "at"
        val delimiters = listOf(
            "day after tomorrow",
            "after tomorrow",
            "after today",
            "from tomorrow",
            "starting tomorrow",
            "future week days",
            "next future week days",
            "next future weekdays",
            "future weekdays",
            "next week days",
            "next weekdays",
            "next week",
            "future month",
            "next month",
            "this month",
            "today",
            "tomorrow",
            "next monday",
            "next tuesday",
            "next wednesday",
            "next thursday",
            "next friday",
            "next saturday",
            "next sunday",
            "after monday",
            "after tuesday",
            "after wednesday",
            "after thursday",
            "after friday",
            "after saturday",
            "after sunday",
            "monday",
            "tuesday",
            "wednesday",
            "thursday",
            "friday",
            "saturday",
            "sunday",
            "morning",
            "noon",
            "afternoon",
            "evening",
            "tonight",
            "night",
            "at",
            "for"
        )
        var title = input
        for (delim in delimiters) {
            val index = title.indexOf(delim)
            if (index != -1) {
                title = title.substring(0, index)
            }
        }
        return title.trim()
            .replaceFirstChar { char -> char.titlecase(Locale.ROOT) }
            .takeIf { it.isNotEmpty() }
    }
}
