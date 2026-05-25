package com.ldp.reader.sourceengine.content.v5

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class V5SourceChapterValidatorTest {
    @Test
    fun keepsV5RunArtifactsScopedToOneSource() {
        val validator = V5SourceChapterValidator()
        val chapters = normalBookChapters()

        val result = validator.validate(
            V5SourceRunRequest(
                title = "测试书",
                author = "作者",
                sourceKey = "source-a",
                chapters = chapters
            )
        )

        assertEquals("source-a", result.sourceKey)
        assertEquals(chapters.size, result.marks.size)
        assertTrue(result.report.logs.any { line -> line.contains("quality") })
        assertTrue(result.marks.all { mark -> mark.chapterTitle.isNotBlank() })
    }

    @Test
    fun mapsQualityGateOutputsToChapterMarks() {
        val validator = V5SourceChapterValidator()
        val chapters = normalBookChapters().take(4) + listOf(
            ChapterInput(
                index = 4,
                title = "完结感言",
                content = endingPostscriptSnippet()
            ),
            ChapterInput(
                index = 5,
                title = "第六章 页面壳",
                content = pureBadExtractionSnippet()
            )
        )

        val result = validator.validate(
            V5SourceRunRequest(
                title = "测试书",
                author = "作者",
                sourceKey = "source-a",
                chapters = chapters
            )
        )

        assertEquals(V5ChapterMarkState.NON_STORY, result.marks.first { it.chapterIndex == 4 }.state)
        assertEquals(V5ChapterMarkState.BAD_EXTRACTION, result.marks.first { it.chapterIndex == 5 }.state)
        assertEquals(4, result.latestNormalOrdinal)
        assertEquals(5, result.firstBadTailOrdinal)
    }

    @Test
    fun mapsConfirmedPollutionSuggestionToWrongMark() {
        val validator = V5SourceChapterValidator {
            NovelPollutionAnalyzer(
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
        val chapters = normalBookChapters() + ChapterInput(
            index = 8,
            title = "后段混入",
            content = normalParagraph(repeat = 120) + alienParagraph(repeat = 70)
        )

        val result = validator.validate(
            V5SourceRunRequest(
                title = "测试书",
                author = "作者",
                sourceKey = "source-a",
                chapters = chapters
            )
        )

        val mark = result.marks.first { it.chapterIndex == 8 }
        assertEquals(V5ChapterMarkState.WRONG, mark.state)
        assertEquals(NovelStateOutputType.POLLUTED_SUFFIX, mark.suggestionState)
        assertEquals(8, result.latestNormalOrdinal)
        assertEquals(9, result.firstBadTailOrdinal)
    }

    @Test
    fun detectsEarlyPollutedSuffixAfterShortValidOpening() {
        val validator = V5SourceChapterValidator()
        val chapters = normalBookChapters() + ChapterInput(
            index = 8,
            title = "第九章 取剑",
            content = "陈迹走到老耳朵身边，看着甲板外的大海，低声问起青云宗的旧事。" +
                "\n" +
                alienParagraph(repeat = 50)
        )

        val result = validator.validate(
            V5SourceRunRequest(
                title = "测试书",
                author = "作者",
                sourceKey = "source-a",
                chapters = chapters
            )
        )

        val mark = result.marks.first { it.chapterIndex == 8 }
        assertEquals(debugSummary(result, mark), V5ChapterMarkState.WRONG, mark.state)
        assertTrue(
            debugSummary(result, mark),
            mark.suggestionState == NovelStateOutputType.POLLUTED_SUFFIX ||
                mark.suggestionState == NovelStateOutputType.POLLUTED_RUN
        )
    }

    @Test
    fun detectsQingShanShortFragmentedTakeSwordChapter() {
        val validator = V5SourceChapterValidator()
        val chapters = qingShanContextChapters() + listOf(
            ChapterInput(
                index = 671,
                title = "672、取剑",
                content = qingShanFragmentedTakeSwordChapter()
            ),
            ChapterInput(
                index = 672,
                title = "673、乱流",
                content = qingShanFragmentedFollowUpChapter()
            )
        )

        val result = validator.validate(
            V5SourceRunRequest(
                title = "青山",
                author = "测试作者",
                sourceKey = "55dushu-qingshan",
                chapters = chapters
            )
        )

        val previous = result.marks.first { it.chapterIndex == 670 }
        val mark = result.marks.first { it.chapterIndex == 671 }
        val next = result.marks.first { it.chapterIndex == 672 }
        assertEquals(debugSummary(result, previous), V5ChapterMarkState.NORMAL, previous.state)
        assertEquals(debugSummary(result, mark), V5ChapterMarkState.WRONG, mark.state)
        assertEquals(debugSummary(result, next), V5ChapterMarkState.WRONG, next.state)
        assertTrue(
            debugSummary(result, mark),
            mark.reasons.any { reason ->
                reason.contains(V5_SHORT_FRAGMENTED_FULL_CHAPTER_REASON) ||
                    reason.contains(V5_SHORT_FRAGMENTED_SEGMENT_REASON)
            }
        )
    }

    @Test
    fun keepsNormalEarlySceneTransitionClean() {
        val validator = V5SourceChapterValidator()
        val chapters = normalBookChapters() + ChapterInput(
            index = 8,
            title = "第九章 入谷",
            content = "陈迹离开甲板后，望见远处山门，心中想起老耳朵先前说过的话。" +
                "\n" +
                normalParagraph(repeat = 50)
        )

        val result = validator.validate(
            V5SourceRunRequest(
                title = "测试书",
                author = "作者",
                sourceKey = "source-a",
                chapters = chapters
            )
        )

        val mark = result.marks.first { it.chapterIndex == 8 }
        assertEquals(debugSummary(result, mark), V5ChapterMarkState.NORMAL, mark.state)
    }

    @Test
    fun keepsEarlyNewArcCleanWhenFutureChaptersContinueIt() {
        val validator = V5SourceChapterValidator()
        val chapters = normalBookChapters() + listOf(
            ChapterInput(
                index = 8,
                title = "第九章 白塔",
                content = "陈迹离开甲板后，听老耳朵提起白塔城的旧案。" +
                    "\n" +
                    newArcParagraph(repeat = 50)
            ),
            ChapterInput(
                index = 9,
                title = "第十章 星火令",
                content = newArcParagraph(repeat = 70)
            ),
            ChapterInput(
                index = 10,
                title = "第十一章 林岚",
                content = newArcParagraph(repeat = 70)
            )
        )

        val result = validator.validate(
            V5SourceRunRequest(
                title = "测试书",
                author = "作者",
                sourceKey = "source-a",
                chapters = chapters
            )
        )

        val mark = result.marks.first { it.chapterIndex == 8 }
        assertEquals(debugSummary(result, mark), V5ChapterMarkState.NORMAL, mark.state)
    }

    @Test
    fun emitsRunAndMarkDiagnosticsToSink() {
        val diagnostics = ArrayList<String>()
        val validator = V5SourceChapterValidator()

        validator.validate(
            V5SourceRunRequest(
                title = "测试书",
                author = "作者",
                sourceKey = "source-a",
                chapters = normalBookChapters().take(2),
                diagnosticSink = V5DiagnosticSink(diagnostics::add)
            )
        )

        assertTrue(diagnostics.any { line -> line.startsWith("v5.run.start") })
        assertTrue(diagnostics.any { line -> line.startsWith("v5.run.analyze.finish") })
        assertTrue(diagnostics.any { line -> line.startsWith("v5.mark.finish") })
    }

    @Test
    fun termStatsDiskCacheKeepsValidatorOutputEquivalentAndEmitsDiagnostics() {
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
        val baseline = V5SourceChapterValidator {
            NovelPollutionAnalyzer(config)
        }.validate(
            V5SourceRunRequest(
                title = "测试书",
                author = "作者",
                sourceKey = "source-a",
                chapters = chapters
            )
        )
        val cacheDir = File("build/tmp/v5-term-stats-cache-equivalence-${System.nanoTime()}")
        val diagnostics = ArrayList<String>()
        val cached = V5SourceChapterValidator {
            NovelPollutionAnalyzer(
                config.copy(
                    termStatsActiveCacheEntries = 1,
                    termStatsMemoryCacheEntries = 1,
                    termStatsDiskCacheDirectory = cacheDir
                )
            )
        }.validate(
            V5SourceRunRequest(
                title = "测试书",
                author = "作者",
                sourceKey = "source-a",
                chapters = chapters,
                diagnosticSink = V5DiagnosticSink(diagnostics::add)
            )
        )

        assertEquals(
            baseline.marks.map { mark -> mark.chapterIndex to mark.state },
            cached.marks.map { mark -> mark.chapterIndex to mark.state }
        )
        assertEquals(
            baseline.report.suggestions.map { suggestion -> suggestion.chapterIndex to suggestion.stateType },
            cached.report.suggestions.map { suggestion -> suggestion.chapterIndex to suggestion.stateType }
        )
        assertTrue(cacheDir.walkTopDown().any { file -> file.isFile && file.extension == "bin" })
        assertTrue(
            diagnostics.any { line ->
                line.contains("term_stats_cache") && Regex("""diskHits=([1-9]\d*)""").containsMatchIn(line)
            }
        )
    }

    @Test
    fun returnsOnlyMarkableChapterMarksWhenProvided() {
        val validator = V5SourceChapterValidator()

        val result = validator.validate(
            V5SourceRunRequest(
                title = "测试书",
                author = "作者",
                sourceKey = "source-a",
                chapters = normalBookChapters(),
                markableChapterIndexes = setOf(6, 7)
            )
        )

        assertEquals(setOf(6, 7), result.marks.map { mark -> mark.chapterIndex }.toSet())
    }

    @Test
    fun expandsDenseTailPollutionAcrossNormalGaps() {
        val marks = (0 until 10).map { index ->
            mark(
                index = index,
                state = if (index in setOf(3, 5, 7, 9)) V5ChapterMarkState.WRONG else V5ChapterMarkState.NORMAL
            )
        }

        val expanded = V5TailContiguousPollutionExpander().expand(marks)

        assertEquals(3, expanded.startChapterIndex)
        assertEquals(listOf(4, 6, 8), expanded.filledChapterIndexes)
        assertTrue(
            expanded.marks
                .filter { mark -> mark.chapterIndex in 3..9 }
                .all { mark -> mark.state == V5ChapterMarkState.WRONG }
        )
    }

    @Test
    fun backfillsBoundaryCandidatesBeforeConfirmedBadCluster() {
        val marks = (0 until 14).map { index ->
            mark(
                index = index,
                state = if (index in setOf(10, 11, 13)) V5ChapterMarkState.WRONG else V5ChapterMarkState.NORMAL
            )
        }

        val expanded = V5TailContiguousPollutionExpander().expand(
            marks,
            boundaryCandidates = listOf(
                boundaryCandidate(8),
                boundaryCandidate(9)
            )
        )

        assertEquals(listOf(8, 9), expanded.boundaryFilledChapterIndexes)
        assertEquals(V5ChapterMarkState.WRONG, expanded.marks.first { it.chapterIndex == 8 }.state)
        assertEquals(V5ChapterMarkState.WRONG, expanded.marks.first { it.chapterIndex == 9 }.state)
        assertEquals(V5ChapterMarkState.NORMAL, expanded.marks.first { it.chapterIndex == 7 }.state)
    }

    @Test
    fun backfillsConnectedBoundaryCandidateChainBeforeConfirmedBadCluster() {
        val marks = (0 until 16).map { index ->
            mark(
                index = index,
                state = if (index in setOf(12, 13, 15)) V5ChapterMarkState.WRONG else V5ChapterMarkState.NORMAL
            )
        }

        val expanded = V5TailContiguousPollutionExpander().expand(
            marks,
            boundaryCandidates = (7..11).map { index -> boundaryCandidate(index) }
        )

        assertEquals((7..11).toList(), expanded.boundaryFilledChapterIndexes)
        assertTrue(
            expanded.marks
                .filter { mark -> mark.chapterIndex in 7..15 }
                .all { mark -> mark.state == V5ChapterMarkState.WRONG }
        )
        assertEquals(V5ChapterMarkState.NORMAL, expanded.marks.first { it.chapterIndex == 6 }.state)
    }

    @Test
    fun stopsBoundaryCandidateChainAtCleanBarrier() {
        val marks = (0 until 16).map { index ->
            mark(
                index = index,
                state = if (index in setOf(12, 13, 15)) V5ChapterMarkState.WRONG else V5ChapterMarkState.NORMAL
            )
        }

        val expanded = V5TailContiguousPollutionExpander().expand(
            marks,
            boundaryCandidates = listOf(8, 9, 11).map { index -> boundaryCandidate(index) }
        )

        assertEquals(listOf(11), expanded.boundaryFilledChapterIndexes)
        assertEquals(V5ChapterMarkState.NORMAL, expanded.marks.first { it.chapterIndex == 9 }.state)
        assertEquals(V5ChapterMarkState.NORMAL, expanded.marks.first { it.chapterIndex == 10 }.state)
        assertEquals(V5ChapterMarkState.WRONG, expanded.marks.first { it.chapterIndex == 11 }.state)
    }

    @Test
    fun doesNotBackfillSameBookArcAbsorbedBoundaryCandidate() {
        val marks = (0 until 14).map { index ->
            mark(
                index = index,
                state = if (index in setOf(10, 11, 13)) V5ChapterMarkState.WRONG else V5ChapterMarkState.NORMAL
            )
        }

        val expanded = V5TailContiguousPollutionExpander().expand(
            marks,
            boundaryCandidates = listOf(
                boundaryCandidate(9, tailBackfillEligible = false)
            )
        )

        assertTrue(expanded.boundaryFilledChapterIndexes.isEmpty())
        assertEquals(V5ChapterMarkState.NORMAL, expanded.marks.first { it.chapterIndex == 9 }.state)
    }

    @Test
    fun doesNotBackfillBoundaryCandidateWithoutFollowingBadCluster() {
        val marks = (0 until 14).map { index ->
            mark(
                index = index,
                state = if (index == 10) V5ChapterMarkState.WRONG else V5ChapterMarkState.NORMAL
            )
        }

        val expanded = V5TailContiguousPollutionExpander().expand(
            marks,
            boundaryCandidates = listOf(boundaryCandidate(9))
        )

        assertTrue(expanded.boundaryFilledChapterIndexes.isEmpty())
        assertEquals(V5ChapterMarkState.NORMAL, expanded.marks.first { it.chapterIndex == 9 }.state)
    }

    @Test
    fun fillsSmallInternalNormalGapBetweenConfirmedBadRuns() {
        val marks = (0 until 14).map { index ->
            mark(
                index = index,
                state = if (index in setOf(3, 4, 6, 7)) V5ChapterMarkState.WRONG else V5ChapterMarkState.NORMAL
            )
        }

        val expanded = V5TailContiguousPollutionExpander().expand(
            marks,
            boundaryCandidates = listOf(boundaryCandidate(5))
        )

        assertTrue(5 in expanded.boundaryFilledChapterIndexes + expanded.gapFilledChapterIndexes)
        assertEquals(V5ChapterMarkState.WRONG, expanded.marks.first { it.chapterIndex == 5 }.state)
    }

    @Test
    fun doesNotFillInternalGapWithoutCandidateEvidence() {
        val marks = (0 until 14).map { index ->
            mark(
                index = index,
                state = if (index in setOf(3, 4, 6, 7)) V5ChapterMarkState.WRONG else V5ChapterMarkState.NORMAL
            )
        }

        val expanded = V5TailContiguousPollutionExpander().expand(marks)

        assertTrue(expanded.gapFilledChapterIndexes.isEmpty())
        assertEquals(V5ChapterMarkState.NORMAL, expanded.marks.first { it.chapterIndex == 5 }.state)
    }

    @Test
    fun doesNotFillSingleCleanGapBetweenIsolatedWrongMarks() {
        val marks = (0 until 6).map { index ->
            mark(
                index = index,
                state = if (index in setOf(2, 4)) V5ChapterMarkState.WRONG else V5ChapterMarkState.NORMAL
            )
        }

        val expanded = V5TailContiguousPollutionExpander().expand(marks)

        assertTrue(expanded.gapFilledChapterIndexes.isEmpty())
        assertEquals(V5ChapterMarkState.NORMAL, expanded.marks.first { it.chapterIndex == 3 }.state)
    }

    @Test
    fun fillsBridgeGapWhenShortFragmentedFullChapterAnchorsWrongNeighbor() {
        val marks = (0 until 6).map { index ->
            when (index) {
                2 -> mark(
                    index = index,
                    state = V5ChapterMarkState.WRONG,
                    reasons = listOf(V5_SHORT_FRAGMENTED_FULL_CHAPTER_REASON)
                )
                4 -> mark(index = index, state = V5ChapterMarkState.WRONG)
                else -> mark(index = index, state = V5ChapterMarkState.NORMAL)
            }
        }

        val expanded = V5TailContiguousPollutionExpander().expand(marks)

        assertEquals(listOf(3), expanded.gapFilledChapterIndexes)
        assertEquals(V5ChapterMarkState.WRONG, expanded.marks.first { it.chapterIndex == 3 }.state)
    }

    @Test
    fun detectsOnlyClearlyDifferentCrossSourceText() {
        val sameBookLeft = normalParagraph(repeat = 80)
        val sameBookRight = normalParagraph(repeat = 80).replace("陈迹", "陈迹")
        val alien = alienParagraph(repeat = 80)

        assertTrue(V5SourceTextSimilarity.score(sameBookLeft, sameBookRight) > 0.90)
        assertTrue(V5SourceTextSimilarity.clearlyDissimilar(sameBookLeft, alien))
    }

    @Test
    fun replayAuditedRawCorpusCaseWithProductionValidator() {
        assumeTrue(
            "Set -Dv5RawCorpusParity=true to replay audited raw corpus with production validator.",
            System.getProperty("v5RawCorpusParity") == "true"
        )
        val corpusPath = "algorithm-test/test-datasets/raw-corpus-101-bundle/raw-corpus-101/" +
            "device-full/extracted-wsl/fetch-batch-1779484863140/016-叩问仙道-1779485161947"
        val root = listOf(File(corpusPath), File("../$corpusPath"))
            .firstOrNull { file -> file.isDirectory }
            ?: File(corpusPath)
        assertTrue("raw corpus case missing: ${root.absolutePath}", root.isDirectory)
        val chapterFiles = File(root, "chapters")
            .listFiles { file -> file.isFile && file.extension == "txt" }
            .orEmpty()
            .sortedWith(compareBy({ parseChapterIndex(it.name) }, { it.name }))
        val planner = V5ChapterValidationPlanner()
        val plan = planner.selectChapters(
            chapters = chapterFiles.map { file ->
                V5ValidationChapter(parseChapterIndex(file.name), parseChapterTitle(file.name))
            },
            readContent = { position, _ -> chapterFiles[position].readText(Charsets.UTF_8) }
        )
        val selectedChapters = plan.analysisPositions.map { position ->
            val file = chapterFiles[position]
            ChapterInput(
                index = parseChapterIndex(file.name),
                title = parseChapterTitle(file.name),
                content = file.readText(Charsets.UTF_8)
            )
        }

        val result = V5SourceChapterValidator().validate(
            V5SourceRunRequest(
                title = "叩问仙道",
                author = "雨打青石",
                sourceKey = "raw-corpus-016",
                chapters = selectedChapters,
                seedChapterIndexes = plan.contextIndexes
            )
        )
        val output = File("build/tmp/v5-production-parity/book-016-marks.tsv").apply {
            parentFile.mkdirs()
        }
        val marksByIndex = result.marks.associateBy { mark -> mark.chapterIndex }
        output.writeText(
            buildString {
                appendLine("index\trole\ttitle\tstate\tquality\tsuggestion\taction\tconfidence")
                plan.analysisPositions.forEach { position ->
                    val file = chapterFiles[position]
                    val index = parseChapterIndex(file.name)
                    val mark = marksByIndex[index]
                    appendLine(
                        listOf(
                            index.toString(),
                            plan.rolesByChapterIndex[index].orEmpty(),
                            parseChapterTitle(file.name),
                            mark?.state?.name.orEmpty(),
                            mark?.qualityType?.name.orEmpty(),
                            mark?.suggestionState?.name.orEmpty(),
                            mark?.action?.name.orEmpty(),
                            mark?.confidence?.let { "%.3f".format(it) }.orEmpty()
                        ).joinToString("\t")
                    )
                }
                appendLine()
                appendLine("reportSuggestions=${result.report.suggestions.map { it.chapterIndex }.sorted().joinToString(",")}")
                appendLine("reportSuggestionCount=${result.report.suggestions.size}")
            }
        )

        assertEquals(V5ChapterMarkState.WRONG, marksByIndex[2783]?.state)
        assertEquals(V5ChapterMarkState.WRONG, marksByIndex[2784]?.state)
        assertEquals(V5ChapterMarkState.WRONG, marksByIndex[2785]?.state)
        assertEquals(V5ChapterMarkState.WRONG, marksByIndex[2786]?.state)
        assertEquals(V5ChapterMarkState.WRONG, marksByIndex[2787]?.state)
        assertTrue(output.isFile)
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

    private fun newArcParagraph(repeat: Int): String {
        return buildString {
            repeat(repeat) {
                append("林岚抵达白塔城，取出星火令，与陈迹商议青云宗密藏。")
                append("白塔城的旧案牵连玄冰剑，林岚决定同行。")
            }
        }
    }

    private fun qingShanContextChapters(): List<ChapterInput> {
        return (663..670).map { index ->
            ChapterInput(
                index = index,
                title = "${index + 1}、海上旧事",
                content = qingShanNormalParagraph(repeat = 70)
            )
        }
    }

    private fun qingShanNormalParagraph(repeat: Int): String {
        return buildString {
            repeat(repeat) {
                append("陈迹从艉楼出来，老耳朵坐在甲板边看海，楼船沿着镜城港外的潮线向前。")
                append("靖海侯府的密谍司仍在追查旧案，陈迹把取剑之事藏在心底。")
            }
        }
    }

    private fun qingShanFragmentedTakeSwordChapter(): String {
        return """
            陈迹从艉楼出来时，海风正从甲板外灌进来，吹得灯笼轻轻摇晃。
            老耳朵没有赌钱，他独自坐在船尾，看着远处黑沉沉的大海。
            陈迹走到他身边：“您喜欢看大海？”
            老耳朵依旧看着大海：“年轻时喜欢，老了以后也只剩下喜欢。”
            陈迹沉默片刻，低声问起那把剑的来历，老耳朵却只是摆摆手，让他自己去取。
            大家都不是傻子，屋中的烛火忽然跳了一下，像是被另一段故事强行接了进来。
            时有些语无伦次的少年，柳琴心按住桌角，说自己昨夜看见北岭雪崩。
            韩铁方苦笑着翻出一枚旧铁券，账册上却写着南荒粮道与三千石军粮。
            有人提起城西的戏班，有人又说起荒村里的井，前后话头全都接不上。
            唐军心说这事不能再拖，他把木盒塞进袖中，转身去找早已失踪的县尉。
            廖承安忽然拍案，说南门外的税银其实在春雷观，不该交给那个卖药的道人。
            沈照夜却只盯着窗纸上的影子，问洛河县的灯会为何提前散场。
            苏小满把半页残谱压在茶盏下，残谱上写的又是另一座山里的婚约。
            赵无咎听得头疼，前一句还在说军粮，后一句便跳到矿洞里的铜铃。
        """.trimIndent()
    }

    private fun qingShanFragmentedFollowUpChapter(): String {
        return """
            周远山把一封没有落款的信交给掌柜，信里却夹着半张海图。
            裴映雪说昨夜城门开过三次，守门的兵丁全都不记得来人长相。
            白敬亭在酒楼二层听完，忽然提起北漠商队欠下的盐票。
            众人还没问清盐票的来历，画面又跳到一处荒废驿站。
            驿站墙上刻着旧年诗句，诗句旁边偏偏挂着王府赏银的告示。
            孟秋娘说自己认得那枚印章，可她下一句又讲起江南水灾。
            罗七把刀压在桌面，问谁偷走了铜匣，没人回答这个问题。
            茶博士端来冷茶，冷茶里浮着细碎纸灰，像另一本书剩下的尾页。
            秦渡把纸灰收进瓷瓶，瓷瓶底部却刻着草原部落的狼纹。
            谢兰舟翻开县志，县志第一页写的是海外仙岛的船税。
            顾南栀笑着说这不是船税，是十年前京师赌坊欠下的本金。
            账房先生立刻摇头，转而讲起一位女将军在雪夜丢失的马。
            冯不疑听到这里忽然拔剑，剑锋指向的却是一张婚书。
            婚书上的新郎姓氏被墨涂掉，只剩下旁边三枚铜钱印。
            码头外有人放起纸鸢，纸鸢线又牵出一段完全无关的山神祭。
            这一页像被剪碎重排，所有人都在说话，却没有一句接着上一句。
        """.trimIndent()
    }

    private fun endingPostscriptSnippet(): String {
        return """
            码字码到没什么灵感，想了想，就给完本的太一道果写一下完结感言。
            从23年五月到今年四月，近两年的时间，太一道果总算是完本了。
            感谢一路过来陪伴的书友，谢谢你们的支持。
            最后，就是推一推新书《人在高武，言出法随》了。
            总之就是求收藏，求追读。
        """.trimIndent()
    }

    private fun pureBadExtractionSnippet(): String {
        return """
            第2868章第二计划启动_仙人消失之后全文免费阅读 - 潇湘书院
            <script>
            function ajaxLog(pramas){
            var xhr = new XMLHttpRequest();
            xhr.open("POST", '/api/user/guest/log', true);
            xhr.setRequestHeader('Content-type', 'application/json');
            xhr.send(JSON.stringify(pramas))
            }
            document.querySelector('#reader').appendChild(script);
            window.localStorage.setItem('source', 'pc_jump');
            </script>
            登录
            首页
            上一章
            下一章
            返回目录
            相关推荐
            手机阅读
        """.trimIndent()
    }

    private fun debugSummary(result: V5SourceRunResult, mark: V5ChapterMarkResult): String {
        return buildString {
            appendLine("mark=$mark")
            appendLine("suggestions=${result.report.suggestions}")
            appendLine("boundary=${result.report.boundaryBackfillCandidates}")
            result.report.logs.takeLast(30).forEach { line -> appendLine(line) }
        }
    }

    private fun parseChapterIndex(name: String): Int {
        return Regex("""^(\d+)-""").find(name)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
    }

    private fun parseChapterTitle(name: String): String {
        return name.removeSuffix(".txt").replace(Regex("""^\d+-"""), "").replace('_', ' ')
    }

    private fun mark(
        index: Int,
        state: V5ChapterMarkState,
        reasons: List<String> = emptyList()
    ): V5ChapterMarkResult {
        return V5ChapterMarkResult(
            chapterIndex = index,
            chapterTitle = "第${index + 1}章",
            state = state,
            confidence = 0.95,
            qualityType = ChapterQualityType.CLEAN_STORY,
            suggestionState = if (state == V5ChapterMarkState.WRONG) NovelStateOutputType.POLLUTED_RUN else null,
            action = if (state == V5ChapterMarkState.WRONG) CleanAction.SUGGEST_DELETE else null,
            reasons = reasons
        )
    }

    private fun boundaryCandidate(
        index: Int,
        tailBackfillEligible: Boolean = true
    ): V5BoundaryBackfillCandidate {
        return V5BoundaryBackfillCandidate(
            chapterIndex = index,
            chapterTitle = "第${index + 1}章",
            stateType = NovelStateOutputType.POLLUTED_RUN,
            action = CleanAction.MARK_ONLY,
            confidence = 0.72,
            reasons = listOf("near-miss alien run"),
            tailBackfillEligible = tailBackfillEligible
        )
    }
}
