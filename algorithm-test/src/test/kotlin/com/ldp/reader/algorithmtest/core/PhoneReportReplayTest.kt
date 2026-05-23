package com.ldp.reader.algorithmtest.core

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Test

class PhoneReportReplayTest {
    @Test
    fun replayPulledPhoneReport() {
        val reportPath = System.getProperty("phoneReportPath").orEmpty()
        assumeTrue("phoneReportPath is not set", reportPath.isNotBlank())

        val root = File(reportPath)
        val chaptersDir = File(root, "chapters")
        assumeTrue("phone report chapters directory is missing: ${chaptersDir.absolutePath}", chaptersDir.isDirectory)

        val chapters = chaptersDir.listFiles { file -> file.isFile && file.extension == "txt" }
            .orEmpty()
            .mapNotNull { file -> file.toChapterInput() }
            .sortedBy { chapter -> chapter.index }
        assumeTrue("phone report has no chapter text", chapters.isNotEmpty())

        val title = System.getProperty("phoneReportTitle").orEmpty().ifBlank { "斗破苍穹" }
        val author = System.getProperty("phoneReportAuthor").orEmpty().ifBlank { "天蚕土豆" }
        val report = NovelPollutionAnalyzer().analyze(title, author, chapters)
        File(root, "replay-report.txt").writeText(
            report.humanSummary() +
                "\n" + debugChunkSummary(report) +
                "\n" + report.logs.joinToString("\n")
        )

        val noSuggestIndexes = System.getProperty("phoneReportNoSuggestIndexes").orEmpty()
            .split(",")
            .mapNotNull { item -> item.trim().toIntOrNull() }
            .toSet()
        noSuggestIndexes.forEach { index ->
            assertFalse(
                "chapter index $index was manually inspected as normal and must not be suggested",
                report.suggestions.any { suggestion -> suggestion.chapterIndex == index }
            )
        }
        val mustSuggestIndexes = System.getProperty("phoneReportMustSuggestIndexes").orEmpty()
            .split(",")
            .mapNotNull { item -> item.trim().toIntOrNull() }
            .toSet()
        mustSuggestIndexes.forEach { index ->
            org.junit.Assert.assertTrue(
                "chapter index $index was manually inspected as polluted and must be suggested",
                report.suggestions.any { suggestion -> suggestion.chapterIndex == index }
            )
        }
    }

    private fun File.toChapterInput(): ChapterInput? {
        val match = Regex("""^(\d+)-(.+)\.txt$""").matchEntire(name) ?: return null
        val index = match.groupValues[1].toInt()
        val title = match.groupValues[2].replace('_', ' ')
        return ChapterInput(
            index = index,
            title = title,
            content = readText()
        )
    }

    private fun debugChunkSummary(report: CleanReport): String {
        val suggestionChapterIndexes = report.suggestions.map { suggestion -> suggestion.chapterIndex }.toSet()
        val suggestionScores = report.chunkScores
            .filter { score -> score.chunk.chapterIndex in suggestionChapterIndexes }
            .joinToString("\n") { score ->
                "suggestion chapter=${score.chunk.chapterIndex} title=${score.chunk.chapterTitle} " +
                    "chunk=${score.chunk.chunkIndex} start=${score.chunk.startOffset} end=${score.chunk.endOffset} " +
                    "belong=${"%.3f".format(score.belongScore)} known=${"%.1f".format(score.knownScore)} " +
                    "alien=${"%.1f".format(score.alienScore)} knownTop=${score.knownFeatures.take(4)} " +
                    "alienTop=${score.alienFeatures.take(6)}"
            }
        val tailStart = report.chunkScores.maxOfOrNull { score -> score.chunk.chapterIndex }?.minus(4) ?: 0
        val tailScores = report.chunkScores
            .filter { score -> score.chunk.chapterIndex >= tailStart }
            .joinToString("\n") { score ->
                "chunk chapter=${score.chunk.chapterIndex} title=${score.chunk.chapterTitle} " +
                    "chunk=${score.chunk.chunkIndex} start=${score.chunk.startOffset} end=${score.chunk.endOffset} " +
                    "belong=${"%.3f".format(score.belongScore)} known=${"%.1f".format(score.knownScore)} " +
                    "alien=${"%.1f".format(score.alienScore)} knownTop=${score.knownFeatures.take(4)} " +
                    "alienTop=${score.alienFeatures.take(6)}"
            }
        val lowestScores = report.chunkScores
            .sortedBy { score -> score.belongScore }
            .take(20)
            .joinToString("\n") { score ->
                "low chapter=${score.chunk.chapterIndex} title=${score.chunk.chapterTitle} " +
                    "chunk=${score.chunk.chunkIndex} start=${score.chunk.startOffset} " +
                    "belong=${"%.3f".format(score.belongScore)} known=${"%.1f".format(score.knownScore)} " +
                    "alien=${"%.1f".format(score.alienScore)} alienTop=${score.alienFeatures.take(6)}"
            }
        return "Suggestion chunk scores:\n$suggestionScores\nTail chunk scores:\n$tailScores\nLowest chunk scores:\n$lowestScores\n"
    }
}
