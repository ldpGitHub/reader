package com.ldp.reader.ui.adapter.view

import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.ldp.reader.R
import com.ldp.reader.databinding.ItemSearchBookBinding
import com.ldp.reader.model.bean.BookSearchResult
import com.ldp.reader.ui.base.adapter.ViewHolderImpl

/**
 * Created by ldp on 17-6-2.
 */
class SearchBookHolder : ViewHolderImpl<BookSearchResult>() {
    private lateinit var mIvCover: ImageView
    private lateinit var mTvName: TextView
    private lateinit var mTvBrief: TextView

    override fun initView() {
        val binding = ItemSearchBookBinding.bind(getItemView())
        mIvCover = binding.searchBookIvCover
        mTvName = binding.searchBookTvName
        mTvBrief = binding.searchBookTvBrief
    }

    override fun onBind(book: BookSearchResult, pos: Int) {
        Glide.with(getContext())
            .load(book.cover)
            .placeholder(R.drawable.ic_book_loading)
            .error(R.drawable.ic_load_error)
            .into(mIvCover)
        mTvName.text = book.title
        mTvBrief.text = getContext().getString(
            R.string.nb_search_book_brief,
            book.author,
            "",
            book.desc
        )
    }

    override fun getItemLayoutId(): Int {
        return R.layout.item_search_book
    }
}
