package com.ldp.reader.sourceengine.content.v5

class V5TailContiguousPollutionExpander(
    private val minBadMarks: Int = 3,
    private val minBadDensity: Double = 0.45,
    private val maxTailMarks: Int = 160
) {
    fun expand(marks: List<V5ChapterMarkResult>): V5TailExpansionResult {
        if (marks.size < minBadMarks) return V5TailExpansionResult(marks)
        val sorted = marks.sortedBy { mark -> mark.chapterIndex }
        val tail = sorted.takeLast(maxTailMarks)
        val candidate = tailClusterCandidate(tail)
            ?: return V5TailExpansionResult(sorted)

        val expanded = sorted.map { mark ->
            if (mark.chapterIndex >= candidate.startChapterIndex && !mark.state.isBadForTail) {
                mark.copy(
                    state = V5ChapterMarkState.WRONG,
                    confidence = mark.confidence.coerceAtLeast(PROPAGATED_CONFIDENCE),
                    suggestionState = mark.suggestionState ?: NovelStateOutputType.POLLUTED_RUN,
                    action = mark.action ?: CleanAction.MARK_ONLY,
                    reasons = (
                        mark.reasons +
                            "tail contiguous pollution propagated from dense bad marks " +
                            "bad=${candidate.badCount}/${candidate.segmentSize}"
                        )
                        .distinct()
                        .take(12)
                )
            } else {
                mark
            }
        }
        val filled = expanded
            .zip(sorted)
            .filter { (new, old) -> new.state != old.state }
            .map { (new, _) -> new.chapterIndex }
        return V5TailExpansionResult(
            marks = expanded,
            startChapterIndex = candidate.startChapterIndex,
            segmentSize = candidate.segmentSize,
            badCount = candidate.badCount,
            density = candidate.density,
            filledChapterIndexes = filled
        )
    }

    private fun tailClusterCandidate(tail: List<V5ChapterMarkResult>): TailExpansionCandidate? {
        val lastBadOffset = tail.indexOfLast { mark -> mark.state.isBadForTail }
        if (lastBadOffset < 0 || lastBadOffset < tail.lastIndex - MAX_TRAILING_NORMAL_MARKS) return null

        var startOffset = lastBadOffset
        var consecutiveNormalGap = 0
        for (offset in lastBadOffset downTo 0) {
            val mark = tail[offset]
            if (mark.state.isBadForTail) {
                startOffset = offset
                consecutiveNormalGap = 0
            } else {
                consecutiveNormalGap += 1
                if (consecutiveNormalGap > MAX_INTERNAL_NORMAL_GAP) break
            }
        }

        val segment = tail.subList(startOffset, tail.size)
        val badCount = segment.count { mark -> mark.state.isBadForTail }
        val density = badCount.toDouble() / segment.size
        return if (badCount >= minBadMarks && density >= minBadDensity) {
            TailExpansionCandidate(segment.first().chapterIndex, segment.size, badCount, density)
        } else {
            null
        }
    }

    private data class TailExpansionCandidate(
        val startChapterIndex: Int,
        val segmentSize: Int,
        val badCount: Int,
        val density: Double
    )

    private companion object {
        private const val MAX_TRAILING_NORMAL_MARKS = 2
        private const val MAX_INTERNAL_NORMAL_GAP = 4
        private const val PROPAGATED_CONFIDENCE = 0.70
    }
}

data class V5TailExpansionResult(
    val marks: List<V5ChapterMarkResult>,
    val startChapterIndex: Int? = null,
    val segmentSize: Int = 0,
    val badCount: Int = 0,
    val density: Double = 0.0,
    val filledChapterIndexes: List<Int> = emptyList()
)
