package com.ldp.reader.ui.adapter;

import com.ldp.reader.model.bean.CollBookBean;
import com.ldp.reader.ui.adapter.view.CollBookHolder;
import com.ldp.reader.ui.base.adapter.IViewHolder;
import com.ldp.reader.widget.adapter.WholeAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ldp on 17-5-8.
 */

public class CollBookAdapter extends WholeAdapter<CollBookBean> {
    private boolean editMode = false;
    private final Set<String> selectedBookKeys = new HashSet<>();

    @Override
    protected IViewHolder<CollBookBean> createViewHolder(int viewType) {
        return new CollBookHolder(this);
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        if (!editMode) {
            selectedBookKeys.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void toggleSelection(CollBookBean book) {
        String key = selectionKey(book);
        if (selectedBookKeys.contains(key)) {
            selectedBookKeys.remove(key);
        } else {
            selectedBookKeys.add(key);
        }
        notifyDataSetChanged();
    }

    public boolean isSelected(CollBookBean book) {
        return selectedBookKeys.contains(selectionKey(book));
    }

    public void selectAllVisible() {
        for (CollBookBean book : mList) {
            selectedBookKeys.add(selectionKey(book));
        }
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedBookKeys.clear();
        notifyDataSetChanged();
    }

    public boolean isAllVisibleSelected() {
        if (mList.isEmpty()) {
            return false;
        }
        for (CollBookBean book : mList) {
            if (!selectedBookKeys.contains(selectionKey(book))) {
                return false;
            }
        }
        return true;
    }

    public int getSelectedCount() {
        return getSelectedBooks().size();
    }

    public List<CollBookBean> getSelectedBooks() {
        List<CollBookBean> selectedBooks = new ArrayList<>();
        for (CollBookBean book : mList) {
            if (selectedBookKeys.contains(selectionKey(book))) {
                selectedBooks.add(book);
            }
        }
        return selectedBooks;
    }

    @Override
    public void removeItem(CollBookBean value) {
        selectedBookKeys.remove(selectionKey(value));
        super.removeItem(value);
    }

    @Override
    public void removeItems(List<CollBookBean> value) {
        for (CollBookBean book : value) {
            selectedBookKeys.remove(selectionKey(book));
        }
        super.removeItems(value);
    }

    static String selectionKey(CollBookBean book) {
        if (book == null) {
            return "";
        }
        String id = clean(book.get_id());
        if (book.isLocal()) {
            return "local:" + clean(book.getCover()) + ":" + id;
        }
        return "online:" + id;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

}
