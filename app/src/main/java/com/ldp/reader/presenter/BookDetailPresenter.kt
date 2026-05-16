package com.ldp.reader.presenter

import android.util.Log
import com.google.gson.Gson
import com.ldp.reader.model.bean.BookChapterBean
import com.ldp.reader.model.bean.BookDetailBeanInOwn
import com.ldp.reader.model.bean.ChapterBean
import com.ldp.reader.model.bean.CollBookBean
import com.ldp.reader.model.bean.DirectSycBookShelfBean
import com.ldp.reader.model.bean.SyncBookShelfBean
import com.ldp.reader.model.local.BookRepository
import com.ldp.reader.model.remote.RemoteRepository
import com.ldp.reader.presenter.contract.BookDetailContract
import com.ldp.reader.ui.base.RxPresenter
import com.ldp.reader.utils.MD5Utils
import com.ldp.reader.utils.RxUtils
import com.ldp.reader.utils.SharedPreUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

/**
 * Created by ldp on 17-5-4.
 */
class BookDetailPresenter : RxPresenter<BookDetailContract.View>(),
    BookDetailContract.Presenter<BookDetailContract.View> {
    private var bookId: String? = null

    override fun refreshBookDetail(bookId: String?) {
        this.bookId = bookId
        refreshBook()
    }

    override fun addToBookShelf(collBook: CollBookBean?) {
        val collBookBean = collBook!!
        val bookChapterBeans: MutableList<BookChapterBean> = ArrayList()
        BookRepository.getInstance()
            .saveCollBookWithAsync(collBookBean)
        Log.d(TAG, "addToBookShelf: $bookId")
        val disposable = RemoteRepository.getInstance()
            .getBookFolder(bookId)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe {
                mView!!.waitToBookShelf()
            }
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { chapterBeans: List<ChapterBean> ->
                    for (chapterBean in chapterBeans) {
                        val bookChapterBeanTemp = BookChapterBean()
                        bookChapterBeanTemp.link = chapterBean.chapterId.toString()
                        bookChapterBeanTemp.title = chapterBean.title
                        bookChapterBeanTemp.id = MD5Utils.strToMd5By16(bookChapterBeanTemp.link!!)
                        bookChapterBeanTemp.bookId = collBookBean.get_id()
                        bookChapterBeanTemp.start = bookChapterBeans.size.toLong()
                        bookChapterBeans.add(bookChapterBeanTemp)
                    }
                    collBookBean.bookChapters = bookChapterBeans
                    collBookBean.chaptersCount = bookChapterBeans.size
                    BookRepository.getInstance()
                        .saveCollBookWithAsync(collBookBean)
                    mView!!.succeedToBookShelf()
                    val collBookBeanResult = BookRepository.getInstance().getCollBook(bookId)
                    Log.d(TAG, "addToBookShelf:collBookBeanResult $collBookBeanResult")

                    synBookShelf()
                },
                {
                    mView!!.errorToBookShelf()
                }
            )

        addDisposable(disposable)
    }

    private fun synBookShelf() {
        val collBooks = BookRepository.getInstance().collBooks
        val bookIds = BookShelfPresenter.onlineBookIdsFrom(collBooks)
        if ("password" == SharedPreUtils.getInstance().getString("loginType")) {
            setBookShelf(bookIds)
        } else {
            val mobile = SharedPreUtils.getInstance().getString("userName")
            val mobileToken = SharedPreUtils.getInstance().getString("token")
            setBookShelfByMobile(bookIds, mobile, mobileToken)
        }
    }

    private fun setBookShelf(bookIds: List<String>) {
        val body = Gson().toJson(BookShelfPresenter.normalizeServerBookIds(bookIds)).toRequestBody(JSON)
        val token = SharedPreUtils.getInstance().getString("token")
        val disposable = RemoteRepository.getInstance().setBookShelf(token, body)
            .compose { upstream -> RxUtils.toSimpleSingle(upstream) }
            .subscribe(
                { _: SyncBookShelfBean -> },
                {}
            )
        addDisposable(disposable)
    }

    private fun setBookShelfByMobile(bookIds: List<String>, mobile: String?, mobileToken: String?) {
        val directSycBookShelfBean = DirectSycBookShelfBean()
        directSycBookShelfBean.bookIds = BookShelfPresenter.normalizeServerBookIds(bookIds)
        directSycBookShelfBean.mobile = mobile
        directSycBookShelfBean.mobileToken = mobileToken
        val body = Gson().toJson(directSycBookShelfBean).toRequestBody(JSON)
        val disposable = RemoteRepository.getInstance().setBookShelfByMobile(body)
            .compose { upstream -> RxUtils.toSimpleSingle(upstream) }
            .subscribe(
                { _: SyncBookShelfBean -> },
                {}
            )
        addDisposable(disposable)
    }

    private fun refreshBook() {
        val disposable = RemoteRepository
            .getInstance()
            .getBookInfo(bookId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { bookDetailBeanInOwn: BookDetailBeanInOwn ->
                    mView!!.finishRefresh(bookDetailBeanInOwn)
                    mView!!.complete()
                },
                {
                    mView!!.showError()
                }
            )

        addDisposable(disposable)
    }

    companion object {
        private const val TAG = "BookDetailPresenter"
        private val JSON: MediaType? = "application/json; charset=utf-8".toMediaTypeOrNull()
    }
}
