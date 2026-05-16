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
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Created by ldp on 18-10-19.
 */
@JvmSuppressWildcards
interface BookApiOwn {
    @GET("/search")
    fun getSearchResult(@Query("bookName") bookName: String?): Single<List<BookSearchResult>>

    @GET("/getBookInfo")
    fun getBookInfo(@Query("bookId") bookId: String?): Single<BookDetailBeanInOwn>

    @POST("/getBookInfoBatch")
    fun getBookInfoBatch(@Body body: RequestBody?): Single<List<BookDetailBeanInOwn>>

    @GET("/getBookFolder")
    fun getBookFolder(@Query("bookId") bookId: String?): Single<List<ChapterBean>>

    @GET("/getBookContent")
    fun getBookContent(
        @Query("bookId") bookId: String?,
        @Query("chapterId") chapterId: String?,
        @Query("sourceIndex") sourceIndex: Int
    ): Single<ContentBean>

    @POST("/login")
    fun userLogin(
        @Query("username") username: String?,
        @Query("password") password: String?
    ): Single<LoginResultBean>

    @POST("/directLogin")
    fun userDirectLogin(
        @Query("appkey") appkey: String?,
        @Query("appSecret") appSecret: String?,
        @Query("token") token: String?,
        @Query("opToken") opToken: String?,
        @Query("operator") operator: String?,
        @Query("registrationId") registrationId: String?
    ): Single<DirectLoginResultBean>

    @POST("/getBookShelf")
    fun getBookShelf(@Header("Authorization") header: String?): Single<List<BookIdBean>>

    @GET("/getBookShelfByMobile")
    fun getBookShelfByMobile(
        @Query("mobile") mobile: String?,
        @Query("mobileToken") mobileToken: String?
    ): Single<List<BookIdBean>>

    @POST("/synBookShelf")
    fun setBookShelf(
        @Header("Authorization") header: String?,
        @Body body: RequestBody?
    ): Single<SyncBookShelfBean>

    @POST("/synBookShelfByMobile")
    fun setBookShelfByMobile(@Body body: RequestBody?): Single<SyncBookShelfBean>

    @POST("/smsLogin")
    fun smsLogin(
        @Query("phoneNumber") phoneNumber: String?,
        @Query("smsCode") smsCode: String?,
        @Query("registrationId") registrationId: String?
    ): Single<SmsLoginBean>
}
