package com.ldp.reader.presenter.contract

import com.ldp.reader.model.bean.CollBookBean
import com.ldp.reader.ui.base.BaseContract

/**
 * Created by ldp on 17-5-8.
 */
@JvmSuppressWildcards
interface BookShelfContract {
    interface View : BaseContract.BaseView {
        fun finishRefresh(collBookBeans: List<CollBookBean>)

        fun finishUpdate()

        fun finishSyncBook()

        fun showErrorTip(error: String?)
    }

    interface Presenter<T : BaseContract.BaseView> : BaseContract.BasePresenter<T> {
        fun refreshCollBooks()

        fun updateCollBooks(collBookBeans: List<CollBookBean>?)

        fun getBookShelf(token: String?)

        fun getBookShelfByMobile(mobile: String?, token: String?)

        fun getBookInfo(bookId: List<String>?)

        fun setBookShelf(bookIds: List<String>?)

        fun setBookShelfByMobile(bookIds: List<String>?, mobile: String?, mobileToken: String?)
    }
}
