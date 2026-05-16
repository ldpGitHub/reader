package com.ldp.reader.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ldp.reader.R
import com.ldp.reader.databinding.ViewRefreshTipBinding

class ScrollRefreshLayout @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(mContext, attrs) {
    private lateinit var mTvTip: TextView
    private lateinit var mEmptyView: View
    private var mContentView: View? = null

    private lateinit var mTipOpenAnim: Animation
    private lateinit var mTopCloseAnim: Animation

    @LayoutRes
    private var mEmptyId = 0
    private var mTipStr: String? = ""

    init {
        initAttrs(attrs)
        initView()
    }

    fun toggleTip() {
        initAnim()
        cancelAnim()
        if (mTvTip.visibility == GONE) {
            mTvTip.visibility = VISIBLE
            mTvTip.startAnimation(mTipOpenAnim)
        } else {
            mTvTip.startAnimation(mTopCloseAnim)
            mTvTip.visibility = GONE
        }
    }

    private fun initAnim() {
        if (!::mTipOpenAnim.isInitialized || !::mTopCloseAnim.isInitialized) {
            mTipOpenAnim = AnimationUtils.loadAnimation(mContext, R.anim.slide_top_in)
            mTopCloseAnim = AnimationUtils.loadAnimation(mContext, R.anim.slide_top_out)

            mTipOpenAnim.fillAfter = true
            mTopCloseAnim.fillAfter = true
        }
    }

    fun showNetTip() {
        toggleTip()
        val runnable = Runnable {
            mTvTip.startAnimation(mTopCloseAnim)
            mTvTip.visibility = GONE
        }
        mTvTip.removeCallbacks(runnable)
        if (mTvTip.visibility == VISIBLE) {
            mTvTip.postDelayed(runnable, 2000)
        }
    }

    private fun cancelAnim() {
        if (mTipOpenAnim.hasStarted()) {
            mTipOpenAnim.cancel()
        }
        if (mTopCloseAnim.hasStarted()) {
            mTipOpenAnim.cancel()
        }
    }

    protected fun showEmptyView() {
        mEmptyView.visibility = VISIBLE
        if (mContentView != null) {
            mContentView!!.visibility = GONE
        }
    }

    protected fun showContent() {
        mEmptyView.visibility = GONE
        if (mContentView != null) {
            mContentView!!.visibility = VISIBLE
        }
    }

    private fun initAttrs(attrs: AttributeSet?) {
        val array = mContext.obtainStyledAttributes(attrs, R.styleable.ScrollRefreshLayout)
        mEmptyId = array.getResourceId(R.styleable.ScrollRefreshLayout_layout_scroll_empty, R.layout.view_empty)
        mTipStr = array.getString(R.styleable.ScrollRefreshLayout_text_error_tip)
        array.recycle()
    }

    private fun initView() {
        mEmptyView = inflateId(this, mEmptyId)
        val tipBinding = ViewRefreshTipBinding.inflate(LayoutInflater.from(mContext), this, false)
        val tipView = tipBinding.root

        addView(mEmptyView)
        addView(tipView)

        mTvTip = tipBinding.scrollRefreshTvTip
        mTvTip.text = mTipStr
        mEmptyView.visibility = GONE
    }

    private fun inflateId(parent: ViewGroup, @LayoutRes id: Int): View {
        return LayoutInflater.from(mContext).inflate(id, parent, false)
    }

    fun setTip(str: String?) {
        mTvTip.text = str
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        if (childCount == CHILD_COUNT) {
            mContentView = child
        }
    }

    override fun addView(child: View?) {
        if (childCount > CHILD_COUNT) {
            throw IllegalStateException("RefreshLayout can host only one direct child")
        }
        super.addView(child)
    }

    override fun addView(child: View?, index: Int) {
        if (childCount > CHILD_COUNT) {
            throw IllegalStateException("RefreshLayout can host only one direct child")
        }
        super.addView(child, index)
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        if (childCount > CHILD_COUNT) {
            throw IllegalStateException("RefreshLayout can host only one direct child")
        }
        super.addView(child, params)
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (childCount > CHILD_COUNT) {
            throw IllegalStateException("RefreshLayout can host only one direct child")
        }
        super.addView(child, index, params)
    }

    companion object {
        private const val CHILD_COUNT = 3
    }
}
