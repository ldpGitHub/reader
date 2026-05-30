package com.ldp.reader.source

import com.ldp.reader.sourceengine.content.v8.V8ChapterQualityType
import com.ldp.reader.sourceengine.content.v8.V8ChapterMarkResult
import com.ldp.reader.sourceengine.content.v8.V8ChapterMarkState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceEngineV8SecondaryProbePolicyTest {
    @Test
    fun inconclusiveTailMarksTriggerSecondaryProbeButNormalDoesNot() {
        assertTrue(SourceEngineV8SecondaryProbePolicy.shouldProbeTailSecondary(mark(V8ChapterMarkState.INCONCLUSIVE)))
        assertTrue(SourceEngineV8SecondaryProbePolicy.shouldProbeTailSecondary(mark(V8ChapterMarkState.WRONG)))
        assertTrue(SourceEngineV8SecondaryProbePolicy.shouldProbeTailSecondary(mark(V8ChapterMarkState.NON_STORY)))
        assertTrue(SourceEngineV8SecondaryProbePolicy.shouldProbeTailSecondary(mark(V8ChapterMarkState.BAD_EXTRACTION)))
        assertFalse(SourceEngineV8SecondaryProbePolicy.shouldProbeTailSecondary(mark(V8ChapterMarkState.NORMAL)))
    }

    private fun mark(state: V8ChapterMarkState): V8ChapterMarkResult {
        return V8ChapterMarkResult(
            chapterIndex = 12,
            chapterTitle = "第12章",
            state = state,
            confidence = 0.8,
            qualityType = V8ChapterQualityType.CLEAN_STORY,
            suggestionState = null,
            action = null,
            reasons = emptyList()
        )
    }
}
