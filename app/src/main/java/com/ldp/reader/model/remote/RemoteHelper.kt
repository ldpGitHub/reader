package com.ldp.reader.model.remote

import com.ldp.reader.utils.Constant
import com.ldp.reader.utils.LenientGsonConverterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RemoteHelper private constructor() {
    private val mRetrofit: Retrofit
    private val mRetrofitByOwn: Retrofit
    private val mOkHttpClient: OkHttpClient

    init {
        val logIntercept: Interceptor = HttpLoggingInterceptor()
        (logIntercept as HttpLoggingInterceptor).level = HttpLoggingInterceptor.Level.BODY
        mOkHttpClient = OkHttpClient.Builder()
            .addInterceptor(logIntercept)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

        mRetrofit = Retrofit.Builder()
            .client(mOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(Constant.API_BASE_URL)
            .build()
        mRetrofitByOwn = Retrofit.Builder()
            .client(mOkHttpClient)
            .addConverterFactory(LenientGsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(Constant.API_BASE_URL_OWN)
            .build()
    }

    fun getRetrofit(): Retrofit {
        return mRetrofit
    }

    fun getRetrofitByOwn(): Retrofit {
        return mRetrofitByOwn
    }

    fun getOkHttpClient(): OkHttpClient {
        return mOkHttpClient
    }

    companion object {
        @Volatile
        private var sInstance: RemoteHelper? = null

        @JvmStatic
        fun getInstance(): RemoteHelper {
            if (sInstance == null) {
                synchronized(RemoteHelper::class.java) {
                    if (sInstance == null) {
                        sInstance = RemoteHelper()
                    }
                }
            }
            return sInstance!!
        }
    }
}
