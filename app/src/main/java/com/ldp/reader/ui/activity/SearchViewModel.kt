package com.ldp.reader.ui.activity

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ldp.reader.model.bean.BookSearchResult
import com.ldp.reader.model.remote.RemoteRepository
import com.ldp.reader.utils.LogUtils
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val _hotWords = MutableLiveData<List<String>>()
    private val _keyWords = MutableLiveData<List<String>>()
    private val _books = MutableLiveData<List<BookSearchResult>>()
    private val _bookSearchErrors = MutableLiveData<Int>()
    private var bookSearchErrorVersion = 0

    val hotWords: LiveData<List<String>> = _hotWords
    val keyWords: LiveData<List<String>> = _keyWords
    val books: LiveData<List<BookSearchResult>> = _books
    val bookSearchErrors: LiveData<Int> = _bookSearchErrors

    fun searchHotWord() {
        viewModelScope.launch {
            try {
                val bean = RemoteRepository.getInstance().getHotWords()
                _hotWords.value = bean
                Log.d("+bean", bean.toString())
                LogUtils.e(bean)
            } catch (e: Throwable) {
                LogUtils.e(e)
            }
        }
    }

    fun searchKeyWord(query: String?) {
        viewModelScope.launch {
            try {
                val bean = RemoteRepository.getInstance().getKeyWords(query)
                Log.d("+bean", bean.toString())
                _keyWords.value = bean
                LogUtils.d("+bean", bean)
            } catch (e: Throwable) {
                LogUtils.e(e)
            }
        }
    }

    fun searchBook(query: String?) {
        Log.d(TAG, "searchBook: $query")
        viewModelScope.launch {
            try {
                _books.value = RemoteRepository.getInstance().getSearchResult(query)
            } catch (throwable: Throwable) {
                LogUtils.e(throwable)
                _bookSearchErrors.value = ++bookSearchErrorVersion
            }
        }
    }

    companion object {
        private val TAG = SearchViewModel::class.java.simpleName
    }
}
