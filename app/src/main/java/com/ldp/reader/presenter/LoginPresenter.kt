package com.ldp.reader.presenter

import android.text.TextUtils
import android.util.Log
import com.ldp.reader.model.remote.RemoteRepository
import com.ldp.reader.presenter.contract.LoginContract
import com.ldp.reader.ui.base.RxPresenter
import com.ldp.reader.utils.RxUtils
import com.ldp.reader.utils.SharedPreUtils
import com.mob.pushsdk.MobPush
import com.mob.pushsdk.MobPushCallback
import com.mob.secverify.OperationCallback
import com.mob.secverify.SecVerify
import com.mob.secverify.VerifyCallback
import com.mob.secverify.common.exception.VerifyException
import com.mob.secverify.datatype.VerifyResult

/**
 * Created by ldp on 17-6-2.
 */
class LoginPresenter : RxPresenter<LoginContract.View>(),
    LoginContract.Presenter<LoginContract.View> {
    private var registrationId: String? = null

    override fun userLogin(userName: String?, passWord: String?) {
        val disposableLogin = RemoteRepository.getInstance().userLogin(userName, passWord)
            .compose { upstream -> RxUtils.toSimpleSingle(upstream) }
            .subscribe(
                { loginResultBean ->
                    if (mView == null) {
                        return@subscribe
                    }
                    mView!!.finishLogin(loginResultBean)
                },
                {
                    if (mView == null) {
                        return@subscribe
                    }
                    mView!!.showError()
                }
            )
        addDisposable(disposableLogin)
    }

    override fun preDirectLogin() {
        Log.d(TAG, "preDirectLogin: registrationId")
        registrationId = SharedPreUtils.getInstance().getString("registrationId")
        Log.d(TAG, "onCallback: registrationId  $registrationId")
        if (TextUtils.isEmpty(registrationId)) {
            MobPush.getRegistrationId(object : MobPushCallback<String> {
                override fun onCallback(s: String?) {
                    Log.d(TAG, "onCallback: registrationId  $s")
                    registrationId = s
                    SharedPreUtils.getInstance().putString("registrationId", registrationId)
                }
            })
        }
        SecVerify.preVerify(object : OperationCallback<Void>() {
            override fun onComplete(data: Void?) {
                Log.d(TAG, "onComplete: $data")
            }

            override fun onFailure(e: VerifyException?) {
                Log.d(TAG, "onFailure: $e")
            }
        })
    }

    override fun smsLogin(phoneNumber: String?, smsCode: String?, registrationId: String?) {
        val disposable = RemoteRepository.getInstance().smsLogin(phoneNumber, smsCode, registrationId)
            .compose { upstream -> RxUtils.toSimpleSingle(upstream) }
            .subscribe(
                { smsLoginBean ->
                    if (mView == null) {
                        return@subscribe
                    }
                    mView!!.finishSmsLogin(smsLoginBean)
                },
                { throwable ->
                    throwable.printStackTrace()
                    if (mView != null) {
                        mView!!.showError()
                    }
                }
            )
        addDisposable(disposable)
    }

    override fun directLogin() {
        Log.d(TAG, "directLogin: ")
        registrationId = SharedPreUtils.getInstance().getString("registrationId")
        SecVerify.verify(object : VerifyCallback() {
            override fun onOtherLogin() {
                if (mView != null) {
                    mView!!.showDirectLoginError()
                }
            }

            override fun onUserCanceled() {
            }

            override fun onComplete(data: VerifyResult?) {
                Log.e(TAG, "onComplete: ")
                Log.e(TAG, "onComplete: $data")
                Log.e(TAG, "onComplete: " + data!!.token)
                Log.e(TAG, "onComplete: registrationId $registrationId")

                val disposable = RemoteRepository.getInstance().userDirectLogin(data, registrationId)
                    .compose { upstream -> RxUtils.toSimpleSingle(upstream) }
                    .subscribe(
                        { directLoginResultBean ->
                            if (mView == null) {
                                return@subscribe
                            }
                            mView!!.finishDirectLogin(directLoginResultBean)
                        },
                        { throwable ->
                            Log.e(TAG, "accept: " + throwable.message + throwable.cause)
                            if (mView == null) {
                                return@subscribe
                            }
                            mView!!.showError()
                        }
                    )
                addDisposable(disposable)
            }

            override fun onFailure(e: VerifyException?) {
                Log.d(TAG, "onFailure: " + e.toString())
                if (mView != null) {
                    mView!!.showDirectLoginError()
                }
            }
        })
    }

    companion object {
        private val TAG = LoginPresenter::class.java.simpleName
    }
}
