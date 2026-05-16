package com.ldp.reader.model.objectbox;

import android.util.Log;

import com.ldp.reader.model.bean.BookChapterBean;
import com.ldp.reader.model.bean.CollBookBean;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.Query;

public class ObjectBoxBookStore {
    private static final String TAG = "ObjectBoxBookStore";

    private final BoxStore boxStore;
    private final Box<ObjectBoxCollBookEntity> collBookBox;
    private final Box<ObjectBoxBookChapterEntity> chapterBox;

    public ObjectBoxBookStore(BoxStore boxStore) {
        this.boxStore = boxStore;
        collBookBox = boxStore.boxFor(ObjectBoxCollBookEntity.class);
        chapterBox = boxStore.boxFor(ObjectBoxBookChapterEntity.class);
    }

    public void runInTxAsync(Runnable runnable) {
        boxStore.runInTxAsync(runnable, (result, error) -> {
            if (error != null) {
                Log.e(TAG, "ObjectBox transaction failed", error);
            }
        });
    }

    public void saveCollBook(CollBookBean book) {
        ObjectBoxCollBookEntity entity = ObjectBoxCollBookEntity.from(book);
        ObjectBoxCollBookEntity existing = findCollBookEntity(book.get_id());
        if (existing != null) {
            entity.setObjectBoxId(existing.getObjectBoxId());
        }
        collBookBox.put(entity);
    }

    public void saveCollBooks(List<CollBookBean> books) {
        for (CollBookBean book : books) {
            saveCollBook(book);
        }
    }

    public CollBookBean getCollBook(String bookId) {
        ObjectBoxCollBookEntity entity = findCollBookEntity(bookId);
        if (entity == null) {
            return null;
        }
        CollBookBean book = entity.toCollBook();
        book.setBookChapters(getBookChapters(bookId));
        return book;
    }

    public List<CollBookBean> getCollBooks() {
        Query<ObjectBoxCollBookEntity> query = collBookBox
                .query()
                .orderDesc(ObjectBoxCollBookEntity_.lastRead)
                .build();
        try {
            List<ObjectBoxCollBookEntity> entities = query.find();
            List<CollBookBean> books = new ArrayList<>(entities.size());
            for (ObjectBoxCollBookEntity entity : entities) {
                CollBookBean book = entity.toCollBook();
                book.setBookChapters(getBookChapters(book.get_id()));
                books.add(book);
            }
            return books;
        } finally {
            query.close();
        }
    }

    public List<BookChapterBean> getBookChapters(String bookId) {
        Query<ObjectBoxBookChapterEntity> query = chapterBox
                .query(ObjectBoxBookChapterEntity_.bookId.equal(bookId))
                .order(ObjectBoxBookChapterEntity_.start)
                .build();
        try {
            List<ObjectBoxBookChapterEntity> entities = query.find();
            List<BookChapterBean> chapters = new ArrayList<>(entities.size());
            for (ObjectBoxBookChapterEntity entity : entities) {
                chapters.add(entity.toBookChapter());
            }
            return chapters;
        } finally {
            query.close();
        }
    }

    public void replaceBookChapters(String bookId, List<BookChapterBean> chapters) {
        deleteBookChapters(bookId);
        List<ObjectBoxBookChapterEntity> entities = new ArrayList<>(chapters.size());
        for (BookChapterBean chapter : chapters) {
            chapter.setBookId(bookId);
            entities.add(ObjectBoxBookChapterEntity.from(chapter));
        }
        chapterBox.put(entities);
    }

    public void deleteBookChapters(String bookId) {
        Query<ObjectBoxBookChapterEntity> query = chapterBox
                .query(ObjectBoxBookChapterEntity_.bookId.equal(bookId))
                .build();
        try {
            query.remove();
        } finally {
            query.close();
        }
    }

    public void deleteCollBook(CollBookBean book) {
        ObjectBoxCollBookEntity entity = findCollBookEntity(book.get_id());
        if (entity != null) {
            collBookBox.remove(entity);
        }
    }

    private ObjectBoxCollBookEntity findCollBookEntity(String bookId) {
        Query<ObjectBoxCollBookEntity> query = collBookBox
                .query(ObjectBoxCollBookEntity_.bookId.equal(bookId))
                .build();
        try {
            return query.findFirst();
        } finally {
            query.close();
        }
    }
}
