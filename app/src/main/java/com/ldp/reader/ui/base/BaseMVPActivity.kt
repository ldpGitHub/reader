package com.ldp.reader.ui.base

import androidx.viewbinding.ViewBinding
import com.ldp.reader.ui.base.BaseContract.BasePresenter
import com.ldp.reader.ui.base.BaseContract.BaseView

/**
 * Created by ldp on 17-4-25.
 */
abstract class BaseMVPActivity<V : BaseView, T : BasePresenter<V>, VB : ViewBinding> :
    BaseActivity<VB>(), BaseView {
    protected lateinit var mPresenter: T
    protected abstract fun bindPresenter(): T
    override fun processLogic() {
        attachView(bindPresenter())
    }

    private fun attachView(presenter: T) {
        mPresenter = presenter
        mPresenter!!.attachView(this as V)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter!!.detachView()
    }
}