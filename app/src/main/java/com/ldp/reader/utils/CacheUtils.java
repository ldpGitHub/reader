package com.ldp.reader.utils;

import android.content.Context;

import java.io.File;

public final class CacheUtils {
    private CacheUtils() {
    }

    public static long getAppCacheSize(Context context) {
        if (context == null) {
            return 0L;
        }
        long size = directorySize(context.getCacheDir());
        size += directorySize(context.getExternalCacheDir());
        return size;
    }

    public static String getAppCacheSizeLabel(Context context) {
        return FileUtils.getFileSize(getAppCacheSize(context));
    }

    public static void clearAppCache(Context context) {
        if (context == null) {
            return;
        }
        deleteChildren(context.getCacheDir());
        deleteChildren(context.getExternalCacheDir());
    }

    private static long directorySize(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        if (file.isFile()) {
            return file.length();
        }
        File[] children = file.listFiles();
        if (children == null) {
            return 0L;
        }
        long size = 0L;
        for (File child : children) {
            size += directorySize(child);
        }
        return size;
    }

    private static void deleteChildren(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            deleteRecursively(child);
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
