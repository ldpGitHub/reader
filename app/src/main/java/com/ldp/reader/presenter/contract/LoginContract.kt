package com.ldp.reader.presenter.contract

import com.ldp.reader.model.bean.DirectLoginResultBean
import com.ldp.reader.model.bean.LoginResultBean
import com.ldp.reader.model.bean.SmsLoginBean
import com.ldp.reader.ui.base.BaseContract

/**
 * Created by ldp on 17-6-2.
 */
@JvmSuppressWildcards
interface LoginContract : BaseContract {
    interface View : BaseContract.BaseView {
        fun finishLogin(loginResultBean: LoginResultBean)

        fun finishDirectLogin(loginResultBean: DirectLoginResultBean)

        fun finishSmsLogin(smsLoginBean: SmsLoginBean)

        fun showDirectLoginError()
    }

    interface Presenter<T : BaseContract.BaseView> : BaseContract.BasePresenter<T> {
        fun userLogin(userName: String?, passWord: String?)

        fun preDirectLogin()

        fun smsLogin(phoneNumber: String?, smsCode: String?, registrationId: String?)

        fun directLogin()
    }
}
