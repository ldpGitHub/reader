package com.ldp.reader.algorithmtest.core

import com.ldp.reader.algorithmtest.source.BatchNovelTargets
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.math.min

class RawCorpusReplayTest {
    @Test
    fun replayFetchedRawCorpus() {
        assumeTrue(
            "Set -DrawCorpusReplay=true to replay fetched raw corpus.",
            System.getProperty("rawCorpusReplay") == "true"
        )

        val corpusRoot = File(
            System.getProperty("rawCorpusRoot")
                ?: "algorithm-test/build/raw-corpus-101"
        )
        assertTrue("raw corpus root missing: ${corpusRoot.absolutePath}", corpusRoot.isDirectory)

        val outputRoot = File(
            System.getProperty("rawCorpusOutput")
                ?: "algorithm-test/build/raw-corpus-replay-${System.currentTimeMillis()}"
        ).apply { mkdirs() }

        val corpusItems = findSuccessfulCorpusItems(corpusRoot)
        assertTrue("expected at least 100 successful corpus books", corpusItems.size >= 100)

        val summaryFile = File(outputRoot, "corpus-summary.tsv")
        val auditPlanFile = File(outputRoot, "audit-plan.tsv")
        summaryFile.writeText(
            "bookNo\ttitle\tauthor\tchapters\tchunks\tsuggestions\tsuggestionIndexes\telapsedMs\treportDir\n"
        )
        auditPlanFile.writeText(
            "bookNo\ttitle\tauthor\tkind\tchapterIndex\tchapterTitle\talgorithmState\tconfidence\tstatus\tnote\tchapterFile\n"
        )

        println("Raw corpus replay started: items=${corpusItems.size} output=${outputRoot.absolutePath}")
        corpusItems.forEachIndexed { ordinal, item ->
            val startedAt = System.currentTimeMillis()
            val chapters = item.readChapters()
            val reportDir = File(outputRoot, item.reportName).apply { mkdirs() }
            val report = NovelPollutionAnalyzer().analyze(
                title = item.title,
                author = item.author,
                chapters = chapters
            )
            val elapsedMs = System.currentTimeMillis() - startedAt
            File(reportDir, "algorithm-report.txt").writeText(report.humanSummary(maxFeatures = 20))
            File(reportDir, "algorithm-log.txt").writeText(report.logs.joinToString("\n"))
            File(reportDir, "source-dir.txt").writeText(item.sourceDir.absolutePath)

            val suggestionIndexes = report.suggestions
                .map { suggestion -> suggestion.chapterIndex }
                .distinct()
                .sorted()
            summaryFile.appendText(
                listOf(
                    item.bookNo.toString(),
                    item.title,
                    item.author,
                    chapters.size.toString(),
                    report.chunkCount.toString(),
                    suggestionIndexes.size.toString(),
                    suggestionIndexes.joinToString(","),
                    elapsedMs.toString(),
                    reportDir.absolutePath
                ).joinToString("\t") + "\n"
            )
            buildAuditPlanRows(item, chapters, report, suggestionIndexes).forEach { row ->
                auditPlanFile.appendText(row + "\n")
            }
            println(
                "Raw corpus replay ${ordinal + 1}/${corpusItems.size}: " +
                    "${item.bookNo} ${item.title} chapters=${chapters.size} " +
                    "suggestions=${suggestionIndexes.size} elapsedMs=$elapsedMs"
            )
        }
        println("Raw corpus replay finished: ${outputRoot.absolutePath}")
    }

    private fun findSuccessfulCorpusItems(root: File): List<CorpusItem> {
        return root.walkTopDown()
            .filter { file -> file.isFile && file.name == "fetch-report.txt" }
            .mapNotNull { reportFile ->
                val sourceDir = reportFile.parentFile ?: return@mapNotNull null
                val bookNo = parseBookNo(sourceDir.name) ?: return@mapNotNull null
                val target = BatchNovelTargets.all.getOrNull(bookNo - 1)
                CorpusItem(
                    bookNo = bookNo,
                    title = target?.title ?: parseTitle(sourceDir.name),
                    author = target?.author.orEmpty(),
                    sourceDir = sourceDir
                )
            }
            .sortedBy { item -> item.bookNo }
            .toList()
    }

    private fun buildAuditPlanRows(
        item: CorpusItem,
        chapters: List<ChapterInput>,
        report: CleanReport,
        suggestionIndexes: List<Int>
    ): List<String> {
        val rows = ArrayList<String>()
        val byIndex = chapters.associateBy { chapter -> chapter.index }
        val suggestionByIndex = report.suggestions.associateBy { suggestion -> suggestion.chapterIndex }
        if (suggestionIndexes.isNotEmpty()) {
            val selectedSuggestions = selectProblemAuditIndexes(suggestionIndexes)
            selectedSuggestions.forEach { chapterIndex ->
                val suggestion = suggestionByIndex[chapterIndex]
                val chapter = byIndex[chapterIndex]
                if (suggestion != null && chapter != null) {
                    rows.add(item.auditRow("SUGGESTION_CHECK", chapter, suggestion))
                }
                listOf(chapterIndex - 1, chapterIndex + 1).forEach { neighborIndex ->
                    if (neighborIndex >= 0 && neighborIndex !in suggestionIndexes) {
                        byIndex[neighborIndex]?.let { neighbor ->
                            rows.add(item.auditRow("BOUNDARY_NEIGHBOR_CHECK", neighbor, null))
                        }
                    }
                }
            }
        } else {
            selectCleanTailAuditIndexes(chapters.size).forEach { chapterIndex ->
                byIndex[chapterIndex]?.let { chapter ->
                    rows.add(item.auditRow("NO_SUGGEST_TAIL_CHECK", chapter, null))
                }
            }
        }
        return rows.distinct()
    }

    private fun selectProblemAuditIndexes(suggestionIndexes: List<Int>): List<Int> {
        val selected = LinkedHashSet<Int>()
        suggestionIndexes.take(3).forEach { selected.add(it) }
        var offset = 4
        while (offset <= suggestionIndexes.size) {
            selected.add(suggestionIndexes[offset - 1])
            offset *= 2
        }
        suggestionIndexes.lastOrNull()?.let { selected.add(it) }
        return selected.toList()
    }

    private fun selectCleanTailAuditIndexes(chapterCount: Int): List<Int> {
        if (chapterCount <= 0) return emptyList()
        val selected = LinkedHashSet<Int>()
        var offset = 1
        while (offset <= chapterCount && selected.size < 8) {
            selected.add(chapterCount - offset)
            offset *= 2
        }
        return selected.toList()
    }

    private fun CorpusItem.readChapters(): List<ChapterInput> {
        val chapterDir = File(sourceDir, "chapters")
        return chapterDir.listFiles { file -> file.isFile && file.extension == "txt" }
            .orEmpty()
            .sortedWith(compareBy({ parseChapterIndex(it.name) }, { it.name }))
            .map { file ->
                val index = parseChapterIndex(file.name)
                ChapterInput(
                    index = index,
                    title = parseChapterTitle(file.name),
                    content = file.readText(Charsets.UTF_8)
                )
            }
    }

    private fun CorpusItem.auditRow(
        kind: String,
        chapter: ChapterInput,
        suggestion: CleanSuggestion?
    ): String {
        val chapterFile = File(sourceDir, "chapters").listFiles { file ->
            file.isFile && file.name.startsWith(chapter.index.toString().padStart(5, '0') + "-")
        }.orEmpty().firstOrNull()
        return listOf(
            bookNo.toString(),
            title,
            author,
            kind,
            chapter.index.toString(),
            chapter.title,
            suggestion?.stateType?.name.orEmpty(),
            suggestion?.confidence?.let { "%.3f".format(it) }.orEmpty(),
            "PENDING",
            "",
            chapterFile?.absolutePath.orEmpty()
        ).joinToString("\t") { value -> value.replace('\t', ' ').replace('\n', ' ') }
    }

    private fun parseBookNo(name: String): Int? {
        return Regex("""^(\d{3})-""").find(name)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun parseTitle(name: String): String {
        return name.replace(Regex("""^\d{3}-"""), "").replace(Regex("""-\d+$"""), "")
    }

    private fun parseChapterIndex(name: String): Int {
        return Regex("""^(\d{5})-""").find(name)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
    }

    private fun parseChapterTitle(name: String): String {
        return name.removeSuffix(".txt").replace(Regex("""^\d{5}-"""), "")
    }

    private data class CorpusItem(
        val bookNo: Int,
        val title: String,
        val author: String,
        val sourceDir: File
    ) {
        val reportName: String =
            "${bookNo.toString().padStart(3, '0')}-${title}".replace(Regex("""[\\/:*?"<>|\s]+"""), "_")
                .let { value -> value.substring(0, min(value.length, 100)) }
    }
}
