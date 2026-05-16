package com.ldp.reader.model.objectbox;

import com.ldp.reader.model.bean.BookChapterBean;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

@Entity
public class ObjectBoxBookChapterEntity {
    @Id
    private long objectBoxId;
    @Index
    private String chapterBusinessId;
    @Index
    private String bookId;
    private String link;
    private String title;
    private String taskName;
    private boolean unreadble;
    private boolean validInZhuishu;
    private long start;
    private long end;

    public ObjectBoxBookChapterEntity() {
    }

    public static ObjectBoxBookChapterEntity from(BookChapterBean chapter) {
        ObjectBoxBookChapterEntity entity = new ObjectBoxBookChapterEntity();
        entity.chapterBusinessId = chapter.getId();
        entity.bookId = chapter.getBookId();
        entity.link = chapter.getLink();
        entity.title = chapter.getTitle();
        entity.taskName = chapter.getTaskName();
        entity.unreadble = chapter.getUnreadble();
        entity.validInZhuishu = chapter.getValidInZhuishu();
        entity.start = chapter.getStart();
        entity.end = chapter.getEnd();
        return entity;
    }

    public BookChapterBean toBookChapter() {
        return new BookChapterBean(
                chapterBusinessId,
                link,
                title,
                taskName,
                unreadble,
                validInZhuishu,
                bookId,
                start,
                end
        );
    }

    public long getObjectBoxId() {
        return objectBoxId;
    }

    public void setObjectBoxId(long objectBoxId) {
        this.objectBoxId = objectBoxId;
    }

    public String getChapterBusinessId() {
        return chapterBusinessId;
    }

    public void setChapterBusinessId(String chapterBusinessId) {
        this.chapterBusinessId = chapterBusinessId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getLink() {
        return link;
    }

    public String getTitle() {
        return title;
    }

    public String getTaskName() {
        return taskName;
    }

    public boolean getUnreadble() {
        return unreadble;
    }

    public boolean getValidInZhuishu() {
        return validInZhuishu;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }
}
