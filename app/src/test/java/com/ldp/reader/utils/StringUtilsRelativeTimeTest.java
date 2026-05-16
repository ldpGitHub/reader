package com.ldp.reader.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilsRelativeTimeTest {

    private static final long NOW = 1_700_000_000_000L;
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;

    @Test
    public void formatsBookUpdateTimeAsRelativeReaderText() {
        assertEquals("2小时前", StringUtils.formatBookUpdateTime(String.valueOf(NOW - 2L * HOUR), NOW));
        assertEquals("3天前", StringUtils.formatBookUpdateTime(String.valueOf(NOW - 3L * DAY), NOW));
        assertEquals("2月前", StringUtils.formatBookUpdateTime(String.valueOf(NOW - 65L * DAY), NOW));
        assertEquals("1年前", StringUtils.formatBookUpdateTime(String.valueOf(NOW - 400L * DAY), NOW));
    }

    @Test
    public void parsesExistingBookDateFormat() {
        long now = 1_700_000_000_000L;
        String twoHoursAgo = StringUtils.dateConvert(now - 2L * HOUR, Constant.FORMAT_BOOK_DATE);

        assertEquals("2小时前", StringUtils.formatBookUpdateTime(twoHoursAgo, now));
    }
}
