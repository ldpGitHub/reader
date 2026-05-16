package com.ldp.reader.model.remote

import com.ldp.reader.model.bean.BookDetailBeanInOwn
import com.ldp.reader.model.bean.BookIdBean
import com.ldp.reader.model.bean.BookSearchResult
import com.ldp.reader.model.bean.ChapterBean
import com.ldp.reader.model.bean.ContentBean
import com.ldp.reader.model.bean.DirectLoginResultBean
import com.ldp.reader.model.bean.LoginResultBean
import com.ldp.reader.model.bean.SmsLoginBean
import com.ldp.reader.model.bean.SyncBookShelfBean
import com.ldp.reader.utils.Constant.APP_KEY
import com.ldp.reader.utils.Constant.APP_SECRET
import com.mob.secverify.datatype.VerifyResult
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.Retrofit

class RemoteRepository private constructor() {
    private val mRetrofit: Retrofit
    private val mRetrofitByOwn: Retrofit
    private val mBookApi: BookApi
    private val mBookApiOwn: BookApiOwn

    init {
        mRetrofit = RemoteHelper.getInstance().getRetrofit()
        mBookApi = mRetrofit.create(BookApi::class.java)

        mRetrofitByOwn = RemoteHelper.getInstance().getRetrofitByOwn()
        mBookApiOwn = mRetrofitByOwn.create(BookApiOwn::class.java)
    }

    fun getSearchResult(bookName: String?): Single<List<BookSearchResult>> {
        return mBookApiOwn.getSearchResult(bookName)
    }

    fun getBookInfo(bookId: String?): Single<BookDetailBeanInOwn> {
        return mBookApiOwn.getBookInfo(bookId)
    }

    fun getBookInfoBatch(body: RequestBody?): Single<List<BookDetailBeanInOwn>> {
        return mBookApiOwn.getBookInfoBatch(body)
    }

    fun getBookFolder(bookId: String?): Single<List<ChapterBean>> {
        return mBookApiOwn.getBookFolder(bookId)
    }

    fun getBookContent(bookId: String?, chapterId: String?, sourceIndex: Int): Single<ContentBean> {
        return mBookApiOwn.getBookContent(bookId, chapterId, sourceIndex)
    }

    fun userLogin(userNameInput: String?, passwordInput: String?): Single<LoginResultBean> {
        return mBookApiOwn.userLogin(userNameInput, passwordInput)
    }

    fun smsLogin(phoneNumber: String?, smsCode: String?, registrationId: String?): Single<SmsLoginBean> {
        return mBookApiOwn.smsLogin(phoneNumber, smsCode, registrationId)
    }

    fun userDirectLogin(verifyResult: VerifyResult?, registrationId: String?): Single<DirectLoginResultBean> {
        return mBookApiOwn.userDirectLogin(
            APP_KEY,
            APP_SECRET,
            verifyResult!!.token,
            verifyResult.opToken,
            verifyResult.operator,
            registrationId
        )
    }

    fun getBookShelf(header: String?): Single<List<BookIdBean>> {
        return mBookApiOwn.getBookShelf(header)
    }

    fun getBookShelfByMobile(mobile: String?, token: String?): Single<List<BookIdBean>> {
        return mBookApiOwn.getBookShelfByMobile(mobile, token)
    }

    fun setBookShelf(token: String?, body: RequestBody?): Single<SyncBookShelfBean> {
        return mBookApiOwn.setBookShelf(token, body)
    }

    fun setBookShelfByMobile(body: RequestBody?): Single<SyncBookShelfBean> {
        return mBookApiOwn.setBookShelfByMobile(body)
    }

    fun getHotWords(): Single<List<String>> {
        return mBookApi.getHotWordPackage().map { bean -> bean.hotWords!! }
    }

    fun getKeyWords(query: String?): Single<List<String>> {
        return mBookApi.getKeyWordPacakge(query).map { bean -> bean.keywords!! }
    }

    companion object {
        @Volatile
        private var sInstance: RemoteRepository? = null

        @JvmStatic
        fun getInstance(): RemoteRepository {
            if (sInstance == null) {
                synchronized(RemoteHelper::class.java) {
                    if (sInstance == null) {
                        sInstance = RemoteRepository()
                    }
                }
            }
            return sInstance!!
        }
    }
}
