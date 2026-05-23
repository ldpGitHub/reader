package com.ldp.reader.utils

import java.io.File
import java.io.FileFilter

object LocalBookImportFiles {
    val importFileFilter: FileFilter = FileFilter { file ->
        if (file.name.startsWith(".")) return@FileFilter false

        if (file.isDirectory) {
            val children = file.list() ?: return@FileFilter false
            return@FileFilter children.isNotEmpty()
        }

        file.length() > 0L && file.name.endsWith(FileUtils.SUFFIX_TXT)
    }

    fun listVisibleChildren(directory: File): List<File> {
        return directory.listFiles(importFileFilter)
            ?.toMutableList()
            ?.apply { sortWith(fileComparator) }
            .orEmpty()
    }

    fun visibleChildCount(directory: File): Int {
        return directory.list()?.size ?: 0
    }

    private val fileComparator = Comparator<File> { left, right ->
        when {
            left.isDirectory && right.isFile -> -1
            right.isDirectory && left.isFile -> 1
            else -> left.name.compareTo(right.name, ignoreCase = true)
        }
    }
}
