package com.ldp.reader.algorithmtest.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogFusionProbeTest {
    @Test
    fun prefersLongerContinuousCatalog() {
        val report = CatalogFusionProbe().analyze(
            listOf(
                NamedCatalog("short", listOf("第一章 开始", "第二章 入门")),
                NamedCatalog(
                    "long",
                    listOf("第一章 开始", "第二章 入门", "第三章 山门", "第四章 归来")
                )
            )
        )

        assertEquals("long", report.best?.sourceName)
        assertEquals(4, report.fusedTitles.size)
        assertTrue(report.humanSummary().contains("Best: long"))
    }
}
