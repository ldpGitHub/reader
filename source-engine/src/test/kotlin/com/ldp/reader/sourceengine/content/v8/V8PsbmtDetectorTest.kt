package com.ldp.reader.sourceengine.content.v8

import com.ldp.reader.sourceengine.content.v8.V8BgeSemanticModel
import java.io.File
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class V8PsbmtDetectorTest {
    private val detector = V8PsbmtDetector(
        config = V8PsbmtConfig(
            minLowThreshold = 0.015,
            maxLowThreshold = 0.055,
            lowThresholdMinDrop = 0.02,
            normalThresholdMinDrop = 0.01,
            minNormalThresholdGap = 0.01,
            futureRescueThreshold = 0.08,
            tailClusterFutureThreshold = 0.12
        )
    )

    @Test
    fun detectsConstructedPrefixSuffixPollutionWithHardGate() {
        val book = bookChapters()
        val foreign = foreignChapters()
        val failures = ArrayList<String>()

        listOf(96, 136, 176).forEachIndexed { index, cut ->
            val current = chapterText(book[3]).take(cut) + chapterText(foreign[index % foreign.size]).drop(30)
            val result = detector.detect(
                V8PsbmtInput(
                    previousChapters = book.take(3).mapIndexed { chapterIndex, text ->
                        V8ChapterContext(chapterIndex, "book-$chapterIndex", chapterText(text))
                    },
                    current = V8ChapterContext(3, "current", current),
                    nextChapters = listOf(V8ChapterContext(4, "next", chapterText(book[4])))
                )
            )
            if (!result.status.isWrongOrSuspect) {
                failures.add("cut=$cut result=${brief(result)}")
            }
        }

        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun pollutedFutureDoesNotRescueFragmentTail() {
        val book = bookChapters()
        val fragmentTail = buildString {
            repeat(4) { append(foreignChapters().flatten().joinToString("")) }
        }
        val current = chapterText(book[3]).take(136) + fragmentTail
        val next = chapterText(book[4]).take(136) + fragmentTail

        val result = detector.detect(
            V8PsbmtInput(
                previousChapters = book.take(3).mapIndexed { chapterIndex, text ->
                    V8ChapterContext(chapterIndex, "book-$chapterIndex", chapterText(text))
                },
                current = V8ChapterContext(3, "current", current),
                nextChapters = listOf(V8ChapterContext(4, "next", next))
            )
        )

        assertTrue(brief(result), result.status.isWrongOrSuspect)
        assertEquals(brief(result), false, result.evidence["futureRescue"] ?: result.evidence["wholeFutureRescue"])
    }

    @Test
    fun localRuptureCueDoesNotBypassMembershipGate() {
        val detector = V8PsbmtDetector(
            config = V8PsbmtConfig(
                minLowThreshold = 0.015,
                maxLowThreshold = 0.99,
                lowThresholdMinDrop = 0.30,
                normalThresholdMinDrop = 0.20,
                minNormalThresholdGap = 0.01,
                semanticWeight = 0.95,
                identityWeight = 0.05,
                safeSuffixLowRatio = 1.0,
                fragmentRepeatRatio = 1.0,
                futureRescueThreshold = 0.99,
                tailClusterFutureThreshold = 0.99
            ),
            semanticModel = ConstantSemanticModel()
        )
        val book = bookChapters()
        val fragmentTail = buildString {
            repeat(4) { append(foreignChapters().flatten().joinToString("")) }
        }
        val current = chapterText(book[3]).take(136) + fragmentTail

        val result = detector.detect(
            V8PsbmtInput(
                previousChapters = book.take(3).mapIndexed { chapterIndex, text ->
                    V8ChapterContext(chapterIndex, "book-$chapterIndex", chapterText(text))
                },
                current = V8ChapterContext(3, "current", current)
            )
        )

        assertEquals(brief(result), V8PsbmtStatus.NORMAL, result.status)
        assertEquals(brief(result), true, result.evidence["fragmentTail"])
        assertEquals(brief(result), true, result.evidence["suffixSafe"])
    }

    @Test
    fun shortPrefixPollutionStillReportsLowMembership() {
        val book = bookChapters()
        val fragmentTail = buildString {
            repeat(4) { append(foreignChapters().flatten().joinToString("")) }
        }
        val current = chapterText(book[3]).take(104) + fragmentTail

        val result = detector.detect(
            V8PsbmtInput(
                previousChapters = book.take(3).mapIndexed { chapterIndex, text ->
                    V8ChapterContext(chapterIndex, "book-$chapterIndex", chapterText(text))
                },
                current = V8ChapterContext(3, "current", current)
            )
        )

        assertTrue(brief(result), result.status.isWrongOrSuspect)
        assertEquals(brief(result), 1.0, result.evidence["wholeLowRatio"])
    }

    @Test
    fun sameBookChapterStaysNormal() {
        val book = bookChapters()
        val previous = book.take(3).mapIndexed { index, text ->
            V8ChapterContext(index, "prev-$index", chapterText(text))
        }
        val current = chapterText(book[3])
        val next = chapterText(book[4])

        val result = detector.detect(
            V8PsbmtInput(
                previousChapters = previous,
                current = V8ChapterContext(3, "new arc", current),
                nextChapters = listOf(V8ChapterContext(4, "new arc next", next))
            )
        )

        assertEquals(brief(result), V8PsbmtStatus.NORMAL, result.status)
    }

    @Test
    fun reportsBgeTargetBooksValidationWhenEnabled() {
        assumeTrue("set -Dv8BgeTargetValidation=true to run BGE target-book validation",
            System.getProperty("v8BgeTargetValidation", "false") == "true")
        val details = firstExisting(
            "bridge-artifacts/v8-schema21-verify/details.tsv",
            "bridge-artifacts/v8-real-verification/details.tsv",
            "../bridge-artifacts/v8-schema21-verify/details.tsv",
            "../bridge-artifacts/v8-real-verification/details.tsv"
        )
        assumeTrue("missing real-cache details: ${details.absolutePath}", details.isFile)
        val targets = System.getProperty("v8BgeTargetBooks", "青山,叩问仙道,清光宝鉴,仙人消失之后,苟在武道世界成圣,仙都")
            .split(',', '，')
            .map { value -> value.trim() }
            .filter { value -> value.isNotBlank() }
            .toSet()

        val rows = parseDetails(details)
            .filter { row ->
                row.dataset == "device-live" &&
                    row.path.isFile &&
                    row.text.length >= 300 &&
                    isTargetBook(row.book, targets)
            }
        assertTrue("missing target rows", rows.isNotEmpty())

        val groups = rows.groupBy { row -> row.dataset to row.book }
            .mapValues { (_, groupRows) ->
                groupRows.sortedWith(compareBy<Row> { it.chapterNo ?: it.index }.thenBy { it.index })
            }
        val selected = selectExpectedBoundaryRows(groups).toMutableSet()
        rows.filter { row -> row.label == "NORMAL" }.forEach { row -> selected.add(row.key) }
        assertTrue("missing selected rows", selected.isNotEmpty())

        bgeDetector().use { model ->
            val detector = V8PsbmtDetector(semanticModel = model)
            val outDir = File(details.parentFile.parentFile, "v8-bge-target-books-validation").apply { mkdirs() }
            val results = ArrayList<TargetResult>()
            val durations = ArrayList<Long>()

            groups.values.forEach { groupRows ->
                groupRows.forEachIndexed { position, row ->
                    if (row.key !in selected) return@forEachIndexed
                    val previous = groupRows
                        .subList(0, position)
                        .asReversed()
                        .filter { previousRow ->
                            expectedLabel(previousRow) == ExpectedLabel.NORMAL &&
                                isNearbyPrevious(row, previousRow)
                        }
                        .take(8)
                        .asReversed()
                    val next = groupRows.drop(position + 1).take(2)
                    val result = detector.detect(
                        V8PsbmtInput(
                            previousChapters = previous.map { previousRow ->
                                V8ChapterContext(previousRow.index, previousRow.title, previousRow.text)
                            },
                            current = V8ChapterContext(row.index, row.title, row.text),
                            nextChapters = next.map { nextRow ->
                                V8ChapterContext(
                                    nextRow.index,
                                    nextRow.title,
                                    nextRow.text,
                                    trusted = expectedLabel(nextRow) == ExpectedLabel.NORMAL
                                )
                            }
                        )
                    )
                    durations.add(result.ms)
                    results.add(TargetResult(row, expectedLabel(row), previous.size, next.size, result))
                }
            }

            writeTargetReports(outDir, results, durations)

            val normal = results.filter { item -> item.expected == ExpectedLabel.NORMAL }
            val polluted = results.filter { item -> item.expected == ExpectedLabel.POLLUTED }
            val normalWrong = normal.count { item -> item.result.status == V8PsbmtStatus.WRONG_CONFIRMED }
            val normalWrongOrSuspect = normal.count { item -> item.result.status.isWrongOrSuspect }
            val pollutedCaught = polluted.count { item -> item.result.status.isWrongOrSuspect }

            assertTrue(
                "normalWrong=$normalWrong\n${normal.filter { it.result.status == V8PsbmtStatus.WRONG_CONFIRMED }.joinToString("\n") { it.row.book + "\t" + it.row.title + "\t" + brief(it.result) }}",
                normalWrong == 0
            )
            assertTrue(
                "normalWrongOrSuspect=$normalWrongOrSuspect normal=${normal.size}\n${normal.filter { it.result.status.isWrongOrSuspect }.joinToString("\n") { it.row.book + "\t" + it.row.title + "\t" + brief(it.result) }}",
                normalWrongOrSuspect <= 1
            )
            assertTrue(
                "pollutedCaught=$pollutedCaught polluted=${polluted.size}\n${polluted.filter { !it.result.status.isWrongOrSuspect }.joinToString("\n") { it.row.book + "\t" + it.row.title + "\t" + brief(it.result) }}",
                pollutedCaught == polluted.size
            )
            assertTrue("medianMs=${durations.medianLong()} maxMs=${durations.maxOrNull()}", durations.medianLong() <= 2_000)
        }
    }

    private fun writeTargetReports(outDir: File, results: List<TargetResult>, durations: List<Long>) {
        outDir.resolve("summary.txt").writeText(
            buildString {
                val normal = results.filter { item -> item.expected == ExpectedLabel.NORMAL }
                val polluted = results.filter { item -> item.expected == ExpectedLabel.POLLUTED }
                appendLine("V8 BGE target-book validation")
                appendLine("records=${results.size}")
                appendLine("normal=${normal.size}")
                appendLine("polluted=${polluted.size}")
                appendLine("normalWrong=${normal.count { item -> item.result.status == V8PsbmtStatus.WRONG_CONFIRMED }}")
                appendLine("normalWrongOrSuspect=${normal.count { item -> item.result.status.isWrongOrSuspect }}")
                appendLine("pollutedCaught=${polluted.count { item -> item.result.status.isWrongOrSuspect }}")
                appendLine("medianMs=${durations.medianLong()}")
                appendLine("p90Ms=${durations.percentileLong(0.90)}")
                appendLine("maxMs=${durations.maxOrNull() ?: 0}")
            },
            Charsets.UTF_8
        )
        outDir.resolve("results.tsv").writeText(
            buildString {
                appendLine("book\texpected\toldLabel\toldRole\tv8Status\tv8Type\toffset\tconfidence\tms\tprevious\tnext\ttitle\tpath\tevidence")
                results.forEach { item ->
                    appendLine(
                        listOf(
                            item.row.book,
                            item.expected.name,
                            item.row.label,
                            item.row.role,
                            item.result.status.name,
                            item.result.type.name,
                            item.result.offset?.toString().orEmpty(),
                            "%.4f".format(item.result.confidence),
                            item.result.ms.toString(),
                            item.previousCount.toString(),
                            item.nextCount.toString(),
                            item.row.title,
                            item.row.path.path,
                            item.result.evidence.entries.joinToString(";") { (key, value) -> "$key=$value" }
                        ).joinToString("\t")
                    )
                }
            },
            Charsets.UTF_8
        )
    }

    private fun expectedLabel(row: Row): ExpectedLabel {
        val chapterNo = row.chapterNo ?: return ExpectedLabel.NORMAL
        return when {
            row.book == "清光宝鉴" && chapterNo >= 69 -> ExpectedLabel.POLLUTED
            row.book == "青山" && chapterNo >= 684 -> ExpectedLabel.POLLUTED
            row.book == "叩问仙道" && chapterNo >= 2696 -> ExpectedLabel.POLLUTED
            row.book == "仙人消失之后" && (chapterNo == 2879 || chapterNo >= 2881) -> ExpectedLabel.POLLUTED
            row.book == "苟在武道世界成圣" && chapterNo >= 663 -> ExpectedLabel.POLLUTED
            row.book == "苟在两界修仙" && chapterNo >= 415 -> ExpectedLabel.POLLUTED
            row.book == "仙都" -> ExpectedLabel.NORMAL
            else -> ExpectedLabel.NORMAL
        }
    }

    private fun selectExpectedBoundaryRows(groups: Map<Pair<String, String>, List<Row>>): Set<String> {
        val selected = LinkedHashSet<String>()
        groups.values.forEach { groupRows ->
            val firstPollutedIndex = groupRows.indexOfFirst { row -> expectedLabel(row) == ExpectedLabel.POLLUTED }
            if (firstPollutedIndex < 0) return@forEach
            val from = (firstPollutedIndex - 2).coerceAtLeast(0)
            val to = (firstPollutedIndex + 2).coerceAtMost(groupRows.lastIndex)
            for (index in from..to) selected.add(groupRows[index].key)
        }
        return selected
    }

    private fun isNearbyPrevious(row: Row, previous: Row): Boolean {
        val chapterNo = row.chapterNo ?: return true
        val previousNo = previous.chapterNo ?: return true
        return chapterNo > previousNo && chapterNo - previousNo <= 16
    }

    private fun parseDetails(file: File): List<Row> {
        val lines = file.readLines(Charsets.UTF_8)
        val header = lines.first().split('\t')
        return lines.drop(1).mapIndexedNotNull { index, line ->
            val parts = line.split('\t')
            val values = header.mapIndexed { headerIndex, name -> name to parts.getOrElse(headerIndex) { "" } }.toMap()
            val path = File(values.getValue("path"))
            if (!path.isFile) return@mapIndexedNotNull null
            Row(
                index = index,
                dataset = values.getValue("dataset"),
                book = values.getValue("book"),
                label = values.getValue("label"),
                role = values.getValue("role"),
                title = values.getValue("title"),
                path = path,
                chapterNo = chapterNo(values.getValue("title")),
                text = readChapter(path, values.getValue("title"))
            )
        }
    }

    private fun readChapter(file: File, title: String): String {
        val text = file.readText(Charsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n').trim()
        val lines = text.lines()
        if (lines.isEmpty()) return text
        val first = lines.first().trim()
        return if (first.titleKey() == title.titleKey() || first.matches(Regex("""^第.+章.*"""))) {
            lines.drop(1).joinToString("\n")
        } else {
            text
        }
    }

    private fun chapterNo(title: String): Int? {
        Regex("""第\s*([0-9]+)\s*[章节]""").find(title)?.let { return it.groupValues[1].toInt() }
        Regex("""第\s*([一二两三四五六七八九十百千万零〇]+)\s*[章节]""").find(title)?.let {
            return chineseNumber(it.groupValues[1])
        }
        Regex("""^\s*([0-9]+)[、.．]""").find(title)?.let {
            return it.groupValues[1].toInt()
        }
        Regex("""^\s*([一二两三四五六七八九十百千万零〇]+)[、.．]""").find(title)?.let {
            return chineseNumber(it.groupValues[1])
        }
        return null
    }

    private fun chineseNumber(value: String): Int {
        val digits = mapOf('零' to 0, '〇' to 0, '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4, '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9)
        val units = mapOf('十' to 10, '百' to 100, '千' to 1_000, '万' to 10_000)
        var total = 0
        var section = 0
        var number = 0
        value.forEach { char ->
            val digit = digits[char]
            if (digit != null) {
                number = digit
            } else {
                val unit = units[char] ?: return@forEach
                if (unit == 10_000) {
                    total += (section + number) * unit
                    section = 0
                } else {
                    section += (number.takeIf { it > 0 } ?: 1) * unit
                }
                number = 0
            }
        }
        return total + section + number
    }

    private fun bgeDetector(): V8BgeSemanticModel {
        val modelDir = firstExistingDir(
            "bridge-artifacts/models/bge-small-zh-v1.5-onnx",
            "../bridge-artifacts/models/bge-small-zh-v1.5-onnx"
        )
        val model = File(modelDir, "model_quantized.onnx")
        val data = File(modelDir, "model_quantized.onnx_data")
        val vocab = File(modelDir, "vocab.txt")
        assumeTrue("missing BGE model asset: ${modelDir.absolutePath}", model.isFile && data.isFile && vocab.isFile)
        return V8BgeSemanticModel(model, vocab, maxTokens = 160, maxEmbeddingCacheEntries = 2048)
    }

    private fun firstExisting(vararg paths: String): File {
        return paths.map(::File).firstOrNull { file -> file.isFile } ?: File(paths.first())
    }

    private fun firstExistingDir(vararg paths: String): File {
        return paths.map(::File).firstOrNull { file -> file.isDirectory } ?: File(paths.first())
    }

    private fun isTargetBook(book: String, targets: Set<String>): Boolean {
        return targets.any { target ->
            book == target
        }
    }

    private fun String.titleKey(): String {
        return replace(Regex("""[\s\p{Punct}，。！？、；：“”‘’（）【】《》]+"""), "").lowercase()
    }

    private fun brief(result: V8PsbmtResult): String {
        return "status=${result.status} type=${result.type} offset=${result.offset} confidence=${result.confidence} " +
            "evidence=${result.evidence} candidates=" +
            result.candidates.joinToString(prefix = "[", postfix = "]") { candidate ->
                "${candidate.offset}:${"%.3f".format(candidate.score)}:low=${"%.2f".format(candidate.suffixLowRatio)}:safe=${candidate.suffixSafe}"
            }
    }

    private fun chapterText(sentences: List<String>): String {
        return buildString {
            repeat(10) { round ->
                sentences.forEach { sentence -> append(sentence).append("第").append(round).append("轮。") }
            }
        }
    }

    private fun bookChapters(): List<List<String>> {
        return listOf(
            listOf(
                "顾南衣推开武馆后门，雨水顺着檐角落下，演武场上的青砖已经被拳劲震出细纹。",
                "沈听澜抱着刀匣站在廊下，提醒他今日擂台不同往常，三江盟的客人都在暗处观望。",
                "老掌柜翻出一卷泛黄拳谱，上面记着伏虎劲、碎玉步和当年北境一战的残缺注解。"
            ),
            listOf(
                "顾南衣收住呼吸，把气血压回丹田，等铜钟第三声响起才缓缓踏入雨幕。",
                "城西镖局送来的密信还带着火漆，信中只说黑水寨又劫了一批药材。",
                "沈听澜把刀匣扣紧，三江盟的暗号在窗纸上留下两道淡淡划痕。"
            ),
            listOf(
                "三江盟客人坐在武馆正堂，茶盏旁压着一枚黑水寨的旧铜钱。",
                "顾南衣看出铜钱边缘有伏虎劲留下的震纹，心里已经猜到来人目的。",
                "沈听澜低声提醒他，北境旧案和这枚铜钱之间恐怕还有一层牵连。"
            ),
            listOf(
                "雨声忽然变急，顾南衣听见后巷传来短促铜哨，正是三江盟约定的求援信号。",
                "沈听澜先一步推开窗，刀匣中寒光一闪，黑水寨的探子已经越过墙头。",
                "老掌柜没有出声，只把那卷伏虎劲残谱塞进顾南衣怀里。"
            ),
            listOf(
                "天亮以后，顾南衣和沈听澜在城外破庙会合，三江盟的人已经撤得干干净净。",
                "残谱上多出一行细字，指向北境雨夜里失踪的第二批药材。",
                "顾南衣知道黑水寨不会善罢甘休，便决定先回武馆稳住弟子。"
            )
        )
    }

    private fun foreignChapters(): List<List<String>> {
        return listOf(
            listOf(
                "秦含晴刚推开审讯室的门，丁雨已经把监控录像投到墙上，刑警队的红蓝灯在雨夜里不停闪烁。",
                "季泽佑扶着方向盘穿过高架桥，手机里传来蒋骁龙的声音，要求他们立刻封锁城南医院。",
                "司马倩翻出一枚蓝宝石戒指，案件编号、银行流水和酒店房卡被她一并摊在桌面上。"
            ),
            listOf(
                "林若仪坐在直播间的补光灯前，屏幕右侧的订单数字不断跳动，运营群里全是催促改价的消息。",
                "周启明把样衣挂回展架，供应链主管刚从广州打来电话，说仓库条码和合同批次完全对不上。",
                "电梯门打开时，法务部的人已经等在会议室，投影上停着一份并购协议和三页风险清单。"
            ),
            listOf(
                "韩砚站在太空港候机厅，透明穹顶外的货运飞船缓慢升空，机械警卫扫描着每一张通行证。",
                "阿诺把芯片藏进袖口，低声说第九殖民区已经失联，星网只剩下一段被截断的求救信号。",
                "指挥官在全息沙盘上标出跃迁坐标，蓝色光点穿过小行星带，最后停在废弃矿站旁边。"
            )
        )
    }

    private class ConstantSemanticModel : V8SemanticModel {
        override fun build(
            referenceTexts: List<String>,
            currentText: String,
            futureTexts: List<String>,
            config: V8PsbmtConfig
        ): V8SemanticSpace {
            val referenceWindows = referenceTexts.flatMap { text ->
                v8SlidingWindows(text.take(5_000), config.windowSize, config.windowStride, config.minWindowChars)
            }
            val allWindows = referenceWindows +
                v8SlidingWindows(currentText.take(2_800), config.windowSize, config.windowStride, config.minWindowChars) +
                futureTexts.flatMap { text ->
                    v8SlidingWindows(text.take(2_800), config.windowSize, config.windowStride, config.minWindowChars)
                }
            val vector = mapOf("constant" to 1.0)
            return V8SemanticSpace(
                referenceWindows = referenceWindows,
                referenceVectors = referenceWindows.map { vector },
                idf = v8BuildIdf(allWindows, config),
                vectorizer = { vector },
                config = config
            )
        }
    }

    private data class Row(
        val index: Int,
        val dataset: String,
        val book: String,
        val label: String,
        val role: String,
        val title: String,
        val path: File,
        val chapterNo: Int?,
        val text: String
    ) {
        val key: String
            get() = "$dataset\t$book\t$index"
    }

    private data class TargetResult(
        val row: Row,
        val expected: ExpectedLabel,
        val previousCount: Int,
        val nextCount: Int,
        val result: V8PsbmtResult
    )

    private enum class ExpectedLabel {
        NORMAL,
        POLLUTED
    }

    private val V8PsbmtStatus.isWrongOrSuspect: Boolean
        get() = this == V8PsbmtStatus.WRONG_CONFIRMED || this == V8PsbmtStatus.SUSPECT_RECHECK_REQUIRED

    private fun List<Long>.medianLong(): Long {
        if (isEmpty()) return 0
        val sorted = sorted()
        return sorted[sorted.size / 2]
    }

    private fun List<Long>.percentileLong(percentile: Double): Long {
        if (isEmpty()) return 0
        val sorted = sorted()
        return sorted[((sorted.size - 1) * percentile).roundToInt().coerceIn(0, sorted.lastIndex)]
    }
}
