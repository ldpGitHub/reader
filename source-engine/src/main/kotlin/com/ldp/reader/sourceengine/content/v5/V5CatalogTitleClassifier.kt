package com.ldp.reader.sourceengine.content.v5

object V5CatalogTitleClassifier {
    fun shouldSkipBeforeContent(title: String, requireStoryChapterTitle: Boolean): Boolean {
        val compact = compactTitle(title)
        if (compact.isBlank()) return false
        if (isStoryChapterTitle(title)) return false
        if (requireStoryChapterTitle) return true
        return definiteNonStoryPatterns.any { pattern -> pattern.containsMatchIn(compact) }
    }

    fun isDefiniteNonStoryTitle(title: String): Boolean {
        return shouldSkipBeforeContent(title, requireStoryChapterTitle = false)
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
