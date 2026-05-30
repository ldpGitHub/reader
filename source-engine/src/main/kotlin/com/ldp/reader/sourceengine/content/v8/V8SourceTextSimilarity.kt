package com.ldp.reader.sourceengine.content.v8

object V8SourceTextSimilarity {
    fun allClearlyDissimilar(pairs: List<Pair<String, String>>): V8SimilarityDecision {
        val scores = pairs
            .map { (left, right) -> score(left, right) }
            .filter { value -> value < 1.0 }
        if (scores.size < MIN_PAIR_COUNT) {
            return V8SimilarityDecision(scores.size, 1.0, 1.0, false)
        }
        val max = scores.maxOrNull() ?: 1.0
        val average = scores.average()
        return V8SimilarityDecision(
            sampleCount = scores.size,
            maxScore = max,
            averageScore = average,
            clearlyDissimilar = max <= MAX_SAMPLE_SIMILARITY && average <= MAX_AVERAGE_SIMILARITY
        )
    }

    private fun score(left: String, right: String): Double {
        val leftNorm = normalize(left)
        val rightNorm = normalize(right)
        if (leftNorm.length < MIN_COMPARE_CHARS || rightNorm.length < MIN_COMPARE_CHARS) return 1.0
        val leftShingles = shingles(leftNorm)
        val rightShingles = shingles(rightNorm)
        if (leftShingles.isEmpty() || rightShingles.isEmpty()) return 1.0
        val smaller = if (leftShingles.size <= rightShingles.size) leftShingles else rightShingles
        val larger = if (leftShingles.size <= rightShingles.size) rightShingles else leftShingles
        val intersection = smaller.count { token -> token in larger }
        val union = leftShingles.size + rightShingles.size - intersection
        return if (union <= 0) 1.0 else intersection.toDouble() / union
    }

    private fun normalize(value: String): String {
        val compact = buildString(value.length) {
            value.lowercase().forEach { char ->
                when {
                    char in '\u4e00'..'\u9fff' -> append(char)
                    char in 'a'..'z' -> append(char)
                    char in '0'..'9' -> append(char)
                }
            }
        }
        if (compact.length <= MAX_COMPARE_CHARS) return compact
        val segment = MAX_COMPARE_CHARS / 3
        val middleStart = (compact.length / 2 - segment / 2).coerceAtLeast(0)
        return compact.take(segment) +
            compact.substring(middleStart, (middleStart + segment).coerceAtMost(compact.length)) +
            compact.takeLast(segment)
    }

    private fun shingles(value: String): Set<String> {
        if (value.length < SHINGLE_SIZE) return emptySet()
        val result = LinkedHashSet<String>()
        var index = 0
        while (index + SHINGLE_SIZE <= value.length) {
            result.add(value.substring(index, index + SHINGLE_SIZE))
            index += SHINGLE_STRIDE
        }
        return result
    }

    private const val SHINGLE_SIZE = 8
    private const val SHINGLE_STRIDE = 2
    private const val MIN_COMPARE_CHARS = 260
    private const val MAX_COMPARE_CHARS = 6_000
    private const val MIN_PAIR_COUNT = 2
    private const val MAX_SAMPLE_SIMILARITY = 0.05
    private const val MAX_AVERAGE_SIMILARITY = 0.035
}

data class V8SimilarityDecision(
    val sampleCount: Int,
    val maxScore: Double,
    val averageScore: Double,
    val clearlyDissimilar: Boolean
)
