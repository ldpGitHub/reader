package com.ldp.reader.ui.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import com.ldp.reader.databinding.FragmentBookStoreBinding
import com.ldp.reader.ui.base.BaseFragment

class BookStoreFragment : BaseFragment<FragmentBookStoreBinding>() {
    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBookStoreBinding {
        return FragmentBookStoreBinding.inflate(inflater, container, false)
    }
}
