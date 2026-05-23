package com.ldp.reader.source

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ldp.reader.model.bean.BookChapterBean
import com.ldp.reader.sourceengine.content.v5.V5ChapterMarkResult
import com.ldp.reader.sourceengine.content.v5.V5ChapterMarkState
import com.ldp.reader.widget.page.TxtChapter
import java.util.Locale

object SourceEngineCatalogMarkRegistry {
    data class MarkUpdate(
        val sourceBookKey: String,
        val sourceLabel: String,
        val sourceUrl: String?,
        val bookName: String?,
        val author: String?,
        val marks: Map<Int, V5ChapterMarkResult>,
        val updatedAtMs: Long = System.currentTimeMillis()
    )

    private val markUpdates = MutableLiveData<MarkUpdate>()
    private val marksBySourceBook = LinkedHashMap<String, Map<Int, V5ChapterMarkResult>>()
    private val marksBySourceBookIdentity = LinkedHashMap<String, Map<Int, V5ChapterMarkResult>>()
    private val marksByBookIdentityTitle = LinkedHashMap<String, Map<String, V5ChapterMarkResult>>()

    val updates: LiveData<MarkUpdate> = markUpdates

    @Synchronized
    fun record(
        sourceBookKey: String,
        sourceLabel: String,
        marks: List<V5ChapterMarkResult>
    ) {
        record(sourceBookKey, sourceLabel, null, null, null, marks)
    }

    @Synchronized
    fun record(
        sourceBookKey: String,
        sourceLabel: String,
        sourceUrl: String?,
        bookName: String?,
        author: String?,
        marks: List<V5ChapterMarkResult>
    ) {
        val byChapterIndex = marks.associateBy { mark -> mark.chapterIndex }
        marksBySourceBook[sourceBookKey] = byChapterIndex
        sourceBookIdentityKey(sourceUrl, bookName, author)?.let { key ->
            marksBySourceBookIdentity[key] = byChapterIndex
        }
        bookIdentityKey(bookName, author)?.let { key ->
            marksByBookIdentityTitle[key] = marks.associateBy { mark -> normalizedChapterTitle(mark.chapterTitle) }
        }
        markUpdates.postValue(MarkUpdate(sourceBookKey, sourceLabel, sourceUrl, bookName, author, byChapterIndex))
    }

    @Synchronized
    fun applyTo(chapters: List<TxtChapter>): Int {
        var changed = 0
        chapters.forEach { chapter ->
            val mark = markForChapterLink(chapter.link)
            if (chapter.applyIntegrityMark(mark)) {
                changed += 1
            }
        }
        return changed
    }

    @Synchronized
    fun applyToBookChapters(chapters: List<BookChapterBean>): Int {
        var changed = 0
        chapters.forEach { chapter ->
            val mark = markForChapterLink(chapter.link)
            if (chapter.applyIntegrityMark(mark)) {
                changed += 1
            }
        }
        return changed
    }

    @Synchronized
    fun countMatching(chapters: List<TxtChapter>): Int {
        return chapters.count { chapter -> markForChapterLink(chapter.link) != null }
    }

    @Synchronized
    fun countMatchingBookChapters(chapters: List<BookChapterBean>): Int {
        return chapters.count { chapter -> markForChapterLink(chapter.link) != null }
    }

    @Synchronized
    internal fun clearForTest() {
        marksBySourceBook.clear()
        marksBySourceBookIdentity.clear()
        marksByBookIdentityTitle.clear()
    }

    fun sourceBookKey(sourceUrl: String, bookUrl: String): String {
        return sourceUrl + "\n" + bookUrl
    }

    private fun markForChapterLink(link: String?): V5ChapterMarkResult? {
        if (!SourceEngineBookRoute.isChapterId(link)) return null
        val payload = runCatching { SourceEngineBookRoute.decodeChapterId(requireNotNull(link)) }.getOrNull()
            ?: return null
        val sourceBookKey = sourceBookKey(payload.sourceUrl, payload.bookUrl)
        return marksBySourceBook[sourceBookKey]?.get(payload.index)
            ?: sourceBookIdentityKey(payload.sourceUrl, payload.bookName, payload.author)
                ?.let { key -> marksBySourceBookIdentity[key]?.get(payload.index) }
            ?: bookIdentityKey(payload.bookName, payload.author)
                ?.let { key -> marksByBookIdentityTitle[key]?.get(normalizedChapterTitle(payload.chapterName)) }
    }

    private fun sourceBookIdentityKey(sourceUrl: String?, bookName: String?, author: String?): String? {
        val source = sourceUrl?.trim().orEmpty()
        val name = normalizedIdentityPart(bookName)
        val writer = normalizedIdentityPart(author)
        if (source.isBlank() || name.isBlank()) return null
        return source + "\n" + name + "\n" + writer
    }

    private fun bookIdentityKey(bookName: String?, author: String?): String? {
        val name = normalizedIdentityPart(bookName)
        val writer = normalizedIdentityPart(author)
        if (name.isBlank()) return null
        return name + "\n" + writer
    }

    private fun normalizedIdentityPart(value: String?): String {
        return value
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace(Regex("""\s+"""), "")
            .orEmpty()
    }

    private fun normalizedChapterTitle(value: String?): String {
        return normalizedIdentityPart(value)
    }

    private fun TxtChapter.applyIntegrityMark(mark: V5ChapterMarkResult?): Boolean {
        val state = mark?.state?.name
        val confidence = mark?.confidence ?: 0.0
        val reason = mark?.reasons?.joinToString("|")
        if (sourceIntegrityState == state &&
            sourceIntegrityConfidence == confidence &&
            sourceIntegrityReason == reason
        ) {
            return false
        }
        sourceIntegrityState = state
        sourceIntegrityConfidence = confidence
        sourceIntegrityReason = reason
        return true
    }

    private fun BookChapterBean.applyIntegrityMark(mark: V5ChapterMarkResult?): Boolean {
        val state = mark?.state?.name
        val confidence = mark?.confidence ?: 0.0
        val reason = mark?.reasons?.joinToString("|")
        if (sourceIntegrityState == state &&
            sourceIntegrityConfidence == confidence &&
            sourceIntegrityReason == reason
        ) {
            return false
        }
        sourceIntegrityState = state
        sourceIntegrityConfidence = confidence
        sourceIntegrityReason = reason
        return true
    }
}

fun TxtChapter.hasHiddenSourceIntegrityMark(): Boolean {
    return sourceIntegrityState == V5ChapterMarkState.WRONG.name ||
        sourceIntegrityState == V5ChapterMarkState.NON_STORY.name ||
        sourceIntegrityState == V5ChapterMarkState.BAD_EXTRACTION.name
}
