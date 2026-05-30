package com.ldp.reader.sourceengine.content.v8

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V8ValidationPlannerTest {
    private val planner = V8ValidationPlanner()

    @Test
    fun selectsBookMemoryForShortBookWhoseWholeCatalogIsTailRiskWindow() {
        val chapters = (0 until 97).map { index ->
            V8ValidationChapter(index = index, title = "第${index + 1}章 青灯照影")
        }

        val plan = planner.selectChapters(chapters) { position, _ ->
            storyContent(position)
        }

        assertEquals(97, plan.targetIndexes.size)
        assertEquals(V8ValidationPlanner.MIN_USABLE_CONTEXT_CHAPTERS, plan.usableContext)
        assertEquals(V8ValidationPlanner.MIN_USABLE_CONTEXT_CHAPTERS, plan.contextPositions.size)
        assertTrue(plan.contextPositions.all { position -> position in chapters.indices })
        assertTrue(plan.contextPositions.any { position -> position < 97 / 2 })
    }

    @Test
    fun keepsNonStoryTitlesOutOfTargetsAndContext() {
        val chapters = listOf(
            V8ValidationChapter(index = 0, title = "第一章 青灯照影"),
            V8ValidationChapter(index = 1, title = "第二章 山门夜雨"),
            V8ValidationChapter(index = 2, title = "请假条"),
            V8ValidationChapter(index = 3, title = "第三章 剑气入云"),
            V8ValidationChapter(index = 4, title = "第四章 石桥听雷"),
            V8ValidationChapter(index = 5, title = "第五章 古卷生尘"),
            V8ValidationChapter(index = 6, title = "第六章 明月照庭"),
            V8ValidationChapter(index = 7, title = "第七章 云台问道"),
            V8ValidationChapter(index = 8, title = "第八章 松风过峡"),
            V8ValidationChapter(index = 9, title = "第九章 寒泉映剑"),
            V8ValidationChapter(index = 10, title = "第十章 夜渡青溪")
        )

        val plan = planner.selectChapters(chapters) { position, _ ->
            storyContent(position)
        }

        assertTrue(2 !in plan.targetIndexes)
        assertTrue(2 !in plan.contextIndexes)
    }

    private fun storyContent(seed: Int): String {
        return buildString {
            repeat(12) { round ->
                append("沈青提着青铜灯穿过雨后的山门，石阶旁的灵泉映出第")
                append(seed + round)
                append("道剑痕。")
                append("白长老把旧卷摊在案上，提醒众弟子今夜云台法阵不可有失。")
                append("远处钟声渐沉，竹林深处的飞剑传来细微震鸣。")
            }
        }
    }
}
