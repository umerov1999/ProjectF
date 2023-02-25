package dev.ragnarok.fenrir.dialog.directauth

import android.os.Bundle
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.api.Auth.scope
import dev.ragnarok.fenrir.api.CaptchaNeedException
import dev.ragnarok.fenrir.api.NeedValidationException
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.LoginResponse
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Captcha
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import java.util.concurrent.TimeUnit

class DirectAuthPresenter(savedInstanceState: Bundle?) :
    RxSupportPresenter<IDirectAuthView>(savedInstanceState) {
    private val networker: INetworker = networkInterfaces
    private var requiredCaptcha: Captcha? = null
    private var requireSmsCode = false
    private var requireAppCode = false
    private var savePassword = false
    private var loginNow = false
    private var username: String? = null
    private var pass: String? = null
    private var smsCode: String? = null
    private var captcha: String? = null
    private var appCode: String? = null
    private var RedirectUrl: String? = null
    fun fireLoginClick() {
        doLogin(false)
    }

    private fun doLogin(forceSms: Boolean) {
        view?.hideKeyboard()
        val trimmedUsername = if (username.nonNullNoEmpty()) username?.trim { it <= ' ' } else ""
        val trimmedPass = if (pass.nonNullNoEmpty()) pass?.trim { it <= ' ' } else ""
        val captchaSid = if (requiredCaptcha != null) requiredCaptcha?.sid else null
        val captchaCode = if (captcha.nonNullNoEmpty()) captcha?.trim { it <= ' ' } else null
        val code: String? = if (requireSmsCode) {
            if (smsCode.nonNullNoEmpty<CharSequence>()) smsCode?.trim { it <= ' ' } else null
        } else if (requireAppCode) {
            if (appCode.nonNullNoEmpty<CharSequence>()) appCode?.trim { it <= ' ' } else null
        } else {
            null
        }
        setLoginNow(true)
        appendDisposable(networker.vkDirectAuth()
            .directLogin(
                "password",
                Constants.API_ID,
                Constants.SECRET,
                trimmedUsername,
                trimmedPass,
                Constants.AUTH_VERSION,
                Constants.DEFAULT_ACCOUNT_TYPE == AccountType.VK_ANDROID,
                scope,
                code,
                captchaSid,
                captchaCode,
                forceSms,
                Constants.DEFAULT_ACCOUNT_TYPE == AccountType.VK_ANDROID
            )
            .fromIOToMain()
            .subscribe({ response -> onLoginResponse(response) }) { t ->
                onLoginError(
                    getCauseIfRuntime(t)
                )
            })
    }

    private fun onLoginError(t: Throwable) {
        setLoginNow(false)
        requiredCaptcha = null
        requireAppCode = false
        requireSmsCode = false
        if (t is CaptchaNeedException) {
            val sid = t.sid ?: return showError(t)
            val img = t.img ?: return showError(t)
            requiredCaptcha = Captcha(sid, img)
        } else if (t is NeedValidationException) {
            if (Constants.DEFAULT_ACCOUNT_TYPE == AccountType.KATE) {
                RedirectUrl = t.validationURL
                if (!RedirectUrl.isNullOrEmpty()) {
                    onValidate()
                }
            } else {
                val type = t.validationType
                val sid = t.sid
                when {
                    "2fa_sms".equals(type, ignoreCase = true) || "2fa_libverify".equals(
                        type,
                        ignoreCase = true
                    ) -> {
                        requireSmsCode = true
                        RedirectUrl = t.validationURL
                    }

                    "2fa_app".equals(type, ignoreCase = true) -> {
                        requireAppCode = true
                    }

                    else -> {
                        showError(t)
                        RedirectUrl = t.validationURL
                        if (!RedirectUrl.isNullOrEmpty()) {
                            onValidate()
                        }
                    }
                }
                if (!sid.isNullOrEmpty() && requireSmsCode) {
                    appendDisposable(networker.vkAuth()
                        .validatePhone(
                            Constants.API_ID,
                            Constants.API_ID,
                            Constants.SECRET,
                            sid,
                            Constants.AUTH_VERSION,
                            true
                        )
                        .delay(1, TimeUnit.SECONDS)
                        .fromIOToMain()
                        .subscribe({ }) {
                            showError(getCauseIfRuntime(t))
                        })
                }
            }
        } else {
            showError(t)
        }
        resolveCaptchaViews()
        resolveSmsRootVisibility()
        resolveAppCodeRootVisibility()
        resolveButtonLoginState()
        when {
            requiredCaptcha != null -> {
                view?.moveFocusToCaptcha()
            }

            requireSmsCode -> {
                view?.moveFocusToSmsCode()
            }

            requireAppCode -> {
                view?.moveFocusToAppCode()
            }
        }
    }

    private fun resolveSmsRootVisibility() {
        view?.setSmsRootVisible(requireSmsCode)
    }

    private fun resolveAppCodeRootVisibility() {
        view?.setAppCodeRootVisible(
            requireAppCode
        )
    }

    private fun resolveCaptchaViews() {
        view?.setCaptchaRootVisible(requiredCaptcha != null)
        if (requiredCaptcha != null) {
            view?.displayCaptchaImage(
                requiredCaptcha?.img
            )
        }
    }

    private fun onLoginResponse(response: LoginResponse) {
        setLoginNow(false)
        var TwFa = "none"
        if (response.access_token.nonNullNoEmpty() && response.user_id > 0) {
            val Pass = if (pass.nonNullNoEmpty()) pass?.trim { it <= ' ' } else ""
            if (requireSmsCode) TwFa = "2fa_sms" else if (requireAppCode) TwFa = "2fa_app"
            val TwFafin = TwFa
            view?.returnSuccessToParent(
                response.user_id,
                response.access_token,
                if (username.nonNullNoEmpty()) username?.trim { it <= ' ' } else "",
                Pass,
                TwFafin,
                savePassword)
        }
    }

    private fun onValidate() {
        view?.returnSuccessValidation(
            RedirectUrl,
            if (username.nonNullNoEmpty()) username?.trim { it <= ' ' } else "",
            if (pass.nonNullNoEmpty()) pass?.trim { it <= ' ' } else "",
            "web_validation",
            savePassword)
    }

    private fun setLoginNow(loginNow: Boolean) {
        this.loginNow = loginNow
        resolveLoadingViews()
    }

    private fun resolveLoadingViews() {
        view?.displayLoading(loginNow)
    }

    override fun onGuiCreated(viewHost: IDirectAuthView) {
        super.onGuiCreated(viewHost)
        resolveLoadingViews()
        resolveSmsRootVisibility()
        resolveAppCodeRootVisibility()
        resolveCaptchaViews()
    }

    fun fireLoginViaWebClick() {
        view?.returnLoginViaWebAction()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveButtonLoginState()
    }

    private fun resolveButtonLoginState() {
        resumedView?.setLoginButtonEnabled(
            username.trimmedNonNullNoEmpty()
                    && pass.nonNullNoEmpty()
                    && (requiredCaptcha == null || captcha.trimmedNonNullNoEmpty())
                    && (!requireSmsCode || smsCode.trimmedNonNullNoEmpty())
                    && (!requireAppCode || appCode.trimmedNonNullNoEmpty())
        )
    }

    fun fireLoginEdit(sequence: CharSequence?) {
        username = sequence.toString()
        resolveButtonLoginState()
    }

    fun firePasswordEdit(s: CharSequence?) {
        pass = s.toString()
        resolveButtonLoginState()
    }

    fun fireSmsCodeEdit(sequence: CharSequence?) {
        smsCode = sequence.toString()
        resolveButtonLoginState()
    }

    fun fireCaptchaEdit(s: CharSequence?) {
        captcha = s.toString()
        resolveButtonLoginState()
    }

    fun fireSaveEdit(isSave: Boolean) {
        savePassword = isSave
    }

    fun fireButtonSendCodeViaSmsClick() {
        doLogin(true)
    }

    fun fireAppCodeEdit(s: CharSequence?) {
        appCode = s.toString()
        resolveButtonLoginState()
    }

}