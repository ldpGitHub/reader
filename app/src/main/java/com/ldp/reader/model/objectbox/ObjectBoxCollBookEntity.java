package com.ldp.reader.model.objectbox;

import com.ldp.reader.model.bean.CollBookBean;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

@Entity
public class ObjectBoxCollBookEntity {
    @Id
    private long objectBoxId;
    @Index
    private String bookId;
    private String title;
    private String author;
    private String shortIntro;
    private String cover;
    private String bookStatus;
    private String updated;
    private String lastRead;
    private int chaptersCount;
    private String lastChapter;
    private boolean isUpdate;
    private boolean isLocal;
    private String bookIdInBiquge;

    public ObjectBoxCollBookEntity() {
    }

    public static ObjectBoxCollBookEntity from(CollBookBean book) {
        ObjectBoxCollBookEntity entity = new ObjectBoxCollBookEntity();
        entity.bookId = book.get_id();
        entity.title = book.getTitle();
        entity.author = book.getAuthor();
        entity.shortIntro = book.getShortIntro();
        entity.cover = book.getCover();
        entity.bookStatus = book.getBookStatus();
        entity.updated = book.getUpdated();
        entity.lastRead = book.getLastRead();
        entity.chaptersCount = book.getChaptersCount();
        entity.lastChapter = book.getLastChapter();
        entity.isUpdate = book.getIsUpdate();
        entity.isLocal = book.getIsLocal();
        entity.bookIdInBiquge = book.getBookIdInBiquge();
        return entity;
    }

    public CollBookBean toCollBook() {
        CollBookBean book = new CollBookBean();
        book.set_id(bookId);
        book.setTitle(title);
        book.setAuthor(author);
        book.setShortIntro(shortIntro);
        book.setCover(cover);
        book.setBookStatus(bookStatus);
        book.setUpdated(updated);
        book.setLastRead(lastRead);
        book.setChaptersCount(chaptersCount);
        book.setLastChapter(lastChapter);
        book.setIsUpdate(isUpdate);
        book.setIsLocal(isLocal);
        book.setBookIdInBiquge(bookIdInBiquge);
        return book;
    }

    public long getObjectBoxId() {
        return objectBoxId;
    }

    public void setObjectBoxId(long objectBoxId) {
        this.objectBoxId = objectBoxId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getLastRead() {
        return lastRead;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getShortIntro() {
        return shortIntro;
    }

    public String getCover() {
        return cover;
    }

    public String getBookStatus() {
        return bookStatus;
    }

    public String getUpdated() {
        return updated;
    }

    public int getChaptersCount() {
        return chaptersCount;
    }

    public String getLastChapter() {
        return lastChapter;
    }

    public boolean getIsUpdate() {
        return isUpdate;
    }

    public boolean getIsLocal() {
        return isLocal;
    }

    public String getBookIdInBiquge() {
        return bookIdInBiquge;
    }
}
