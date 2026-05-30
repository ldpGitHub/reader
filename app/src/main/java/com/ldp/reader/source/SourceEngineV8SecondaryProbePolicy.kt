package com.ldp.reader.source

import com.ldp.reader.sourceengine.content.v8.V8ChapterMarkResult
import com.ldp.reader.sourceengine.content.v8.V8ChapterMarkState

internal object SourceEngineV8SecondaryProbePolicy {
    fun shouldProbeTailSecondary(mark: V8ChapterMarkResult): Boolean {
        return mark.state.isBadForTail || mark.state == V8ChapterMarkState.INCONCLUSIVE
    }
}
