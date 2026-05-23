package com.ldp.reader.algorithmtest.core

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class TypicalCaseReplayTest {
    @Test
    fun replayTypicalRealCases() {
        assumeTrue(
            "Set -DtypicalCaseProbe=true to run the local real-novel replay suite",
            System.getProperty("typicalCaseProbe") == "true"
        )

        val root = typicalCasesRoot()
        val cases = loadCases(File(root, "cases.tsv"))
        val failures = ArrayList<String>()
        val reportDir = replayOutputDir().apply { mkdirs() }

        cases.forEach { case ->
            val chapters = loadChapters(File(root, "${case.id}/chapters"))
            val report = NovelPollutionAnalyzer().analyze(case.title, case.author, chapters)
            File(reportDir, "${case.id}.txt").writeText(
                report.humanSummary() +
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
    fun typicalCaseFixturesArePresent() {
        val root = typicalCasesRoot()
        val cases = loadCases(File(root, "cases.tsv"))

        assertFalse("expected typical cases", cases.isEmpty())
        cases.forEach { case ->
            val chapters = loadChapters(File(root, "${case.id}/chapters"))
            assertTrue("${case.id} should include real chapter text", chapters.size >= 20)
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

    private fun typicalCasesRoot(): File {
        val projectRoot = System.getProperty("readerProjectRoot").orEmpty()
        val root = File(projectRoot, "algorithm-test/src/test/resources/typical-cases")
        assertTrue("typical cases root is missing: ${root.absolutePath}", root.isDirectory)
        return root
    }

    private fun replayOutputDir(): File {
        val projectRoot = System.getProperty("readerProjectRoot").orEmpty()
        return File(projectRoot, "algorithm-test/build/typical-case-replay")
    }

    private fun loadCases(file: File): List<TypicalCase> {
        assertTrue("typical case manifest is missing: ${file.absolutePath}", file.isFile)
        return file.readLines()
            .drop(1)
            .filter { line -> line.isNotBlank() }
            .map { line ->
                val parts = line.split('\t')
                TypicalCase(
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

    private data class TypicalCase(
        val id: String,
        val title: String,
        val author: String,
        val mustSuggestIndexes: Set<Int>,
        val noSuggestIndexes: Set<Int>
    )
}
