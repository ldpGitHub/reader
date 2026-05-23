package com.ldp.reader.algorithmtest.source

import com.ldp.reader.algorithmtest.core.ChapterInput
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleValidationProbeTest {
    @Test
    fun reportsDuplicateContentAndOrdinalProblems() {
        val report = SampleValidationProbe.analyze(
            listOf(
                ChapterInput(7733, "第7735章 王心妍的所在", "林逸返回北岛。"),
                ChapterInput(7734, "第7236章 再投射一次", "林逸尝试元神投射。"),
                ChapterInput(7745, "第7747章 再投射一次", "林逸尝试元神投射。")
            )
        )

        assertTrue(report.contains("duplicate content"))
        assertTrue(report.contains("ordinal mismatch"))
        assertTrue(report.contains("ordinal regression"))
    }

    @Test
    fun allowsStableOrdinalOffsetFromAuthorNotes() {
        val report = SampleValidationProbe.analyze(
            listOf(
                ChapterInput(379, "第364章 坐镇一方", "陆长安坐镇洞府。"),
                ChapterInput(519, "第499章 妙用无穷，灵宝器灵", "陆长安祭出灵宝。"),
                ChapterInput(520, "第500章 一劫灵宝，牛刀小试", "陆长安试宝。"),
                ChapterInput(522, "请假两天", "请假说明。"),
                ChapterInput(523, "第502章 商盟大典，星海风云", "商盟大典开启。")
            )
        )

        assertTrue(report.contains("Sample validation issues: 0"))
    }
}
