package com.ldp.reader.algorithmtest.source

import com.ldp.reader.algorithmtest.core.ChapterInput
import java.security.MessageDigest

object SampleValidationProbe {
    fun analyze(chapters: List<ChapterInput>): String {
        if (chapters.isEmpty()) return "Sample validation: no chapters"
        val issues = ArrayList<String>()

        duplicateContentIssues(chapters).take(8).forEach { issue -> issues.add(issue) }
        ordinalMismatchIssues(chapters).take(8).forEach { issue -> issues.add(issue) }
        ordinalRegressionIssues(chapters).take(8).forEach { issue -> issues.add(issue) }

        return buildString {
            appendLine("Sample validation issues: ${issues.size}")
            issues.forEach { issue -> appendLine("- $issue") }
            if (issues.isEmpty()) appendLine("- none")
        }.trimEnd()
    }

    private fun duplicateContentIssues(chapters: List<ChapterInput>): List<String> {
        return chapters
            .groupBy { chapter -> contentHash(chapter.content) }
            .values
            .filter { group -> group.size > 1 }
            .map { group ->
                "duplicate content: " + group.joinToString(" == ") { chapter ->
                    "#${chapter.index} ${chapter.title}"
                }
            }
    }

    private fun ordinalMismatchIssues(chapters: List<ChapterInput>): List<String> {
        val issues = ArrayList<String>()
        var previous: Pair<ChapterInput, Int>? = null
        chapters.sortedBy { chapter -> chapter.index }.forEach { chapter ->
            val ordinal = titleOrdinal(chapter.title) ?: return@forEach
            val before = previous
            if (before != null) {
                val previousOffset = before.first.index + 1 - before.second
                val currentOffset = chapter.index + 1 - ordinal
                if (kotlin.math.abs(currentOffset - previousOffset) >= 20) {
                    issues.add(
                        "ordinal mismatch: #${before.first.index} ${before.first.title} -> " +
                            "#${chapter.index} ${chapter.title}"
                    )
                }
            }
            previous = chapter to ordinal
        }
        return issues
    }

    private fun ordinalRegressionIssues(chapters: List<ChapterInput>): List<String> {
        val issues = ArrayList<String>()
        var previous: ChapterInput? = null
        var previousOrdinal: Int? = null
        chapters.sortedBy { chapter -> chapter.index }.forEach { chapter ->
            val ordinal = titleOrdinal(chapter.title)
            val before = previous
            val beforeOrdinal = previousOrdinal
            if (before != null && beforeOrdinal != null && ordinal != null && ordinal + 10 < beforeOrdinal) {
                issues.add(
                    "ordinal regression: #${before.index} ${before.title} -> #${chapter.index} ${chapter.title}"
                )
            }
            previous = chapter
            previousOrdinal = ordinal
        }
        return issues
    }

    private fun titleOrdinal(title: String): Int? {
        return Regex("""第0*(\d+)章""").find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun contentHash(content: String): String {
        val normalized = content
            .replace(Regex("""\s+"""), "")
            .replace("read3();", "")
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
