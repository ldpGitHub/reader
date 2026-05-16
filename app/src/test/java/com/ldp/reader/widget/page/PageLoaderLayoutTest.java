package com.ldp.reader.widget.page;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PageLoaderLayoutTest {

    @Test
    public void readingContentMarginsKeepStatusBarSafeAndCompactFooter() {
        int displayHeight = 2780;
        int statusBarHeight = 140;
        int contentPadding = 28;

        int topMargin = PageLoader.calculateContentTopMargin(statusBarHeight, contentPadding);
        int bottomMargin = PageLoader.calculateContentBottomMargin(contentPadding);

        assertEquals(168, topMargin);
        assertEquals(28, bottomMargin);
        assertEquals(2584, PageLoader.calculateVisibleContentHeight(displayHeight, topMargin, bottomMargin));
    }
}
