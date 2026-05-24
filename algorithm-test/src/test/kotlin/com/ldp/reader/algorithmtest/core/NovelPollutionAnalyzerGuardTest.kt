package com.ldp.reader.algorithmtest.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovelPollutionAnalyzerGuardTest {
    @Test
    fun requiresCrossChapterTermsForFingerprint() {
        val chapters = (0 until 6).map { index ->
            ChapterInput(
                index = index,
                title = "第${index + 1}章",
                content = normalParagraph(repeat = 18) + if (index == 5) "陈迹一时间 看向远处。" else ""
            )
        }

        val report = NovelPollutionAnalyzer().analyze("测试书", "作者", chapters)
        val featureTexts = report.fingerprint.coreFeatures.map { feature -> feature.text }.toSet()

        assertTrue(featureTexts.contains("陈迹"))
        assertFalse(featureTexts.contains("陈迹一"))
        assertTrue(featureTexts.contains("青云宗"))
        assertFalse(featureTexts.contains("青云"))
    }

    @Test
    fun suppressesCharacterBodyPartFragments() {
        val chapters = (0 until 8).map { index ->
            ChapterInput(
                index = index,
                title = "第${index + 1}章",
                content = buildString {
                    repeat(60) {
                        append("萧炎心头微沉，萧炎手掌紧握，萧炎脸色平静。")
                        append("萧炎 与 古元 在 星陨阁 商议 斗帝 源气。")
                    }
                }
            )
        }

        val report = NovelPollutionAnalyzer().analyze("斗破苍穹", "天蚕土豆", chapters)
        val featureTexts = report.fingerprint.coreFeatures.map { feature -> feature.text }.toSet()

        assertTrue(featureTexts.contains("萧炎"))
        assertFalse(featureTexts.contains("萧炎心"))
        assertFalse(featureTexts.contains("萧炎手"))
        assertFalse(featureTexts.contains("萧炎脸"))
    }

    @Test
    fun suppressesCommonAdjectivesAsCharacterNames() {
        val chapters = (0 until 8).map { index ->
            ChapterInput(
                index = index,
                title = "第${index + 1}章",
                content = buildString {
                    repeat(60) {
                        append("陆长安 覆海真君 碧海商盟 天星海 长青功。")
                        append("众人 很高兴，神色 古怪，却继续 商议 海神殿。")
                    }
                }
            )
        }

        val report = NovelPollutionAnalyzer().analyze("我在修仙界万古长青", "快餐店", chapters)
        val featureTexts = report.fingerprint.coreFeatures.map { feature -> feature.text }.toSet()

        assertTrue(featureTexts.contains("陆长安"))
        assertFalse(featureTexts.contains("高兴"))
        assertFalse(featureTexts.contains("古怪"))
    }

    @Test
    fun surnameRecallFragmentsDoNotBecomeAlienEvidenceWithoutStructure() {
        val chapters = normalBookChapters() + ChapterInput(
            index = 8,
            title = "同书洪灾救人",
            content = normalParagraph(repeat = 95) + buildString {
                repeat(18) {
                    append("梁渠带人救灾，乡民喊着白瞎伱们吃那么多饭，另外两个青年哈哈大笑。")
                    append("颜庆山与卫绍还在堤坝旁巡视，肥鲶鱼翻出水面。")
                }
            }
        )

        val report = analyzerForTests().analyze("从水猴子开始成神", "甲壳蚁", chapters)

        assertFalse(report.suggestions.any { suggestion -> suggestion.chapterIndex == 8 })
    }

    @Test
    fun suffixAlienEntitiesMustBeNewAgainstChapterPrefix() {
        val chapters = normalBookChapters() + ChapterInput(
            index = 8,
            title = "同章支线",
            content = buildString {
                repeat(55) {
                    append("关欢 与 蒙川女 在 天墟道城 商议，泉青成 盯住 拜越。")
                    append("沉迟 和 长温 已经 合围，关欢 准备 突围。")
                }
                repeat(65) {
                    append("蒙川女 卷起 拜越 和 关欢 离开，泉青成 的 死亡门 被 打碎。")
                    append("沉迟 与 长温 追了过去，天墟道城 仍旧 封锁。")
                }
            }
        )

        val report = analyzerForTests().analyze("神话之后", "鹅是老五", chapters)

        assertFalse(
            "same-chapter local arc should be absorbed\n${report.humanSummary()}\n${report.logs.joinToString("\n")}",
            report.suggestions.any { suggestion -> suggestion.chapterIndex == 8 }
        )
    }

    @Test
    fun keepsSameWorldContinuationWithoutStrongAlienEvidence() {
        val chapters = doupoBookChapters() + ChapterInput(
            index = 8,
            title = "后记",
            content = buildString {
                repeat(80) {
                    append("萧炎 古元 药老 斗帝 源气 星陨阁 炎盟。")
                    append("萧炎 再次 回望 斗气大陆，众人 进入 新世界。")
                }
            }
        )

        val report = analyzerForTests().analyze("斗破苍穹", "天蚕土豆", chapters)

        assertFalse(report.suggestions.any { suggestion -> suggestion.chapterIndex == 8 })
    }

    @Test
    fun ignoresAlienRunBeforeJudgmentArea() {
        val chapters = normalBookChapters() + ChapterInput(
            index = 8,
            title = "前段插入",
            content = alienParagraph(repeat = 55) + normalParagraph(repeat = 120)
        )

        val report = analyzerForTests().analyze("测试书", "作者", chapters)

        assertFalse(report.suggestions.any { suggestion -> suggestion.chapterIndex == 8 })
    }

    @Test
    fun detectsSuffixPollutionAfterJudgmentArea() {
        val chapters = normalBookChapters() + ChapterInput(
            index = 8,
            title = "后段混入",
            content = normalParagraph(repeat = 120) + alienParagraph(repeat = 70)
        )

        val report = analyzerForTests().analyze("测试书", "作者", chapters)
        val suggestion = report.suggestions.firstOrNull { item -> item.chapterIndex == 8 }

        assertTrue(
            "expected suffix pollution suggestion\n${report.humanSummary()}\n${report.logs.joinToString("\n")}",
            suggestion != null
        )
        assertTrue("expected suffix offset after first third", suggestion!!.startOffset >= chapters.last().content.length / 3)
    }

    @Test
    fun termStatsDiskCacheKeepsAnalyzerOutputEquivalent() {
        val chapters = normalBookChapters() + ChapterInput(
            index = 8,
            title = "后段混入",
            content = normalParagraph(repeat = 120) + alienParagraph(repeat = 70)
        )
        val config = AlgorithmConfig(
            chunkSize = 800,
            chunkOverlap = 120,
            minFeatureFrequency = 3,
            minFeatureChapterCount = 2,
            refineRounds = 1,
            minSuffixChunks = 1
        )
        val baseline = NovelPollutionAnalyzer(config).analyze("测试书", "作者", chapters)
        val cacheDir = File("build/tmp/algorithm-test-v5-term-stats-${System.nanoTime()}")
        val progress = ArrayList<String>()
        val cached = NovelPollutionAnalyzer(
            config.copy(
                termStatsActiveCacheEntries = 1,
                termStatsMemoryCacheEntries = 1,
                termStatsDiskCacheDirectory = cacheDir
            )
        ).analyze("测试书", "作者", chapters, progress = progress::add)

        assertEquals(
            baseline.suggestions.map { suggestion -> suggestion.chapterIndex to suggestion.stateType },
            cached.suggestions.map { suggestion -> suggestion.chapterIndex to suggestion.stateType }
        )
        assertEquals(
            baseline.fingerprint.coreFeatures.map { feature -> feature.text to feature.weight },
            cached.fingerprint.coreFeatures.map { feature -> feature.text to feature.weight }
        )
        assertTrue(cacheDir.walkTopDown().any { file -> file.isFile && file.extension == "bin" })
        assertTrue(
            progress.any { line ->
                line.startsWith("term_stats_cache") && Regex("""diskHits=([1-9]\d*)""").containsMatchIn(line)
            }
        )
    }

    private fun analyzerForTests(): NovelPollutionAnalyzer {
        return NovelPollutionAnalyzer(
            AlgorithmConfig(
                chunkSize = 800,
                chunkOverlap = 120,
                minFeatureFrequency = 3,
                minFeatureChapterCount = 2,
                refineRounds = 1,
                minSuffixChunks = 1
            )
        )
    }

    private fun normalBookChapters(): List<ChapterInput> {
        return (0 until 8).map { index ->
            ChapterInput(
                index = index,
                title = "第${index + 1}章",
                content = normalParagraph(repeat = 80)
            )
        }
    }

    private fun doupoBookChapters(): List<ChapterInput> {
        return (0 until 8).map { index ->
            ChapterInput(
                index = index,
                title = "第${index + 1}章",
                content = buildString {
                    repeat(80) {
                        append("萧炎 药老 古元 星陨阁 炎盟 斗帝 源气 异火。")
                        append("萧炎 与 药老 在 星陨阁 商议。")
                    }
                }
            )
        }
    }

    private fun normalParagraph(repeat: Int): String {
        return buildString {
            repeat(repeat) {
                append("陈迹前往青云宗，施展霄剑诀，抵达落星谷，祭出玄冰剑，突破筑基期。")
                append("陈迹与青云宗同行。")
            }
        }
    }

    private fun alienParagraph(repeat: Int): String {
        return buildString {
            repeat(repeat) {
                append("叶辰进入星辉集团，董事会设在江州城，顾婉儿签下豪门合同。")
                append("叶辰与顾婉儿进入星辉集团。")
            }
        }
    }
}
