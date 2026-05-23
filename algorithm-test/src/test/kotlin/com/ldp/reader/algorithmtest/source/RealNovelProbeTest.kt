package com.ldp.reader.algorithmtest.source

import com.ldp.reader.algorithmtest.AlgorithmTrace
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class RealNovelProbeTest {
    @Test
    fun runRealNovelProbeWhenExplicitlyEnabled() = runBlocking {
        assumeTrue(System.getProperty("realNovelProbe") == "true")

        val title = System.getProperty("realNovelTitle") ?: "仙逆"
        val author = System.getProperty("realNovelAuthor") ?: "耳根"
        val maxSearchSources = System.getProperty("realNovelMaxSearchSources")?.toIntOrNull() ?: 160
        val maxCatalogSources = System.getProperty("realNovelMaxCatalogSources")?.toIntOrNull() ?: 24
        val minCatalogChapters = System.getProperty("realNovelMinCatalogChapters")?.toIntOrNull() ?: 80
        val projectRoot = File(requireNotNull(System.getProperty("readerProjectRoot")))
        val sourceFile = File(projectRoot, "app/src/main/assets/source-engine/book-sources.json")
        val qualitySeedFile = File(projectRoot, "app/src/main/assets/source-quality-seed-v1.tsv")
        val outDir = File(projectRoot, "algorithm-test/build/reports/real-novel-probe/${safeName(title)}")
        outDir.mkdirs()
        val report = try {
            SourceExperimentRunner().run(
                SourceExperimentConfig(
                    title = title,
                    author = author,
                    sourceJson = sourceFile.readText(),
                    qualitySeedTsv = qualitySeedFile.takeIf { it.exists() }?.readText().orEmpty(),
                    maxSearchSources = maxSearchSources,
                    maxCatalogSources = maxCatalogSources,
                    minCatalogChapters = minCatalogChapters
                )
            )
        } catch (throwable: Throwable) {
            File(outDir, "failure.txt").writeText(throwable.stackTraceToString())
            File(outDir, "source-trace.txt").writeText(AlgorithmTrace.snapshot().joinToString("\n"))
            throw throwable
        }
        File(outDir, "report.txt").writeText(report.humanSummary())
        File(outDir, "source-trace.txt").writeText(report.sourceTrace.joinToString("\n"))
        report.cleanReport.logs.takeIf { it.isNotEmpty() }?.let { logs ->
            File(outDir, "algorithm-log.txt").writeText(logs.joinToString("\n"))
        }
        report.sampledChapters.forEach { chapter ->
            File(outDir, "${chapter.index.toString().padStart(5, '0')}-${safeName(chapter.title)}.txt")
                .writeText(chapter.content)
        }
    }

    private fun safeName(value: String): String {
        return value.replace(Regex("""[\\/:*?"<>|\s]+"""), "_").take(80)
    }
}
