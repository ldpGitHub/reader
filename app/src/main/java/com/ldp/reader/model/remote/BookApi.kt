package com.ldp.reader.model.remote

import com.ldp.reader.model.bean.packages.HotWordPackage
import com.ldp.reader.model.bean.packages.KeyWordPackage
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by ldp on 17-4-20.
 */
@JvmSuppressWildcards
interface BookApi {
    @GET("/book/hot-word")
    fun getHotWordPackage(): Call<HotWordPackage>

    @GET("/book/auto-complete")
    fun getKeyWordPacakge(@Query("query") query: String?): Call<KeyWordPackage>
}
