package com.ldp.reader.utils;

import java.util.Calendar;
import java.util.Locale;

public final class ReadingStatsUtils {
    private static final long MINUTE_MS = 60_000L;
    private static final long HOUR_MS = 60L * MINUTE_MS;
    private static final String KEY_TOTAL = "reading_total";
    private static final String KEY_DAY_PREFIX = "reading_day_";
    private static final String KEY_WEEK_PREFIX = "reading_week_";
    private static final String KEY_BOOK_PREFIX = "reading_book_";

    private ReadingStatsUtils() {
    }

    public static void recordReading(String bookId, long startMs, long endMs) {
        if (endMs <= startMs) {
            return;
        }
        long durationMs = endMs - startMs;
        SharedPreUtils prefs = SharedPreUtils.getInstance();
        String dayKey = KEY_DAY_PREFIX + currentDayKey(endMs);
        String weekKey = KEY_WEEK_PREFIX + currentWeekKey(endMs);
        prefs.putLong(KEY_TOTAL, prefs.getLong(KEY_TOTAL, 0L) + durationMs);
        prefs.putLong(dayKey, prefs.getLong(dayKey, 0L) + durationMs);
        prefs.putLong(weekKey, prefs.getLong(weekKey, 0L) + durationMs);
        if (bookId != null && !bookId.isEmpty()) {
            String bookKey = KEY_BOOK_PREFIX + Integer.toHexString(bookId.hashCode());
            prefs.putLong(bookKey, prefs.getLong(bookKey, 0L) + durationMs);
        }
    }

    public static String getWeeklyReadingLabel() {
        return formatWeeklyReadingLabel(getWeeklyReadingMillis(System.currentTimeMillis()));
    }

    public static String getMineReadingLabel() {
        long nowMs = System.currentTimeMillis();
        return formatMineReadingLabel(getTotalReadingMillis(), getTodayReadingMillis(nowMs));
    }

    public static long getTotalReadingMillis() {
        long nowMs = System.currentTimeMillis();
        SharedPreUtils prefs = SharedPreUtils.getInstance();
        long todayMs = getStoredTodayReadingMillis(prefs, nowMs);
        long weeklyMs = getStoredWeeklyReadingMillis(prefs, nowMs);
        long totalMs = prefs.getLong(KEY_TOTAL, 0L);
        return normalizedTotalReadingMillis(totalMs, todayMs, weeklyMs);
    }

    public static long getTodayReadingMillis(long nowMs) {
        SharedPreUtils prefs = SharedPreUtils.getInstance();
        long todayMs = getStoredTodayReadingMillis(prefs, nowMs);
        long weeklyMs = getStoredWeeklyReadingMillis(prefs, nowMs);
        long totalMs = normalizedTotalReadingMillis(
                prefs.getLong(KEY_TOTAL, 0L),
                todayMs,
                weeklyMs
        );
        long normalizedWeeklyMs = normalizedWeeklyReadingMillis(totalMs, todayMs, weeklyMs);
        return normalizedTodayReadingMillis(totalMs, normalizedWeeklyMs, todayMs);
    }

    public static long getWeeklyReadingMillis(long nowMs) {
        SharedPreUtils prefs = SharedPreUtils.getInstance();
        long todayMs = getStoredTodayReadingMillis(prefs, nowMs);
        long weeklyMs = getStoredWeeklyReadingMillis(prefs, nowMs);
        long totalMs = normalizedTotalReadingMillis(
                prefs.getLong(KEY_TOTAL, 0L),
                todayMs,
                weeklyMs
        );
        return normalizedWeeklyReadingMillis(totalMs, todayMs, weeklyMs);
    }

    static long normalizedTotalReadingMillis(long totalMs, long todayMs, long weeklyMs) {
        return Math.max(0L, Math.max(totalMs, Math.max(todayMs, weeklyMs)));
    }

    static long normalizedWeeklyReadingMillis(long totalMs, long todayMs, long weeklyMs) {
        long normalizedWeek = Math.max(0L, Math.max(todayMs, weeklyMs));
        return Math.min(Math.max(0L, totalMs), normalizedWeek);
    }

    static long normalizedTodayReadingMillis(long totalMs, long weeklyMs, long todayMs) {
        long normalizedToday = Math.max(0L, todayMs);
        return Math.min(Math.min(Math.max(0L, totalMs), Math.max(0L, weeklyMs)), normalizedToday);
    }

    public static String formatWeeklyReadingLabel(long durationMs) {
        return "本周读" + formatDurationLabel(durationMs);
    }

    public static String formatDurationLabel(long durationMs) {
        long totalMinutes = Math.max(0L, durationMs / MINUTE_MS);
        if (totalMinutes < 60L) {
            return totalMinutes + "分钟";
        }
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (minutes == 0L) {
            return hours + "小时";
        }
        return hours + "小时" + minutes + "分钟";
    }

    public static String formatMineReadingLabel(long totalDurationMs, long todayDurationMs) {
        long totalHours = Math.max(0L, totalDurationMs / HOUR_MS);
        long todayMinutes = Math.max(0L, todayDurationMs / MINUTE_MS);
        return "累计读" + totalHours + "小时 | 今日读" + todayMinutes + "分钟";
    }

    private static String currentDayKey(long nowMs) {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.setTimeInMillis(nowMs);
        return calendar.get(Calendar.YEAR) + "_"
                + calendar.get(Calendar.DAY_OF_YEAR);
    }

    private static String currentWeekKey(long nowMs) {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.setTimeInMillis(nowMs);
        return calendar.get(Calendar.YEAR) + "_" + calendar.get(Calendar.WEEK_OF_YEAR);
    }

    private static long getStoredTodayReadingMillis(SharedPreUtils prefs, long nowMs) {
        return prefs.getLong(KEY_DAY_PREFIX + currentDayKey(nowMs), 0L);
    }

    private static long getStoredWeeklyReadingMillis(SharedPreUtils prefs, long nowMs) {
        return prefs.getLong(KEY_WEEK_PREFIX + currentWeekKey(nowMs), 0L);
    }
}
