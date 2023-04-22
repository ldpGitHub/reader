package com.ldp.reader.ui.base

import androidx.viewbinding.ViewBinding
import com.ldp.reader.ui.base.BaseContract.BasePresenter
import com.ldp.reader.ui.base.BaseContract.BaseView

/**
 * Created by ldp on 17-4-25.
 */
abstract class BaseMVPFragment<V : BaseView, T : BasePresenter<V>, VB : ViewBinding> : BaseFragment<VB>(),
    BaseView {
    protected var mPresenter: T? = null
    protected abstract fun bindPresenter(): T
    override fun processLogic() {
        mPresenter = bindPresenter()
        mPresenter!!.attachView(this as V)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter!!.detachView()
    }
}