package com.ldp.reader.utils

import java.util.Calendar
import java.util.Locale

object ReadingStatsUtils {
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60L * MINUTE_MS
    private const val KEY_TOTAL = "reading_total"
    private const val KEY_DAY_PREFIX = "reading_day_"
    private const val KEY_WEEK_PREFIX = "reading_week_"
    private const val KEY_BOOK_PREFIX = "reading_book_"

    @JvmStatic
    fun recordReading(bookId: String?, startMs: Long, endMs: Long) {
        if (endMs <= startMs) {
            return
        }
        val durationMs = endMs - startMs
        val prefs = SharedPreUtils.getInstance()
        val dayKey = KEY_DAY_PREFIX + currentDayKey(endMs)
        val weekKey = KEY_WEEK_PREFIX + currentWeekKey(endMs)
        prefs.putLong(KEY_TOTAL, prefs.getLong(KEY_TOTAL, 0L) + durationMs)
        prefs.putLong(dayKey, prefs.getLong(dayKey, 0L) + durationMs)
        prefs.putLong(weekKey, prefs.getLong(weekKey, 0L) + durationMs)
        if (bookId != null && bookId.isNotEmpty()) {
            val bookKey = KEY_BOOK_PREFIX + Integer.toHexString(bookId.hashCode())
            prefs.putLong(bookKey, prefs.getLong(bookKey, 0L) + durationMs)
        }
    }

    @JvmStatic
    fun getWeeklyReadingLabel(): String {
        return formatWeeklyReadingLabel(getWeeklyReadingMillis(System.currentTimeMillis()))
    }

    @JvmStatic
    fun getMineReadingLabel(): String {
        val nowMs = System.currentTimeMillis()
        return formatMineReadingLabel(getTotalReadingMillis(), getTodayReadingMillis(nowMs))
    }

    @JvmStatic
    fun getTotalReadingMillis(): Long {
        val nowMs = System.currentTimeMillis()
        val prefs = SharedPreUtils.getInstance()
        val todayMs = getStoredTodayReadingMillis(prefs, nowMs)
        val weeklyMs = getStoredWeeklyReadingMillis(prefs, nowMs)
        val totalMs = prefs.getLong(KEY_TOTAL, 0L)
        return normalizedTotalReadingMillis(totalMs, todayMs, weeklyMs)
    }

    @JvmStatic
    fun getTodayReadingMillis(nowMs: Long): Long {
        val prefs = SharedPreUtils.getInstance()
        val todayMs = getStoredTodayReadingMillis(prefs, nowMs)
        val weeklyMs = getStoredWeeklyReadingMillis(prefs, nowMs)
        val totalMs = normalizedTotalReadingMillis(
            prefs.getLong(KEY_TOTAL, 0L),
            todayMs,
            weeklyMs
        )
        val normalizedWeeklyMs = normalizedWeeklyReadingMillis(totalMs, todayMs, weeklyMs)
        return normalizedTodayReadingMillis(totalMs, normalizedWeeklyMs, todayMs)
    }

    @JvmStatic
    fun getWeeklyReadingMillis(nowMs: Long): Long {
        val prefs = SharedPreUtils.getInstance()
        val todayMs = getStoredTodayReadingMillis(prefs, nowMs)
        val weeklyMs = getStoredWeeklyReadingMillis(prefs, nowMs)
        val totalMs = normalizedTotalReadingMillis(
            prefs.getLong(KEY_TOTAL, 0L),
            todayMs,
            weeklyMs
        )
        return normalizedWeeklyReadingMillis(totalMs, todayMs, weeklyMs)
    }

    @JvmStatic
    fun normalizedTotalReadingMillis(totalMs: Long, todayMs: Long, weeklyMs: Long): Long {
        return Math.max(0L, Math.max(totalMs, Math.max(todayMs, weeklyMs)))
    }

    @JvmStatic
    fun normalizedWeeklyReadingMillis(totalMs: Long, todayMs: Long, weeklyMs: Long): Long {
        val normalizedWeek = Math.max(0L, Math.max(todayMs, weeklyMs))
        return Math.min(Math.max(0L, totalMs), normalizedWeek)
    }

    @JvmStatic
    fun normalizedTodayReadingMillis(totalMs: Long, weeklyMs: Long, todayMs: Long): Long {
        val normalizedToday = Math.max(0L, todayMs)
        return Math.min(Math.min(Math.max(0L, totalMs), Math.max(0L, weeklyMs)), normalizedToday)
    }

    @JvmStatic
    fun formatWeeklyReadingLabel(durationMs: Long): String {
        return "本周读" + formatDurationLabel(durationMs)
    }

    @JvmStatic
    fun formatDurationLabel(durationMs: Long): String {
        val totalMinutes = Math.max(0L, durationMs / MINUTE_MS)
        if (totalMinutes < 60L) {
            return totalMinutes.toString() + "分钟"
        }
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        if (minutes == 0L) {
            return hours.toString() + "小时"
        }
        return hours.toString() + "小时" + minutes + "分钟"
    }

    @JvmStatic
    fun formatMineReadingLabel(totalDurationMs: Long, todayDurationMs: Long): String {
        val totalHours = Math.max(0L, totalDurationMs / HOUR_MS)
        val todayMinutes = Math.max(0L, todayDurationMs / MINUTE_MS)
        return "累计读" + totalHours + "小时 | 今日读" + todayMinutes + "分钟"
    }

    private fun currentDayKey(nowMs: Long): String {
        val calendar = Calendar.getInstance(Locale.CHINA)
        calendar.timeInMillis = nowMs
        return calendar[Calendar.YEAR].toString() + "_" + calendar[Calendar.DAY_OF_YEAR]
    }

    private fun currentWeekKey(nowMs: Long): String {
        val calendar = Calendar.getInstance(Locale.CHINA)
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.minimalDaysInFirstWeek = 4
        calendar.timeInMillis = nowMs
        return calendar[Calendar.YEAR].toString() + "_" + calendar[Calendar.WEEK_OF_YEAR]
    }

    private fun getStoredTodayReadingMillis(prefs: SharedPreUtils, nowMs: Long): Long {
        return prefs.getLong(KEY_DAY_PREFIX + currentDayKey(nowMs), 0L)
    }

    private fun getStoredWeeklyReadingMillis(prefs: SharedPreUtils, nowMs: Long): Long {
        return prefs.getLong(KEY_WEEK_PREFIX + currentWeekKey(nowMs), 0L)
    }
}
