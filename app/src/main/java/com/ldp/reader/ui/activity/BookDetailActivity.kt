package com.ldp.reader.ui.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.ldp.reader.R
import com.ldp.reader.databinding.ActivityBookDetailBinding
import com.ldp.reader.model.bean.BookDetailBeanInOwn
import com.ldp.reader.model.bean.CollBookBean
import com.ldp.reader.model.local.BookRepository
import com.ldp.reader.presenter.BookDetailPresenter
import com.ldp.reader.presenter.contract.BookDetailContract
import com.ldp.reader.ui.base.BaseMVPActivity
import com.ldp.reader.utils.ToastUtils

/**
 * Created by ldp on 17-5-4.
 */
class BookDetailActivity : BookDetailContract.View,
    BaseMVPActivity<BookDetailActivity, BookDetailContract.Presenter<BookDetailActivity>, ActivityBookDetailBinding>()
    {
    /** */
    private var mCollBookBean: CollBookBean? = null
    private var mProgressDialog: ProgressDialog? = null

    /** */
    private var mBookId: String? = null
    private var isBriefOpen = false
    private var isCollected = false
    override fun bindPresenter(): BookDetailContract.Presenter<BookDetailActivity> {
        return BookDetailPresenter() as BookDetailContract.Presenter<BookDetailActivity>
    }

    override fun initData(savedInstanceState: Bundle?) {
        super.initData(savedInstanceState)
        mBookId = if (savedInstanceState != null) {
            savedInstanceState.getString(EXTRA_BOOK_ID)
        } else {
            intent.getStringExtra(EXTRA_BOOK_ID)
        }
    }

    override fun setUpToolbar(toolbar: Toolbar?) {
        super.setUpToolbar(toolbar)
        supportActionBar!!.title = "书籍详情"
    }

    override fun initClick() {
        super.initClick()

        binding?.apply {

            //可伸缩的TextView
            bookDetailTvBrief.setOnClickListener { view ->
                isBriefOpen = if (isBriefOpen) {
                    bookDetailTvBrief.setMaxLines(4)
                    false
                } else {
                    bookDetailTvBrief.setMaxLines(8)
                    true
                }
            }

            bookListAvChase.setOnClickListener { V ->
                //点击存储
                isCollected = if (isCollected) {
                    //放弃点击
                    BookRepository.getInstance()
                        .deleteCollBookInRx(mCollBookBean)
                    bookListTvChase.text = resources.getString(R.string.nb_book_detail_chase_update)
                    val drawable = ResourcesCompat.getDrawable(
                        resources, R.drawable.selector_btn_book_list,
                        null
                    )
                    bookListLlChase.background = drawable
                    bookListTvChase.background = drawable
                    bookListAvChase.speed = -1f
                    bookListAvChase.playAnimation()
                    false
                } else {
                    mPresenter!!.addToBookShelf(mCollBookBean)
                    bookListTvChase.setText(resources.getString(R.string.nb_book_detail_give_up))

                    //修改背景
                    val drawable = resources.getDrawable(R.drawable.shape_common_gray_corner)
                    bookListLlChase.setBackground(drawable)
                    bookListTvChase.setBackground(drawable)
                    bookListAvChase.setSpeed(1f)
                    bookListAvChase.playAnimation()
                    true
                }
            }
            bookDetailTvRead.setOnClickListener { v ->
                startActivityForResult(
                    Intent(this@BookDetailActivity, ReadActivity::class.java)
                        .putExtra(ReadActivity.EXTRA_IS_COLLECTED, isCollected)
                        .putExtra(ReadActivity.EXTRA_COLL_BOOK, mCollBookBean), REQUEST_READ
                )
            }

        }

    }

    override fun processLogic() {
        super.processLogic()
        binding?.refreshLayout?.showLoading()
        mPresenter!!.refreshBookDetail(mBookId)
    }

    override fun getViewBinding(): ActivityBookDetailBinding {
        return ActivityBookDetailBinding.inflate(layoutInflater)
    }

    override fun finishRefresh(bean: BookDetailBeanInOwn) {
        binding?.apply {
            //封面
            Glide.with(this@BookDetailActivity)
                .load(bean.cover)
                .placeholder(R.drawable.ic_book_loading)
                .error(R.drawable.ic_load_error)
                .centerCrop()
                .into(bookDetailIvCover)
            //书名
            bookDetailTvTitle.setText(bean.title)
            //作者
            bookDetailTvAuthor.setText(bean.author)
            //简介
            bookDetailTvBrief.setText(bean.desc)
            mCollBookBean = BookRepository.getInstance().getCollBook(bean.bookId.toString() + "")


            //判断是否收藏
            if (mCollBookBean != null) {
                Log.d(TAG, "finishRefresh: " + "mCollBookBean != null")
                isCollected = true
                bookListTvChase.setText(resources.getString(R.string.nb_book_detail_give_up))
                //修改背景
                val drawable = resources.getDrawable(R.drawable.shape_common_gray_corner)
                bookListTvChase.setBackground(drawable)
                bookListLlChase.setBackground(drawable)
                bookListAvChase.setSpeed(1f)
                bookListAvChase.playAnimation()
                bookDetailTvRead.setText("继续阅读")
            } else {
                mCollBookBean = bean.collBookBean
                Log.d(TAG, "finishRefresh: " + "mCollBookBean = bean.getCollBookBean()")
            }
        }


    }

    override fun waitToBookShelf() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(this)
            mProgressDialog!!.setTitle("正在添加到书架中")
        }
        mProgressDialog!!.show()
    }

    override fun errorToBookShelf() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
        ToastUtils.show("加入书架失败，请检查网络")
    }

    override fun succeedToBookShelf() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
        ToastUtils.show("加入书架成功")
    }

    override fun showError() {
        binding?.refreshLayout?.showError()
    }

    override fun complete() {
        binding?.refreshLayout?.showFinish()
    }

    /** */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_BOOK_ID, mBookId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //如果进入阅读页面收藏了，页面结束的时候，就需要返回改变收藏按钮
        if (requestCode == REQUEST_READ) {
            if (data == null) {
                return
            }
            isCollected = data.getBooleanExtra(RESULT_IS_COLLECTED, false)
            if (isCollected) {
                binding?.apply {
                    bookListTvChase.setText(resources.getString(R.string.nb_book_detail_give_up))
                    //修改背景
                    val drawable = resources.getDrawable(R.drawable.shape_common_gray_corner)
                    bookListTvChase.setBackground(drawable)
                    bookListLlChase.setBackground(drawable)
                    bookDetailTvRead.setText("继续阅读")
                }

            }
        }
    }

    companion object {
        const val RESULT_IS_COLLECTED = "result_is_collected"
        private const val TAG = "BookDetailActivity"
        private const val EXTRA_BOOK_ID = "extra_book_id"
        private const val REQUEST_READ = 1
        public fun startActivity(context: Context, bookId: String?) {
            val intent = Intent(context, BookDetailActivity::class.java)
            intent.putExtra(EXTRA_BOOK_ID, bookId)
            context.startActivity(intent)
        }
    }

}
