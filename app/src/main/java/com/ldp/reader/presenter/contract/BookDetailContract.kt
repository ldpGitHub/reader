package com.ldp.reader.presenter.contract

import com.ldp.reader.model.bean.BookDetailBeanInOwn
import com.ldp.reader.model.bean.CollBookBean
import com.ldp.reader.ui.base.BaseContract

/**
 * Created by ldp on 17-5-4.
 */
@JvmSuppressWildcards
interface BookDetailContract {
    interface View : BaseContract.BaseView {
        fun finishRefresh(bean: BookDetailBeanInOwn)

        fun waitToBookShelf()

        fun errorToBookShelf()

        fun succeedToBookShelf()
    }

    interface Presenter<T : BaseContract.BaseView> : BaseContract.BasePresenter<T> {
        fun refreshBookDetail(bookId: String?)

        fun addToBookShelf(collBook: CollBookBean?)
    }
}
