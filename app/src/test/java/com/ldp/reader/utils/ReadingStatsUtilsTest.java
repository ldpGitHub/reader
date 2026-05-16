package com.ldp.reader.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReadingStatsUtilsTest {

    @Test
    public void formatsWeeklyReadingMinutes() {
        assertEquals("本周读0分钟", ReadingStatsUtils.formatWeeklyReadingLabel(0));
        assertEquals("本周读5分钟", ReadingStatsUtils.formatWeeklyReadingLabel(5L * 60_000L));
    }

    @Test
    public void formatsWeeklyReadingHoursAndMinutes() {
        assertEquals("本周读1小时5分钟", ReadingStatsUtils.formatWeeklyReadingLabel(65L * 60_000L));
        assertEquals("本周读2小时", ReadingStatsUtils.formatWeeklyReadingLabel(120L * 60_000L));
    }

    @Test
    public void formatsMineReadingLabelLikeReaderProfile() {
        assertEquals(
                "累计读0小时 | 今日读0分钟",
                ReadingStatsUtils.formatMineReadingLabel(0L, 0L)
        );
        assertEquals(
                "累计读2小时 | 今日读18分钟",
                ReadingStatsUtils.formatMineReadingLabel(125L * 60_000L, 18L * 60_000L)
        );
    }

    @Test
    public void formatsPlainDurationForStatsRows() {
        assertEquals("0分钟", ReadingStatsUtils.formatDurationLabel(0L));
        assertEquals("3分钟", ReadingStatsUtils.formatDurationLabel(3L * 60_000L));
        assertEquals("1小时5分钟", ReadingStatsUtils.formatDurationLabel(65L * 60_000L));
        assertEquals("2小时", ReadingStatsUtils.formatDurationLabel(120L * 60_000L));
    }

    @Test
    public void normalizesLegacyStatsSoAggregatesRemainCoherent() {
        long fiveMinutes = 5L * 60_000L;
        long sevenMinutes = 7L * 60_000L;

        long total = ReadingStatsUtils.normalizedTotalReadingMillis(
                fiveMinutes,
                fiveMinutes,
                sevenMinutes
        );
        long week = ReadingStatsUtils.normalizedWeeklyReadingMillis(
                total,
                fiveMinutes,
                sevenMinutes
        );
        long today = ReadingStatsUtils.normalizedTodayReadingMillis(
                total,
                week,
                fiveMinutes
        );

        assertEquals(sevenMinutes, total);
        assertEquals(sevenMinutes, week);
        assertEquals(fiveMinutes, today);
    }

    @Test
    public void normalizesTodayIntoWeekAndTotalWhenOnlyDayWasStored() {
        long todayOnly = 6L * 60_000L;

        long total = ReadingStatsUtils.normalizedTotalReadingMillis(0L, todayOnly, 0L);
        long week = ReadingStatsUtils.normalizedWeeklyReadingMillis(total, todayOnly, 0L);
        long today = ReadingStatsUtils.normalizedTodayReadingMillis(total, week, todayOnly);

        assertEquals(todayOnly, total);
        assertEquals(todayOnly, week);
        assertEquals(todayOnly, today);
    }
}
