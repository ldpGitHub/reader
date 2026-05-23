package com.ldp.reader.ui.adapter.view

import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ldp.reader.R
import com.ldp.reader.databinding.ItemCategoryBinding
import com.ldp.reader.sourceengine.content.v5.V5ChapterMarkState
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
        val markState = value.sourceIntegrityState
        val color = when (markState) {
            V5ChapterMarkState.WRONG.name,
            V5ChapterMarkState.BAD_EXTRACTION.name -> R.color.light_red
            V5ChapterMarkState.NON_STORY.name -> R.color.nb_text_common_h3
            else -> R.color.nb_text_default
        }
        mTvChapter.setTextColor(ContextCompat.getColor(getContext(), color))
        mTvChapter.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        mTvChapter.text = integrityDisplayTitle(value)
    }

    override fun getItemLayoutId(): Int {
        return R.layout.item_category
    }

    fun setSelectedChapter() {
        mTvChapter.setTextColor(ContextCompat.getColor(getContext(), R.color.light_red))
        mTvChapter.isSelected = true
    }

    private fun integrityDisplayTitle(value: TxtChapter): String {
        val title = value.title.orEmpty()
        return when (value.sourceIntegrityState) {
            V5ChapterMarkState.WRONG.name -> "$title  错"
            V5ChapterMarkState.NON_STORY.name -> "$title  注"
            V5ChapterMarkState.BAD_EXTRACTION.name -> "$title  抽"
            else -> title
        }
    }
}
