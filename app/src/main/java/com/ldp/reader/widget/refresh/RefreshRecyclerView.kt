package com.ldp.reader.widget.refresh

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class RefreshRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RefreshLayout(context, attrs, defStyleAttr) {
    private lateinit var mRecyclerView: RecyclerView
    private var isFirstLoad = true

    override fun createContentView(parent: ViewGroup): View {
        mRecyclerView = RecyclerView(context)
        return mRecyclerView
    }

    fun setLayoutManager(manager: RecyclerView.LayoutManager?) {
        mRecyclerView.layoutManager = manager
    }

    fun addItemDecoration(decoration: RecyclerView.ItemDecoration) {
        mRecyclerView.addItemDecoration(decoration)
    }

    fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        mRecyclerView.adapter = adapter
        adapter!!.registerAdapterDataObserver(MyAdapterDataObserver())
    }

    fun getReyclerView(): RecyclerView {
        return mRecyclerView
    }

    internal inner class MyAdapterDataObserver : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            update()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            update()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            update()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            update()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            update()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            update()
        }

        private fun update() {
            val count = mRecyclerView.adapter!!.itemCount
            if (isFirstLoad) {
                if (count == 0) {
                    showEmpty()
                } else {
                    showFinish()
                }
                isFirstLoad = false
            }
        }
    }
}
