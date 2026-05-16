package com.ldp.reader.presenter.contract

import com.ldp.reader.model.bean.BookChapterBean
import com.ldp.reader.ui.base.BaseContract
import com.ldp.reader.widget.page.TxtChapter

/**
 * Created by ldp on 17-5-16.
 */
@JvmSuppressWildcards
interface ReadContract : BaseContract {
    interface View : BaseContract.BaseView {
        fun showCategory(
            bookChapterList: List<BookChapterBean>,
            bookId: String,
            isBiqugeLoaded: Boolean
        )

        fun finishChapter(isRefresh: Boolean)

        fun errorChapter()
    }

    interface Presenter<T : BaseContract.BaseView> : BaseContract.BasePresenter<T> {
        fun loadCategory(bookId: String?)

        fun loadChapter(bookId: String?, bookChapterList: List<TxtChapter>)

        fun refreshChapter(bookId: String?, bookChapter: TxtChapter?, sourceIndex: Int)
    }
}
