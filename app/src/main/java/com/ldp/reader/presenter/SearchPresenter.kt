package com.ldp.reader.presenter

import android.util.Log
import com.ldp.reader.model.remote.RemoteRepository
import com.ldp.reader.presenter.contract.SearchContract
import com.ldp.reader.ui.base.RxPresenter
import com.ldp.reader.utils.LogUtils
import com.ldp.reader.utils.RxUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by ldp on 17-6-2.
 */
class SearchPresenter : RxPresenter<SearchContract.View>(),
    SearchContract.Presenter<SearchContract.View> {

    override fun searchHotWord() {
        val disp = RemoteRepository.getInstance()
            .getHotWords()
            .compose { upstream -> RxUtils.toSimpleSingle(upstream) }
            .subscribe(
                { bean ->
                    mView!!.finishHotWords(bean)
                    Log.d("+bean", bean.toString())
                    LogUtils.e(bean)
                },
                { e ->
                    LogUtils.e(e)
                }
            )
        addDisposable(disp)
    }

    override fun searchKeyWord(query: String?) {
        val disp = RemoteRepository.getInstance()
            .getKeyWords(query)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { bean ->
                    Log.d("+bean", bean.toString())

                    mView!!.finishKeyWords(bean)
                    LogUtils.d("+bean", bean)
                },
                { e ->
                    LogUtils.e(e)
                }
            )
        addDisposable(disp)
    }

    override fun searchBook(query: String?) {
        Log.d(TAG, "searchBook: $query")
        val disp = RemoteRepository.getInstance()
            .getSearchResult(query)
            .compose { upstream -> RxUtils.toSimpleSingle(upstream) }
            .subscribe(
                { bookSearchResults -> mView!!.finishBooks(bookSearchResults) },
                { throwable ->
                    LogUtils.e(throwable)
                    mView!!.errorBooks()
                }
            )
        addDisposable(disp)
    }

    companion object {
        private val TAG = SearchPresenter::class.java.simpleName
    }
}
