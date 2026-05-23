package com.ldp.reader.algorithmtest.core

enum class FeatureType {
    CHARACTER,
    ORGANIZATION,
    LOCATION,
    SKILL,
    ITEM,
    CURRENCY,
    REALM,
    WORLD_TERM,
    PHRASE,
    RELATION_EDGE
}

data class AlgorithmConfig(
    val chunkSize: Int = 800,
    val chunkOverlap: Int = 150,
    val seedChapterRatio: Double = 0.70,
    val minFeatureFrequency: Int = 3,
    val minFeatureChapterCount: Int = 2,
    val minAlienFeatureFrequency: Int = 2,
    val coreFeatureLimit: Int = 500,
    val supportFeatureLimit: Int = 1_000,
    val refineRounds: Int = 2,
    val cleanChunkThreshold: Double = 0.58,
    val abnormalThreshold: Double = 0.40,
    val suspiciousThreshold: Double = 0.55,
    val minReportedConfidence: Double = 0.70,
    val minAbnormalRunLength: Int = 2,
    val minSuffixChunks: Int = 1,
    val normalBeforeThreshold: Double = 0.58,
    val abnormalAfterThreshold: Double = 0.45,
    val judgmentStartRatio: Double = 1.0 / 3.0,
    val relationWeight: Double = 1.4,
    val alienWeight: Double = 1.7,
    val alienRelationWeight: Double = 1.1,
    val smooth: Double = 24.0
)

data class ChapterInput(
    val index: Int,
    val title: String,
    val content: String
)

data class TextChunk(
    val chapterIndex: Int,
    val chapterTitle: String,
    val chunkIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val chapterLength: Int,
    val text: String
)

data class FingerprintFeature(
    val text: String,
    val type: FeatureType,
    val chapterHitCount: Int,
    val totalHitCount: Int,
    val weight: Double
)

data class NovelFingerprint(
    val title: String,
    val author: String,
    val coreFeatures: List<FingerprintFeature>,
    val supportFeatures: List<FingerprintFeature>,
    val relationEdges: Map<String, Double>
) {
    val featureWeights: Map<String, Double> =
        (coreFeatures + supportFeatures).associate { feature -> feature.text to feature.weight }

    val featureTypes: Map<String, FeatureType> =
        (coreFeatures + supportFeatures).associate { feature -> feature.text to feature.type }
}

data class ChunkScore(
    val chunk: TextChunk,
    val knownScore: Double,
    val alienScore: Double,
    val relationScore: Double,
    val alienRelationScore: Double,
    val containmentScore: Double,
    val belongScore: Double,
    val knownFeatures: List<String>,
    val alienFeatures: List<String>,
    val reasons: List<String>
)

enum class PollutionType {
    LOCAL_ABNORMAL,
    SUFFIX_POLLUTION
}

enum class NovelStateOutputType {
    NORMAL,
    NON_STORY,
    POLLUTED_SUFFIX,
    POLLUTED_RUN,
    UNCERTAIN
}

enum class CleanAction {
    KEEP,
    MARK_ONLY,
    SUGGEST_DELETE,
    AUTO_DELETE_ALLOWED
}

data class CleanSuggestion(
    val chapterIndex: Int,
    val chapterTitle: String,
    val pollutionType: PollutionType,
    val startOffset: Int,
    val endOffset: Int,
    val confidence: Double,
    val action: CleanAction,
    val reasons: List<String>,
    val stateType: NovelStateOutputType = NovelStateOutputType.UNCERTAIN
)

data class CleanReport(
    val title: String,
    val author: String,
    val chapterCount: Int,
    val chunkCount: Int,
    val fingerprint: NovelFingerprint,
    val chunkScores: List<ChunkScore>,
    val suggestions: List<CleanSuggestion>,
    val logs: List<String>
) {
    fun humanSummary(maxFeatures: Int = 12): String {
        val builder = StringBuilder()
        builder.appendLine("Novel: $title / $author")
        builder.appendLine("Chapters: $chapterCount, chunks: $chunkCount")
        builder.appendLine("Core features: ${fingerprint.coreFeatures.size}, support: ${fingerprint.supportFeatures.size}")
        builder.appendLine("Top features:")
        fingerprint.coreFeatures.take(maxFeatures).forEach { feature ->
            builder.appendLine(
                "- ${feature.type}:${feature.text} weight=${"%.1f".format(feature.weight)} " +
                    "chapters=${feature.chapterHitCount} hits=${feature.totalHitCount}"
            )
        }
        builder.appendLine("Suggestions: ${suggestions.size}")
        suggestions.forEach { suggestion ->
            builder.appendLine(
                "- chapter ${suggestion.chapterIndex + 1} ${suggestion.chapterTitle}: " +
                    "${suggestion.stateType}/${suggestion.pollutionType} start=${suggestion.startOffset} " +
                    "confidence=${"%.2f".format(suggestion.confidence)} action=${suggestion.action}"
            )
            suggestion.reasons.take(4).forEach { reason -> builder.appendLine("  * $reason") }
        }
        return builder.toString()
    }
}
