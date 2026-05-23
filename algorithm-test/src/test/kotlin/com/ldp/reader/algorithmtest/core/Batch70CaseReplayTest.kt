package com.ldp.reader.algorithmtest.core

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class Batch70CaseReplayTest {
    @Test
    fun replayAuditedBatch70Cases() {
        assumeTrue(
            "Set -Dbatch70CaseProbe=true to run the audited 70-book batch replay suite",
            System.getProperty("batch70CaseProbe") == "true"
        )

        val root = batch70CasesRoot()
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
    fun batch70FixturesArePresent() {
        val root = batch70CasesRoot()
        val cases = loadCases(File(root, "cases.tsv"))

        assertTrue("expected at least one audited batch70 case", cases.isNotEmpty())
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

    private fun batch70CasesRoot(): File {
        val projectRoot = System.getProperty("readerProjectRoot").orEmpty()
        val root = File(projectRoot, "algorithm-test/src/test/resources/batch70-cases")
        assertTrue("batch70 cases root is missing: ${root.absolutePath}", root.isDirectory)
        return root
    }

    private fun replayOutputDir(): File {
        val projectRoot = System.getProperty("readerProjectRoot").orEmpty()
        return File(projectRoot, "algorithm-test/build/batch70-case-replay")
    }

    private fun loadCases(file: File): List<Batch70Case> {
        assertTrue("batch70 case manifest is missing: ${file.absolutePath}", file.isFile)
        return file.readLines()
            .drop(1)
            .filter { line -> line.isNotBlank() }
            .map { line ->
                val parts = line.split('\t')
                Batch70Case(
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

    private data class Batch70Case(
        val id: String,
        val title: String,
        val author: String,
        val mustSuggestIndexes: Set<Int>,
        val noSuggestIndexes: Set<Int>
    )
}
