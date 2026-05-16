package com.ldp.reader.model.local;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class BookRepositoryStorageContractTest {

    @Test
    public void repositoryKeepsBookshelfChapterAndRecordStorageSemantics() throws IOException {
        String repository = readFile("src/main/java/com/ldp/reader/model/local/BookRepository.kt");

        assertInOrder(repository,
                "val collBooks: List<CollBookBean>",
                "get() = mBookStore.getCollBooks()");

        assertInOrder(repository,
                "fun getBookChapters(bookId: String?): List<BookChapterBean>",
                "return mBookStore.getBookChapters(bookId)");

        assertInOrder(repository,
                "fun deleteCollBookWithFiles(bean: CollBookBean)",
                "deleteBook(bean.get_id())",
                "deleteBookChapter(bean.get_id())",
                "mBookStore.deleteCollBook(bean)");

        assertInOrder(repository,
                "fun saveBookRecord(bean: BookRecordBean)",
                "mBookRecordStore.saveBookRecord(bean)");

        assertInOrder(repository,
                "fun getBookRecord(bookId: String?): BookRecordBean?",
                "return mBookRecordStore.getBookRecord(bookId)");

        assertInOrder(repository,
                "fun deleteBookRecord(id: String?)",
                "mBookRecordStore.deleteBookRecord(id)");

        assertInOrder(repository,
                "private fun replaceBookChaptersInTx(bookId: String?, beans: List<BookChapterBean>?)",
                "mBookStore.replaceBookChapters(bookId, beans)");
    }

    @Test
    public void persistedEntitiesKeepCurrentBusinessIdsAndRelationFields() throws IOException {
        String collBook = readFile("src/main/java/com/ldp/reader/model/bean/CollBookBean.kt");
        String chapter = readFile("src/main/java/com/ldp/reader/model/bean/BookChapterBean.kt");
        String record = readFile("src/main/java/com/ldp/reader/model/bean/BookRecordBean.kt");

        assertTrue("CollBookBean should no longer be a GreenDAO entity",
                !collBook.contains("org.greenrobot.greendao.annotation"));
        assertInOrder(collBook,
                "private var idValue",
                "private var bookChapterList");

        assertTrue("BookChapterBean should no longer be a GreenDAO entity",
                !chapter.contains("org.greenrobot.greendao.annotation"));
        assertInOrder(chapter,
                "var id",
                "var bookId",
                "var start",
                "var end");

        assertTrue("BookRecordBean should no longer be a GreenDAO entity",
                !record.contains("org.greenrobot.greendao.annotation"));
        assertInOrder(record,
                "var bookId",
                "var chapter",
                "var pagePos");
    }

    @Test
    public void greenDaoBuildAndGeneratedSourcesAreRemoved() throws IOException {
        String appGradle = readFile("build.gradle");
        String rootGradle = readFile("../build.gradle");
        String gradleProperties = readFile("../gradle.properties");

        assertTrue(!appGradle.contains("org.greenrobot.greendao"));
        assertTrue(!appGradle.contains("greendao {"));
        assertTrue(!rootGradle.contains("greendao-gradle-plugin"));
        assertTrue(!gradleProperties.contains("greendaoVersion"));
        assertTrue(!new File("src/main/java/com/ldp/reader/model/gen").exists());
        assertTrue(!new File("src/main/java/com/ldp/reader/model/local/DaoDbHelper.java").exists());
    }

    private static void assertInOrder(String source, String... parts) {
        int cursor = 0;
        for (String part : parts) {
            int index = source.indexOf(part, cursor);
            assertTrue("Missing or out of order: " + part, index >= 0);
            cursor = index + part.length();
        }
    }

    private static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
    }
}
