package com.ldp.reader.algorithmtest.catalog

data class NamedCatalog(
    val sourceName: String,
    val chapters: List<String>
)

data class CatalogCandidateScore(
    val sourceName: String,
    val chapterCount: Int,
    val duplicateCount: Int,
    val ordinalCount: Int,
    val missingOrdinalGaps: Int,
    val firstTitle: String,
    val lastTitle: String,
    val score: Double
)

data class CatalogFusionReport(
    val best: CatalogCandidateScore?,
    val candidates: List<CatalogCandidateScore>,
    val fusedTitles: List<String>,
    val notes: List<String>
) {
    fun humanSummary(maxTitles: Int = 20): String {
        val builder = StringBuilder()
        builder.appendLine("Catalog candidates: ${candidates.size}")
        best?.let { item ->
            builder.appendLine(
                "Best: ${item.sourceName}, chapters=${item.chapterCount}, " +
                    "ordinals=${item.ordinalCount}, gaps=${item.missingOrdinalGaps}, score=${"%.1f".format(item.score)}"
            )
            builder.appendLine("First: ${item.firstTitle}")
            builder.appendLine("Last: ${item.lastTitle}")
        }
        candidates.take(8).forEach { item ->
            builder.appendLine(
                "- ${item.sourceName}: chapters=${item.chapterCount}, dup=${item.duplicateCount}, " +
                    "ord=${item.ordinalCount}, gaps=${item.missingOrdinalGaps}, score=${"%.1f".format(item.score)}"
            )
        }
        builder.appendLine("Fused preview:")
        fusedTitles.take(maxTitles).forEachIndexed { index, title ->
            builder.appendLine("${index + 1}. $title")
        }
        notes.forEach { note -> builder.appendLine("note: $note") }
        return builder.toString()
    }
}

class CatalogFusionProbe {
    fun analyze(catalogs: List<NamedCatalog>): CatalogFusionReport {
        val candidates = catalogs
            .map { catalog -> scoreCatalog(catalog) }
            .sortedWith(
                compareByDescending<CatalogCandidateScore> { score -> score.score }
                    .thenByDescending { score -> score.chapterCount }
                    .thenBy { score -> score.missingOrdinalGaps }
            )
        val bestCatalog = candidates.firstOrNull()?.sourceName?.let { name ->
            catalogs.firstOrNull { catalog -> catalog.sourceName == name }
        }
        val fused = fuseByBestCatalog(bestCatalog, catalogs)
        val notes = buildList {
            if (catalogs.isEmpty()) add("no catalog input")
            if (candidates.any { score -> score.chapterCount < (candidates.firstOrNull()?.chapterCount ?: 0) / 2 }) {
                add("short catalog candidates detected")
            }
        }
        return CatalogFusionReport(
            best = candidates.firstOrNull(),
            candidates = candidates,
            fusedTitles = fused,
            notes = notes
        )
    }

    private fun scoreCatalog(catalog: NamedCatalog): CatalogCandidateScore {
        val titles = catalog.chapters.map { title -> title.trim() }.filter { title -> title.isNotBlank() }
        val keys = titles.map { title -> normalizeTitle(title) }
        val duplicates = keys.size - keys.toSet().size
        val ordinals = titles.mapNotNull { title -> ordinalOf(title) }
        val gaps = missingOrdinalGaps(ordinals)
        val ordinalContinuity = if (ordinals.isEmpty()) 0.0 else 1.0 / (1.0 + gaps)
        val duplicatePenalty = duplicates * 3.0
        val score = titles.size * 2.0 + ordinals.size * 1.4 + ordinalContinuity * 50.0 - duplicatePenalty
        return CatalogCandidateScore(
            sourceName = catalog.sourceName,
            chapterCount = titles.size,
            duplicateCount = duplicates,
            ordinalCount = ordinals.size,
            missingOrdinalGaps = gaps,
            firstTitle = titles.firstOrNull().orEmpty(),
            lastTitle = titles.lastOrNull().orEmpty(),
            score = score
        )
    }

    private fun fuseByBestCatalog(best: NamedCatalog?, catalogs: List<NamedCatalog>): List<String> {
        val base = best?.chapters.orEmpty().map { title -> title.trim() }.filter { title -> title.isNotBlank() }
        if (base.isNotEmpty()) return base
        val seen = LinkedHashSet<String>()
        val fused = ArrayList<String>()
        catalogs.forEach { catalog ->
            catalog.chapters.forEach { title ->
                val key = normalizeTitle(title)
                if (key.isNotBlank() && seen.add(key)) fused.add(title)
            }
        }
        return fused
    }

    private fun normalizeTitle(value: String): String {
        return value
            .lowercase()
            .replace(Regex("""[《》「」『』【】\[\]（）()，,。.!！?？\s]+"""), "")
    }

    private fun ordinalOf(value: String): Int? {
        numberPattern.find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        chineseOrdinalPattern.find(value)?.groupValues?.getOrNull(1)?.let { return chineseNumberToInt(it) }
        return null
    }

    private fun missingOrdinalGaps(ordinals: List<Int>): Int {
        val sorted = ordinals.filter { ordinal -> ordinal in 1..20_000 }.distinct().sorted()
        if (sorted.size < 2) return 0
        return sorted.zipWithNext().sumOf { (left, right) -> (right - left - 1).coerceAtLeast(0) }
    }

    private fun chineseNumberToInt(value: String): Int? {
        var result = 0
        var section = 0
        var number = 0
        value.forEach { char ->
            when (char) {
                '零' -> Unit
                '一' -> number = 1
                '二', '两' -> number = 2
                '三' -> number = 3
                '四' -> number = 4
                '五' -> number = 5
                '六' -> number = 6
                '七' -> number = 7
                '八' -> number = 8
                '九' -> number = 9
                '十' -> {
                    section += (if (number == 0) 1 else number) * 10
                    number = 0
                }
                '百' -> {
                    section += (if (number == 0) 1 else number) * 100
                    number = 0
                }
                '千' -> {
                    section += (if (number == 0) 1 else number) * 1000
                    number = 0
                }
                else -> return null
            }
        }
        result += section + number
        return result.takeIf { it > 0 }
    }

    private companion object {
        private val numberPattern = Regex("""第?\s*(\d{1,5})\s*[章章节回卷]""")
        private val chineseOrdinalPattern = Regex("""第\s*([零一二两三四五六七八九十百千]+)\s*[章章节回卷]""")
    }
}
