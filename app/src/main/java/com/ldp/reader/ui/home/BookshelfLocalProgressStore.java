package com.ldp.reader.ui.home;

import com.ldp.reader.utils.SharedPreUtils;

public final class BookshelfLocalProgressStore {
    private static final String PREFIX = "bookshelf_local_progress_tenths_";
    private static final int UNKNOWN_PROGRESS = -1;

    private BookshelfLocalProgressStore() {
    }

    public static void saveProgressTenths(String bookId, int progressTenths) {
        if (bookId == null || bookId.trim().isEmpty()) {
            return;
        }
        int safeProgress = Math.max(0, Math.min(progressTenths, 999));
        SharedPreUtils.getInstance().putInt(PREFIX + bookId, safeProgress);
    }

    public static int getProgressTenths(String bookId) {
        if (bookId == null || bookId.trim().isEmpty()) {
            return UNKNOWN_PROGRESS;
        }
        return SharedPreUtils.getInstance().getInt(PREFIX + bookId, UNKNOWN_PROGRESS);
    }

    public static void clear(String bookId) {
        if (bookId == null || bookId.trim().isEmpty()) {
            return;
        }
        SharedPreUtils.getInstance().putInt(PREFIX + bookId, UNKNOWN_PROGRESS);
    }
}
