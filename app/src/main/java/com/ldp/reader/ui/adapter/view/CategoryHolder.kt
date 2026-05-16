package com.ldp.reader.ui.adapter.view

import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ldp.reader.R
import com.ldp.reader.databinding.ItemCategoryBinding
import com.ldp.reader.ui.base.adapter.ViewHolderImpl
import com.ldp.reader.utils.BookManager
import com.ldp.reader.widget.page.TxtChapter

/**
 * Created by ldp on 17-5-16.
 */
class CategoryHolder : ViewHolderImpl<TxtChapter>() {
    private lateinit var mTvChapter: TextView

    override fun initView() {
        val binding = ItemCategoryBinding.bind(getItemView())
        mTvChapter = binding.categoryTvChapter
    }

    override fun onBind(value: TxtChapter, pos: Int) {
        val drawable: Drawable? = if (value.link == null) {
            ContextCompat.getDrawable(getContext(), R.drawable.selector_category_load)
        } else if (value.bookId != null && BookManager.isChapterCached(value.bookId, value.title)) {
            ContextCompat.getDrawable(getContext(), R.drawable.selector_category_load)
        } else {
            ContextCompat.getDrawable(getContext(), R.drawable.selector_category_unload)
        }

        mTvChapter.isSelected = false
        mTvChapter.setTextColor(ContextCompat.getColor(getContext(), R.color.nb_text_default))
        mTvChapter.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        mTvChapter.text = value.title
    }

    override fun getItemLayoutId(): Int {
        return R.layout.item_category
    }

    fun setSelectedChapter() {
        mTvChapter.setTextColor(ContextCompat.getColor(getContext(), R.color.light_red))
        mTvChapter.isSelected = true
    }
}
