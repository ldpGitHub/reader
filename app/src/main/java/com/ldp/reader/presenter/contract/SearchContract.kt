package com.ldp.reader.presenter.contract

import com.ldp.reader.model.bean.BookSearchResult
import com.ldp.reader.ui.base.BaseContract

/**
 * Created by ldp on 17-6-2.
 */
@JvmSuppressWildcards
interface SearchContract : BaseContract {
    interface View : BaseContract.BaseView {
        fun finishHotWords(hotWords: List<String>)

        fun finishKeyWords(keyWords: List<String>)

        fun finishBooks(dataBeans: List<BookSearchResult>)

        fun errorBooks()
    }

    interface Presenter<T : BaseContract.BaseView> : BaseContract.BasePresenter<T> {
        fun searchHotWord()

        fun searchKeyWord(query: String?)

        fun searchBook(query: String?)
    }
}
