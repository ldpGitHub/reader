package com.ldp.reader.ui.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import com.ldp.reader.model.bean.CollBookBean;

import org.junit.Test;

public class CollBookAdapterTest {

    @Test
    public void selectionKeyDistinguishesLocalAndOnlineBooksWithSameRawId() {
        CollBookBean localBook = book("same-id", "本地书", "/sdcard/books/local.txt", true);
        CollBookBean onlineBook = book("same-id", "在线书", "https://example.com/cover.jpg", false);

        assertNotEquals(CollBookAdapter.selectionKey(localBook), CollBookAdapter.selectionKey(onlineBook));
    }

    @Test
    public void selectionKeySurvivesRefreshForSameLocalFile() {
        CollBookBean beforeRefresh = book("local-id", "旧对象", "/sdcard/books/local.txt", true);
        CollBookBean afterRefresh = book("local-id", "新对象", "/sdcard/books/local.txt", true);

        assertEquals(CollBookAdapter.selectionKey(beforeRefresh), CollBookAdapter.selectionKey(afterRefresh));
        assertFalse(CollBookAdapter.selectionKey(afterRefresh).isEmpty());
    }

    private static CollBookBean book(String id, String title, String cover, boolean local) {
        CollBookBean book = new CollBookBean();
        book.set_id(id);
        book.setTitle(title);
        book.setCover(cover);
        book.setLocal(local);
        return book;
    }
}
