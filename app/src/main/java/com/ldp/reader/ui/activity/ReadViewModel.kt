package com.ldp.reader.ui.activity

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ldp.reader.model.bean.BookChapterBean
import com.ldp.reader.model.bean.ChapterBean
import com.ldp.reader.model.bean.CollBookBean
import com.ldp.reader.model.bean.ContentBean
import com.ldp.reader.model.local.BookRepository
import com.ldp.reader.model.remote.RemoteRepository
import com.ldp.reader.utils.LogUtils
import com.ldp.reader.utils.MD5Utils
import com.ldp.reader.utils.RxUtils
import com.ldp.reader.widget.page.TxtChapter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.ArrayDeque
import java.util.Arrays

/**
 * Created by ldp on 17-5-16.
 */
class ReadViewModel : ViewModel() {
    data class CategoryResult(
        val bookChapterList: List<BookChapterBean>,
        val bookId: String,
        val isBiqugeLoaded: Boolean
    )

    private val disposables = CompositeDisposable()
    private val _categories = MutableLiveData<CategoryResult>()
    private val _chapterFinishedEvents = MutableLiveData<Boolean>()
    private val _chapterErrorEvents = MutableLiveData<Int>()
    private var chapterErrorVersion = 0
    private var mChapterSub: Subscription? = null
    private var bookIdInBiquge: String? = ""

    val categories: LiveData<CategoryResult> = _categories
    val chapterFinishedEvents: LiveData<Boolean> = _chapterFinishedEvents
    val chapterErrorEvents: LiveData<Int> = _chapterErrorEvents

    fun loadCategory(bookId: String?) {
        val bookChapterBeans: MutableList<BookChapterBean> = ArrayList()
        val collBookBean = BookRepository.getInstance().getCollBook(bookId)!!

        Log.d(TAG, "loadCategory: $bookId$collBookBean")
        val disposable = RemoteRepository.getInstance()
            .getBookFolder(bookId)
            .compose { upstream -> RxUtils.toSimpleSingle(upstream) }
            .subscribe(
                { chapterBeans: List<ChapterBean> ->
                    for (chapterBean in chapterBeans) {
                        val bookChapterBeanTemp = BookChapterBean()
                        bookChapterBeanTemp.link = chapterBean.chapterId.toString()
                        bookChapterBeanTemp.title = chapterBean.title
                        bookChapterBeanTemp.id = MD5Utils.strToMd5By16(bookChapterBeanTemp.link!!)
                        Log.d(TAG, "+章节名  " + chapterBean.title)
                        bookChapterBeanTemp.bookId = collBookBean.get_id()
                        bookChapterBeanTemp.start = bookChapterBeans.size.toLong()
                        bookChapterBeans.add(bookChapterBeanTemp)
                    }
                    collBookBean.bookChapters = bookChapterBeans
                    collBookBean.chaptersCount = bookChapterBeans.size
                    Log.d(TAG, "accept: $bookChapterBeans")
                    _categories.value = CategoryResult(bookChapterBeans, bookId!!, true)

                    BookRepository.getInstance()
                        .saveCollBookWithAsync(collBookBean)
                },
                {
                    _chapterErrorEvents.value = ++chapterErrorVersion
                }
            )
        disposables.add(disposable)
    }

    @Synchronized
    fun loadChapter(bookId: String?, bookChapterList: List<TxtChapter>) {
        val size = bookChapterList.size
        Log.e(TAG, "loadChapter  列表大小" + size + Arrays.asList(bookChapterList).toString())

        if (mChapterSub != null) {
            mChapterSub!!.cancel()
        }

        val bookChapterSBeanByBiquge: MutableList<Single<ContentBean>> = ArrayList(bookChapterList.size)

        val titlesInBiquge = ArrayDeque<String>()

        for (i in 0 until size) {
            val bookChapter = bookChapterList[i]
            if (bookChapter.title == null || TextUtils.isEmpty(bookChapter.title)) {
                continue
            }
            val pureLink = bookChapter.link
            val bean = BookRepository.getInstance().getCollBook(bookId)!!
            bookIdInBiquge = bean.get_id()
            Log.d("+收到的章节笔趣阁Id", bookIdInBiquge!!)
            val bookChapterBeanByBiqugeSingle = RemoteRepository.getInstance()
                .getBookContent(bookIdInBiquge, pureLink, 0)
            Log.d("+收到的章节ID", bookChapter.link!!)
            bookChapterSBeanByBiquge.add(bookChapterBeanByBiqugeSingle)
            titlesInBiquge.add(bookChapter.title!!)
        }

        Single.concat(bookChapterSBeanByBiquge)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Subscriber<ContentBean> {
                var titleInBiquge: String? = titlesInBiquge.poll()

                override fun onSubscribe(s: Subscription) {
                    s.request(Int.MAX_VALUE.toLong())
                    mChapterSub = s
                }

                override fun onNext(contentBean: ContentBean) {
                    BookRepository.getInstance().saveChapterInfo(
                        bookId,
                        titleInBiquge,
                        contentBean.content
                    )
                    Log.e(
                        "+chapterBody",
                        "title" + titleInBiquge + titlesInBiquge + " " + contentBean.content
                    )
                    _chapterFinishedEvents.value = false
                    titleInBiquge = titlesInBiquge.poll()
                }

                override fun onError(t: Throwable) {
                    if (bookChapterList[0].title == titleInBiquge) {
                        _chapterErrorEvents.value = ++chapterErrorVersion
                    }
                    LogUtils.e(t)
                }

                override fun onComplete() {
                }
            })
    }

    @Synchronized
    fun refreshChapter(bookId: String?, bookChapter: TxtChapter?, sourceIndex: Int) {
        val pureLink = bookChapter!!.link
        val bean = BookRepository.getInstance().getCollBook(bookId)!!
        bookIdInBiquge = bean.get_id()
        val disposable = RemoteRepository.getInstance()
            .getBookContent(bookIdInBiquge, pureLink, sourceIndex)
            .compose { upstream -> RxUtils.toSimpleSingle(upstream) }
            .subscribe(
                { contentBean ->
                    BookRepository.getInstance().saveChapterInfo(
                        bookId,
                        bookChapter.title,
                        contentBean.content
                    )
                    _chapterFinishedEvents.value = true
                },
                {
                    _chapterErrorEvents.value = ++chapterErrorVersion
                }
            )
        disposables.add(disposable)
    }

    override fun onCleared() {
        if (mChapterSub != null) {
            mChapterSub!!.cancel()
        }
        disposables.clear()
        super.onCleared()
    }

    companion object {
        private val TAG = ReadViewModel::class.java.simpleName
    }
}
