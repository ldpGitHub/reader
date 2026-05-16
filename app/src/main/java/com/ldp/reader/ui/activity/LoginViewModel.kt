package com.ldp.reader.ui.activity

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ldp.reader.model.bean.DirectLoginResultBean
import com.ldp.reader.model.bean.LoginResultBean
import com.ldp.reader.model.bean.SmsLoginBean
import com.ldp.reader.model.remote.RemoteRepository
import com.ldp.reader.utils.SharedPreUtils
import com.mob.pushsdk.MobPush
import com.mob.pushsdk.MobPushCallback
import com.mob.secverify.OperationCallback
import com.mob.secverify.SecVerify
import com.mob.secverify.VerifyCallback
import com.mob.secverify.common.exception.VerifyException
import com.mob.secverify.datatype.VerifyResult
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _loginResults = MutableLiveData<LoginResultBean>()
    private val _directLoginResults = MutableLiveData<DirectLoginResultBean>()
    private val _smsLoginResults = MutableLiveData<SmsLoginBean>()
    private val _loginErrors = MutableLiveData<Int>()
    private val _directLoginErrors = MutableLiveData<Int>()
    private var loginErrorVersion = 0
    private var directLoginErrorVersion = 0
    private var registrationId: String? = null

    val loginResults: LiveData<LoginResultBean> = _loginResults
    val directLoginResults: LiveData<DirectLoginResultBean> = _directLoginResults
    val smsLoginResults: LiveData<SmsLoginBean> = _smsLoginResults
    val loginErrors: LiveData<Int> = _loginErrors
    val directLoginErrors: LiveData<Int> = _directLoginErrors

    fun userLogin(userName: String?, passWord: String?) {
        viewModelScope.launch {
            try {
                _loginResults.value = RemoteRepository.getInstance().userLogin(userName, passWord)
            } catch (e: Throwable) {
                _loginErrors.value = ++loginErrorVersion
            }
        }
    }

    fun preDirectLogin() {
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

    fun smsLogin(phoneNumber: String?, smsCode: String?, registrationId: String?) {
        viewModelScope.launch {
            try {
                _smsLoginResults.value = RemoteRepository.getInstance().smsLogin(phoneNumber, smsCode, registrationId)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                _loginErrors.value = ++loginErrorVersion
            }
        }
    }

    fun directLogin() {
        Log.d(TAG, "directLogin: ")
        registrationId = SharedPreUtils.getInstance().getString("registrationId")
        SecVerify.verify(object : VerifyCallback() {
            override fun onOtherLogin() {
                _directLoginErrors.postValue(++directLoginErrorVersion)
            }

            override fun onUserCanceled() {
            }

            override fun onComplete(data: VerifyResult?) {
                Log.e(TAG, "onComplete: ")
                Log.e(TAG, "onComplete: $data")
                Log.e(TAG, "onComplete: " + data!!.token)
                Log.e(TAG, "onComplete: registrationId $registrationId")

                viewModelScope.launch {
                    try {
                        _directLoginResults.value = RemoteRepository.getInstance().userDirectLogin(data, registrationId)
                    } catch (throwable: Throwable) {
                        Log.e(TAG, "accept: " + throwable.message + throwable.cause)
                        _loginErrors.value = ++loginErrorVersion
                    }
                }
            }

            override fun onFailure(e: VerifyException?) {
                Log.d(TAG, "onFailure: " + e.toString())
                _directLoginErrors.postValue(++directLoginErrorVersion)
            }
        })
    }

    companion object {
        private val TAG = LoginViewModel::class.java.simpleName
    }
}
