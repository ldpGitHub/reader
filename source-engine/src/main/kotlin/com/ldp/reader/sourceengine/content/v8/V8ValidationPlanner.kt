package com.ldp.reader.sourceengine.content.v8

class V8ValidationPlanner(
    private val qualityGate: V8ChapterQualityGate = V8ChapterQualityGate()
) {
    fun selectChapters(
        chapters: List<V8ValidationChapter>,
        diagnosticSink: V8DiagnosticSink = V8DiagnosticSink.None,
        readContent: (position: Int, chapter: V8ValidationChapter) -> String
    ): V8ValidationPlan {
        if (chapters.isEmpty()) {
            diagnosticSink.record(v8DiagnosticLine("v8.plan.empty"))
            return V8ValidationPlan.EMPTY
        }

        val skippedByTitle = skippedTitlePositions(chapters)
        val targetPositions = chapters.indices
            .filter { position -> position !in skippedByTitle }
            .toSet()
        val contextPositions = selectContextPositions(chapters.size, skippedByTitle, readContent, chapters)
        val rolesByPosition = LinkedHashMap<Int, String>()
        contextPositions.forEach { position -> rolesByPosition[position] = ROLE_CONTEXT }
        targetPositions.forEach { position -> rolesByPosition.putIfAbsent(position, ROLE_TARGET) }

        diagnosticSink.record(
            v8DiagnosticLine(
                "v8.plan.finish",
                "chapters" to chapters.size,
                "target" to targetPositions.size,
                "context" to contextPositions.size,
                "usableContext" to contextPositions.size,
                "skipped" to skippedByTitle.size
            )
        )

        return V8ValidationPlan(
            analysisPositions = rolesByPosition.keys.sorted(),
            targetPositions = targetPositions,
            contextPositions = contextPositions,
            targetIndexes = targetPositions.map { position -> chapters[position].index }.toSet(),
            contextIndexes = contextPositions.map { position -> chapters[position].index }.toSet(),
            rolesByPosition = rolesByPosition.toMap(),
            rolesByChapterIndex = rolesByPosition.mapKeys { (position, _) -> chapters[position].index },
            usableContext = contextPositions.size
        )
    }

    private fun skippedTitlePositions(chapters: List<V8ValidationChapter>): Set<Int> {
        val storyTitleCount = chapters.count { chapter -> V8CatalogTitleClassifier.isStoryChapterTitle(chapter.title) }
        val requireStoryTitle = storyTitleCount >= MIN_CHAPTER_TITLE_GATE_MATCHES &&
            storyTitleCount * 100 >= chapters.size * CHAPTER_TITLE_GATE_PERCENT
        return chapters
            .withIndex()
            .filter { (_, chapter) ->
                V8CatalogTitleClassifier.shouldSkipBeforeContent(chapter.title, requireStoryTitle)
            }
            .map { (position, _) -> position }
            .toSet()
    }

    private fun selectContextPositions(
        chapterCount: Int,
        skippedByTitle: Set<Int>,
        readContent: (position: Int, chapter: V8ValidationChapter) -> String,
        chapters: List<V8ValidationChapter>
    ): Set<Int> {
        val tailStart = (chapterCount - TAIL_RISK_WINDOW_CHAPTERS).coerceAtLeast(0)
        val endExclusive = tailStart.coerceAtLeast((chapterCount * 7 / 10).coerceAtLeast(1))
        val candidates = LinkedHashSet<Int>()
        evenlySpacedPositions(0, endExclusive, 4).forEach(candidates::add)
        evenlySpacedPositions((endExclusive - 120).coerceAtLeast(0), endExclusive, 8).forEach(candidates::add)
        var offset = 1
        while (offset <= endExclusive && candidates.size < MAX_CONTEXT_CANDIDATES) {
            candidates.add(endExclusive - offset)
            offset *= 2
        }
        val selected = LinkedHashSet<Int>()
        candidates
            .asSequence()
            .filter { position -> position in 0 until chapterCount }
            .filter { position -> position !in skippedByTitle }
            .forEach { position ->
                if (selected.size >= MIN_USABLE_CONTEXT_CHAPTERS) return@forEach
                val chapter = chapters[position]
                val quality = qualityGate.inspect(
                    V8ChapterInput(
                        index = chapter.index,
                        title = chapter.title,
                        content = readContent(position, chapter)
                    )
                )
                if (quality.usableForStory) selected.add(position)
            }
        return selected
    }

    private fun evenlySpacedPositions(startInclusive: Int, endExclusive: Int, count: Int): List<Int> {
        val size = endExclusive - startInclusive
        if (size <= 0) return emptyList()
        if (size <= count) return (startInclusive until endExclusive).toList()
        return (0 until count).map { index ->
            startInclusive + ((size - 1).toLong() * index / (count - 1).coerceAtLeast(1)).toInt()
        }.distinct()
    }

    companion object {
        const val TAIL_RISK_WINDOW_CHAPTERS = 160
        const val MIN_USABLE_CONTEXT_CHAPTERS = 8
        const val ROLE_CONTEXT = "CONTEXT"
        const val ROLE_TARGET = "TARGET"
        private const val MAX_CONTEXT_CANDIDATES = 64
        private const val MIN_CHAPTER_TITLE_GATE_MATCHES = 8
        private const val CHAPTER_TITLE_GATE_PERCENT = 50
    }
}

object V8CatalogTitleClassifier {
    fun shouldSkipBeforeContent(title: String, requireStoryChapterTitle: Boolean): Boolean {
        val compact = compactTitle(title)
        if (compact.isBlank()) return false
        if (isStoryChapterTitle(title)) return false
        if (requireStoryChapterTitle) return true
        return definiteNonStoryPatterns.any { pattern -> pattern.containsMatchIn(compact) }
    }

    fun isStoryChapterTitle(title: String): Boolean {
        val spaced = title
            .replace('\u3000', ' ')
            .replace('\u00a0', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (spaced.isBlank()) return false
        val compact = compactTitle(spaced)
        return storyChapterPatterns.any { pattern -> pattern.containsMatchIn(compact) } ||
            spacedStoryChapterPatterns.any { pattern -> pattern.containsMatchIn(spaced) }
    }

    private fun compactTitle(title: String): String {
        return title.replace(Regex("""\s+"""), "").trim()
    }

    private val definiteNonStoryPatterns = listOf(
        Regex("""(?:作者(?:的话|说|感言|有话说|拜年|的话说)|作家(?:的话|说)|作者君(?:的话|说))"""),
        Regex("""(?:完本感言|完结感言|上架感言|写在最后|后记|致谢|写给书友|书友的一封信|茶话会|感谢大家|谢谢大家)"""),
        Regex("""(?:请假|请假条|休息一天|停更|更新说明|更新调整|通知|公告|活动|中奖|抽奖|求票|求月票|求推荐票|求收藏|求追读)"""),
        Regex("""(?:推书|推一本书|推荐一本书|新书(?:已发|发布|起航|预告|推荐|来了)?|第[0-9０-９零〇一二两三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]+册预告)"""),
        Regex("""(?:拜年|新春快乐|春节快乐|元旦快乐|新年快乐|冬至快乐|中秋快乐|国庆快乐|除夕快乐|元宵快乐|端午快乐|七夕快乐|圣诞快乐|祝大家.*快乐|祝书友.*快乐)"""),
        Regex("""^番外$"""),
        Regex("""(?:番外(?:说明|通知|预告|发布|更新)|番外[+＋].*(?:新书|推书|感言|通知)|番外)""")
    )

    private val storyChapterPatterns = listOf(
        Regex("""第[0-9０-９零〇一二两三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]+[章节回篇话]"""),
        Regex("""^(?:chapter|chap\.?|ch\.?)[0-9０-９]+""", RegexOption.IGNORE_CASE),
        Regex("""^番(?:外)?[0-9０-９零〇一二两三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]+"""),
        Regex("""^(?:楔子|序章|引子|序幕|终章|尾声)(?:$|[：:、，,.\-].*)""")
    )

    private val spacedStoryChapterPatterns = listOf(
        Regex("""^\s*[0-9０-９]{1,5}\s*[.、]\s*\S+"""),
        Regex("""^\s*[0-9０-９]{1,5}\s+(?![年月日])\S+"""),
        Regex("""^\s*(?:chapter|chap\.?|ch\.?)\s*[0-9０-９]+""", RegexOption.IGNORE_CASE)
    )
}

data class V8ValidationChapter(
    val index: Int,
    val title: String
)

data class V8ValidationPlan(
    val analysisPositions: List<Int>,
    val targetPositions: Set<Int>,
    val contextPositions: Set<Int>,
    val targetIndexes: Set<Int>,
    val contextIndexes: Set<Int>,
    val rolesByPosition: Map<Int, String>,
    val rolesByChapterIndex: Map<Int, String>,
    val usableContext: Int
) {
    companion object {
        val EMPTY = V8ValidationPlan(
            analysisPositions = emptyList(),
            targetPositions = emptySet(),
            contextPositions = emptySet(),
            targetIndexes = emptySet(),
            contextIndexes = emptySet(),
            rolesByPosition = emptyMap(),
            rolesByChapterIndex = emptyMap(),
            usableContext = 0
        )
    }
}
