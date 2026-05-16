package com.ldp.reader.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Created by ldp on 17-3-31.
 */
abstract class BaseFragment<VB : ViewBinding?> : Fragment() {
    public var binding: VB? = null

    protected fun initData(savedInstanceState: Bundle?) {}
    protected open fun initClick() {}
    protected open fun processLogic() {}
    protected open fun initWidget(savedInstanceState: Bundle?) {}

    /******************************lifecycle area */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = getViewBinding(inflater, container)
        return binding!!.root
    }

    protected abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData(savedInstanceState)
        initWidget(savedInstanceState)
        initClick()
        processLogic()
    }

    override fun onDetach() {
        super.onDetach()
        binding = null
    }
}
