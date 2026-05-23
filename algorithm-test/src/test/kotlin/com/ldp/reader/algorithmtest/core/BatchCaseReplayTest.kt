package com.ldp.reader.algorithmtest.core

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class BatchCaseReplayTest {
    @Test
    fun replayTwentyRealBatchCases() {
        assumeTrue(
            "Set -DbatchCaseProbe=true to run the twenty-book real-novel replay suite",
            System.getProperty("batchCaseProbe") == "true"
        )

        val root = batchCasesRoot()
        val cases = loadCases(File(root, "cases.tsv"))
        val failures = ArrayList<String>()
        val reportDir = replayOutputDir().apply { mkdirs() }

        cases.forEach { case ->
            val chapters = loadChapters(File(root, "${case.id}/chapters"))
            val report = NovelPollutionAnalyzer().analyze(case.title, case.author, chapters)
            val watchedIndexes = (case.mustSuggestIndexes + case.noSuggestIndexes).toSortedSet()
            File(reportDir, "${case.id}.txt").writeText(
                report.humanSummary() +
                    "\n" + watchedIndexes.joinToString("\n") { index ->
                        report.chapterDebugSummary(index)
                    } +
                    "\n" + report.logs.joinToString("\n")
            )
            val suggested = report.suggestions.map { suggestion -> suggestion.chapterIndex }.toSet()

            val extraSuggestions = suggested - case.mustSuggestIndexes
            if (extraSuggestions.isNotEmpty()) {
                failures.add("${case.id}: extra suggested chapters ${extraSuggestions.sorted()}")
            }
            val missingSuggestions = case.mustSuggestIndexes - suggested
            if (missingSuggestions.isNotEmpty()) {
                failures.add("${case.id}: missing suggested chapters ${missingSuggestions.sorted()}")
            }
            case.noSuggestIndexes.forEach { index ->
                if (index in suggested) failures.add("${case.id}: chapter $index must not be suggested")
            }
        }

        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun batchCaseFixturesArePresent() {
        val root = batchCasesRoot()
        val cases = loadCases(File(root, "cases.tsv"))

        assertTrue("expected twenty batch cases", cases.size == 20)
        cases.forEach { case ->
            val chapters = loadChapters(File(root, "${case.id}/chapters"))
            assertTrue("${case.id} should include real chapter text", chapters.size >= 40)
            assertTrue(
                "${case.id} should include every must-suggest chapter",
                case.mustSuggestIndexes.all { index -> chapters.any { chapter -> chapter.index == index } }
            )
            assertTrue(
                "${case.id} should include every no-suggest chapter",
                case.noSuggestIndexes.all { index -> chapters.any { chapter -> chapter.index == index } }
            )
        }
    }

    private fun batchCasesRoot(): File {
        val projectRoot = System.getProperty("readerProjectRoot").orEmpty()
        val root = File(projectRoot, "algorithm-test/src/test/resources/batch-cases")
        assertTrue("batch cases root is missing: ${root.absolutePath}", root.isDirectory)
        return root
    }

    private fun replayOutputDir(): File {
        val projectRoot = System.getProperty("readerProjectRoot").orEmpty()
        return File(projectRoot, "algorithm-test/build/batch-case-replay")
    }

    private fun loadCases(file: File): List<BatchCase> {
        assertTrue("batch case manifest is missing: ${file.absolutePath}", file.isFile)
        return file.readLines()
            .drop(1)
            .filter { line -> line.isNotBlank() }
            .map { line ->
                val parts = line.split('\t')
                BatchCase(
                    id = parts[0],
                    title = parts[1],
                    author = parts[2],
                    mustSuggestIndexes = parseIndexes(parts.getOrNull(3).orEmpty()),
                    noSuggestIndexes = parseIndexes(parts.getOrNull(4).orEmpty())
                )
            }
    }

    private fun loadChapters(chaptersDir: File): List<ChapterInput> {
        assertTrue("chapters directory is missing: ${chaptersDir.absolutePath}", chaptersDir.isDirectory)
        return chaptersDir.listFiles { file -> file.isFile && file.extension == "txt" }
            .orEmpty()
            .mapNotNull { file -> file.toChapterInput() }
            .sortedBy { chapter -> chapter.index }
    }

    private fun parseIndexes(value: String): Set<Int> {
        return value.split(",")
            .mapNotNull { item -> item.trim().toIntOrNull() }
            .toSet()
    }

    private fun File.toChapterInput(): ChapterInput? {
        val match = Regex("""^(\d+)-(.+)\.txt$""").matchEntire(name) ?: return null
        return ChapterInput(
            index = match.groupValues[1].toInt(),
            title = match.groupValues[2].replace('_', ' '),
            content = readText()
        )
    }

    private fun CleanReport.chapterDebugSummary(chapterIndex: Int): String {
        val scores = chunkScores
            .filter { score -> score.chunk.chapterIndex == chapterIndex }
            .sortedBy { score -> score.chunk.startOffset }
        if (scores.isEmpty()) return "watch.chapter=$chapterIndex missing"

        val builder = StringBuilder()
        builder.appendLine("watch.chapter=$chapterIndex title=${scores.first().chunk.chapterTitle}")
        scores.forEach { score ->
            builder.appendLine(
                "  chunk=${score.chunk.chunkIndex} ${score.chunk.startOffset}-${score.chunk.endOffset} " +
                    "belong=${"%.2f".format(score.belongScore)} " +
                    "known=${"%.1f".format(score.knownScore)} alien=${"%.1f".format(score.alienScore)} " +
                    "knownFeatures=${score.knownFeatures.joinToString("|")} " +
                    "alienFeatures=${score.alienFeatures.joinToString("|")}"
            )
        }
        return builder.toString().trimEnd()
    }

    private data class BatchCase(
        val id: String,
        val title: String,
        val author: String,
        val mustSuggestIndexes: Set<Int>,
        val noSuggestIndexes: Set<Int>
    )
}
