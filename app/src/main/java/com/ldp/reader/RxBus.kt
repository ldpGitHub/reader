package com.ldp.reader

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class RxBus private constructor() {
    private val mEventBus: PublishSubject<Any> = PublishSubject.create()

    fun post(event: Any) {
        mEventBus.onNext(event)
    }

    fun post(code: Int, event: Any) {
        val msg = Message(code, event)
        mEventBus.onNext(msg)
    }

    fun toObservable(): Observable<Any> {
        return mEventBus
    }

    fun <T> toObservable(cls: Class<T>): Observable<T> {
        return mEventBus.ofType(cls)
    }

    fun <T> toObservable(code: Int, cls: Class<T>): Observable<T> {
        return mEventBus.ofType(Message::class.java)
            .filter { msg -> msg.code == code && cls.isInstance(msg.event) }
            .map { msg -> cls.cast(msg.event) }
    }

    private class Message(val code: Int, val event: Any)

    companion object {
        @Volatile
        private var sInstance: RxBus? = null

        @JvmStatic
        fun getInstance(): RxBus {
            if (sInstance == null) {
                synchronized(RxBus::class.java) {
                    if (sInstance == null) {
                        sInstance = RxBus()
                    }
                }
            }
            return sInstance!!
        }
    }
}
