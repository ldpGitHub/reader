package com.ldp.reader.algorithmtest.core

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class Batch70SeedReplayTest {
    @Test
    fun replayFullBatch70SeedCases() {
        assumeTrue(
            "Set -Dbatch70SeedProbe=true to run the full 61-book seed replay suite",
            System.getProperty("batch70SeedProbe") == "true"
        )

        val root = seedRoot()
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
    fun fullBatch70SeedFixturesArePresent() {
        val root = seedRoot()
        val cases = loadCases(File(root, "cases.tsv"))
        val labels = loadLabels(File(root, "labels.tsv"))

        assertTrue("expected 61 complete seed cases", cases.size == 61)
        assertTrue("expected full per-chapter labels", labels.size >= 2_900)
        cases.forEach { case ->
            val chapters = loadChapters(File(root, "${case.id}/chapters"))
            val chapterIndexes = chapters.map { chapter -> chapter.index }.toSet()
            assertTrue("${case.id} should include real sampled chapter text", chapters.size >= 47)
            assertTrue(
                "${case.id} should label every sampled chapter",
                labels.filter { label -> label.id == case.id }
                    .map { label -> label.index }
                    .toSet() == chapterIndexes
            )
            assertTrue(
                "${case.id} should include every must-suggest chapter",
                case.mustSuggestIndexes.all { index -> index in chapterIndexes }
            )
            assertTrue(
                "${case.id} should include every no-suggest chapter",
                case.noSuggestIndexes.all { index -> index in chapterIndexes }
            )
            assertTrue(
                "${case.id} should classify every sampled chapter as must or no-suggest",
                case.mustSuggestIndexes + case.noSuggestIndexes == chapterIndexes
            )
        }
    }

    private fun seedRoot(): File {
        val projectRoot = System.getProperty("readerProjectRoot").orEmpty()
        val root = File(projectRoot, "algorithm-test/src/test/resources/batch70-seed-cases")
        assertTrue("batch70 seed cases root is missing: ${root.absolutePath}", root.isDirectory)
        return root
    }

    private fun replayOutputDir(): File {
        val projectRoot = System.getProperty("readerProjectRoot").orEmpty()
        return File(projectRoot, "algorithm-test/build/batch70-seed-replay")
    }

    private fun loadCases(file: File): List<SeedCase> {
        assertTrue("batch70 seed case manifest is missing: ${file.absolutePath}", file.isFile)
        return file.readLines()
            .drop(1)
            .filter { line -> line.isNotBlank() }
            .map { line ->
                val parts = line.split('\t')
                SeedCase(
                    id = parts[0],
                    title = parts[1],
                    author = parts[2],
                    mustSuggestIndexes = parseIndexes(parts.getOrNull(3).orEmpty()),
                    noSuggestIndexes = parseIndexes(parts.getOrNull(4).orEmpty())
                )
            }
    }

    private fun loadLabels(file: File): List<SeedLabel> {
        assertTrue("batch70 seed labels are missing: ${file.absolutePath}", file.isFile)
        return file.readLines()
            .drop(1)
            .filter { line -> line.isNotBlank() }
            .map { line ->
                val parts = line.split('\t')
                SeedLabel(
                    id = parts[0],
                    index = parts[1].toInt()
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

    private data class SeedCase(
        val id: String,
        val title: String,
        val author: String,
        val mustSuggestIndexes: Set<Int>,
        val noSuggestIndexes: Set<Int>
    )

    private data class SeedLabel(
        val id: String,
        val index: Int
    )
}
