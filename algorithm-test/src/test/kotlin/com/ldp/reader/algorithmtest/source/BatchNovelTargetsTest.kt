package com.ldp.reader.algorithmtest.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchNovelTargetsTest {
    @Test
    fun targetPoolContainsExpandedNovelSet() {
        val all = BatchNovelTargets.all
        val serial = BatchNovelTargets.serial
        val completed = BatchNovelTargets.completed

        assertTrue("expected at least seventy-nine serial targets", serial.size >= 79)
        assertTrue("expected at least forty completed targets", completed.size >= 40)
        assertTrue("expected at least one hundred nineteen total targets", all.size >= 119)
        assertEquals(
            "target title/author pairs must be unique",
            all.size,
            all.map { target -> target.title to target.author }.toSet().size
        )
        all.forEach { target ->
            assertTrue("blank title in target $target", target.title.isNotBlank())
            assertTrue("blank author in target $target", target.author.isNotBlank())
        }
    }

    @Test
    fun targetPoolIncludesRequestedSerialNovels() {
        val requested = setOf(
            "叩问仙道" to "雨打青石",
            "苟在武道世界成圣" to "在水中的纸老虎",
            "苟在两界修仙" to "文抄公"
        )

        val actual = BatchNovelTargets.serial.map { target -> target.title to target.author }.toSet()

        requested.forEach { target ->
            assertTrue("missing requested target $target", target in actual)
        }
    }

    @Test
    fun targetPoolHasTopUpWindowAfterFirstHundredTargets() {
        val topUpWindow = BatchNovelTargets.all.drop(100).take(12)

        assertEquals("expected twelve top-up targets after the first hundred", 12, topUpWindow.size)
    }

    @Test
    fun sourceExperimentSamplesOneHundredChaptersByDefault() {
        val config = SourceExperimentConfig(
            title = "测试书",
            author = "作者",
            sourceJson = "[]"
        )

        assertTrue(
            "expected 100 selected chapters by default",
            config.maxFingerprintChapters + config.maxTailChapters >= 100
        )
    }
}
