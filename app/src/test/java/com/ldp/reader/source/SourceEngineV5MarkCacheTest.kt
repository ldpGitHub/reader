package com.ldp.reader.source

import com.ldp.reader.sourceengine.content.v5.V5ChapterMarkResult
import com.ldp.reader.sourceengine.content.v5.V5ChapterMarkState
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SourceEngineV5MarkCacheTest {
    @Test
    fun persistsMarksForMatchingCatalogIdentity() {
        val root = Files.createTempDirectory("v5-mark-cache").toFile()
        try {
            val cache = SourceEngineV5MarkCache { root }
            val identity = identity(catalogSize = 100, lastTitle = "Chapter 100")

            assertEquals(true, cache.save(identity, "source@example", listOf(mark(99, V5ChapterMarkState.WRONG))))

            val cached = cache.load(identity)
            assertNotNull(cached)
            assertEquals("source@example", cached!!.sourceLabel)
            assertEquals(V5ChapterMarkState.WRONG, cached.marks.single().state)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun ignoresCacheWhenCatalogShapeChanges() {
        val root = Files.createTempDirectory("v5-mark-cache").toFile()
        try {
            val cache = SourceEngineV5MarkCache { root }
            val oldIdentity = identity(catalogSize = 100, lastTitle = "Chapter 100")
            val newIdentity = identity(catalogSize = 101, lastTitle = "Chapter 101")
            assertEquals(true, cache.save(oldIdentity, "source@example", listOf(mark(99, V5ChapterMarkState.WRONG))))

            assertNull(cache.load(newIdentity))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun ignoresCacheWhenTailCatalogDigestChanges() {
        val root = Files.createTempDirectory("v5-mark-cache").toFile()
        try {
            val cache = SourceEngineV5MarkCache { root }
            val oldIdentity = identity(catalogSize = 100, lastTitle = "Chapter 100", tailTitleDigest = "old-tail")
            val newIdentity = identity(catalogSize = 100, lastTitle = "Chapter 100", tailTitleDigest = "new-tail")
            assertEquals(true, cache.save(oldIdentity, "source@example", listOf(mark(99, V5ChapterMarkState.WRONG))))

            assertNull(cache.load(newIdentity))
        } finally {
            root.deleteRecursively()
        }
    }

    private fun identity(
        catalogSize: Int,
        lastTitle: String,
        tailTitleDigest: String = "tail-$catalogSize-$lastTitle"
    ): SourceEngineV5MarkCache.Identity {
        return SourceEngineV5MarkCache.Identity(
            sourceBookKey = "https://source.example\nhttps://source.example/book/1",
            sourceUrl = "https://source.example",
            bookUrl = "https://source.example/book/1",
            bookName = "Target Book",
            author = "Target Author",
            catalogSize = catalogSize,
            firstTitle = "Chapter 1",
            lastTitle = lastTitle,
            tailTitleDigest = tailTitleDigest
        )
    }

    private fun mark(index: Int, state: V5ChapterMarkState): V5ChapterMarkResult {
        return V5ChapterMarkResult(
            chapterIndex = index,
            chapterTitle = "Chapter $index",
            state = state,
            confidence = 0.9,
            qualityType = null,
            suggestionState = null,
            action = null,
            reasons = listOf("test")
        )
    }
}
