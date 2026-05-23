package com.ldp.reader.sourceengine.content.v5

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5ChapterValidationPlannerTest {
    private val planner = V5ChapterValidationPlanner()

    @Test
    fun targetReplayPlanKeepsExpandedTailWindowAndBookMemory() {
        val chapters = chapters(1_000)

        val plan = planner.selectChapters(chapters) { _, _ -> storyContent() }

        assertEquals(488, plan.targetPositions.minOrNull())
        assertEquals(162, plan.targetPositions.size)
        assertTrue((840 until 1_000).all { position -> position in plan.targetPositions })
        assertTrue(488 in plan.targetPositions)
        assertTrue((0 until 32).all { position -> position !in plan.targetPositions })
        assertEquals(V5ChapterValidationPlanner.ROLE_TARGET_EXTENDED, plan.rolesByPosition[488])
        assertEquals(V5ChapterValidationPlanner.ROLE_TARGET_EXTENDED, plan.rolesByPosition[744])
        assertEquals(V5ChapterValidationPlanner.ROLE_TARGET_TAIL, plan.rolesByPosition[840])
        assertEquals(V5ChapterValidationPlanner.ROLE_TARGET_RECENT, plan.rolesByPosition[998])
        assertEquals(V5ChapterValidationPlanner.ROLE_TARGET_RECENT, plan.rolesByPosition[999])
        assertEquals(V5ChapterValidationPlanner.ROLE_LONG_ANCHOR, plan.rolesByPosition[0])
        assertEquals(V5ChapterValidationPlanner.ROLE_MID_CONTEXT, plan.rolesByPosition[350])
        assertEquals(V5ChapterValidationPlanner.ROLE_NEAR_CONTEXT, plan.rolesByPosition[540])
        assertTrue(plan.usableContext >= V5ChapterValidationPlanner.MIN_USABLE_CONTEXT_CHAPTERS)
        assertTrue(plan.targetIndexes.all { index -> index !in plan.contextIndexes })
    }

    @Test
    fun targetReplayPlanCoversBadTailBeforeFormerHundredWindow() {
        val chapters = chapters(1_095)

        val plan = planner.selectChapters(chapters) { _, _ -> storyContent() }

        assertTrue(961 in plan.targetPositions)
        assertTrue(990 in plan.targetPositions)
        assertTrue(1_040 in plan.targetPositions)
        assertTrue(plan.diagnostics.any { line ->
            line.startsWith("v5.plan.start") && line.contains("tailStart=935")
        })
    }

    @Test
    fun contextBackfillUsesQualityGateUntilEightUsableContextChapters() {
        val chapters = chapters(20)
        val cleanIndexes = setOf(0, 1, 2, 3, 5, 6, 7, 8)

        val plan = planner.selectChapters(chapters) { _, chapter ->
            if (chapter.index in cleanIndexes) {
                storyContent()
            } else {
                badExtractionContent()
            }
        }

        assertEquals(8, plan.usableContext)
        assertTrue(plan.rolesByPosition.values.any { role -> role == V5ChapterValidationPlanner.ROLE_MEMORY_BACKFILL })
        assertTrue(plan.diagnostics.any { line -> line.startsWith("v5.plan.backfill.accept") })
        assertFalse(plan.contextIndexes.any { index -> index in plan.targetIndexes })
    }

    @Test
    fun emitsPlannerDiagnosticsToSink() {
        val diagnostics = ArrayList<String>()

        val plan = planner.selectChapters(
            chapters = chapters(120),
            diagnosticSink = V5DiagnosticSink { line -> diagnostics.add(line) }
        ) { _, _ ->
            storyContent()
        }

        assertTrue(plan.diagnostics.any { line -> line.startsWith("v5.plan.start") })
        assertTrue(diagnostics.any { line -> line.startsWith("v5.plan.targets") })
        assertTrue(diagnostics.any { line -> line.startsWith("v5.plan.finish") })
    }

    private fun chapters(count: Int): List<V5ValidationChapter> {
        return (0 until count).map { index -> V5ValidationChapter(index, "第${index + 1}章 正文") }
    }

    private fun storyContent(): String {
        return buildString {
            repeat(40) {
                append("陈迹前往青云宗，施展霄剑诀，抵达落星谷，祭出玄冰剑，突破筑基期。")
                append("陈迹与青云宗同行，众人低声商议，随后继续沿着旧域边缘前行。")
            }
        }
    }

    private fun badExtractionContent(): String {
        return """
            <script>
            function ajaxLog(pramas){
            var xhr = new XMLHttpRequest();
            xhr.open("POST", '/api/user/guest/log', true);
            xhr.send(JSON.stringify(pramas))
            }
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
}
