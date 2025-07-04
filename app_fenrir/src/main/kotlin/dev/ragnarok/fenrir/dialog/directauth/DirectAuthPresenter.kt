package dev.ragnarok.fenrir.dialog.directauth

import android.os.Bundle
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.Auth.scope
import dev.ragnarok.fenrir.api.CaptchaNeedException
import dev.ragnarok.fenrir.api.NeedValidationException
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.response.LoginResponse
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.model.Captcha
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.service.ApiErrorCodes
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayedFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

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
        val trimmedUsername = if (username.nonNullNoEmpty()) username?.trim() else ""
        val trimmedPass = if (pass.nonNullNoEmpty()) pass?.trim() else ""
        val captchaSid = if (requiredCaptcha != null) requiredCaptcha?.sid else null
        val captchaCode = if (captcha.nonNullNoEmpty()) captcha?.trim() else null
        val code: String? = if (requireSmsCode) {
            if (smsCode.nonNullNoEmpty()) smsCode?.trim() else null
        } else if (requireAppCode) {
            if (appCode.nonNullNoEmpty()) appCode?.trim() else null
        } else {
            null
        }
        setLoginNow(true)
        appendJob(
            networker.vkDirectAuth()
                .directLogin(
                    "password",
                    Constants.API_ID,
                    Constants.SECRET,
                    trimmedUsername,
                    trimmedPass,
                    Constants.AUTH_API_VERSION,
                    Constants.DEFAULT_ACCOUNT_TYPE == AccountType.VK_ANDROID,
                    scope,
                    code,
                    captchaSid,
                    captchaCode,
                    forceSms,
                    Constants.DEFAULT_ACCOUNT_TYPE == AccountType.VK_ANDROID
                )
                .fromIOToMain({ response -> onLoginResponse(response) }) { t ->
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
                val phone = t.phone
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
                    appendJob(
                        networker.vkAuth()
                            .validatePhone(
                                phone,
                                Constants.API_ID,
                                Constants.API_ID,
                                Constants.SECRET,
                                sid,
                                Constants.AUTH_API_VERSION,
                                libverify_support = true,
                                allow_callreset = true
                            )
                            .delayedFlow(1000)
                            .fromIOToMain({ }) {
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
            val Pass = if (pass.nonNullNoEmpty()) pass?.trim() else ""
            if (requireSmsCode) TwFa = "2fa_sms" else if (requireAppCode) TwFa = "2fa_app"
            val TwFafin = TwFa
            view?.returnSuccessToParent(
                response.user_id,
                response.access_token,
                if (username.nonNullNoEmpty()) username?.trim() else "",
                Pass,
                TwFafin,
                savePassword
            )
        } else if (response.errorBasic != null && response.errorBasic?.errorCode == ApiErrorCodes.VALIDATE_NEED) {
            view?.startDefaultValidation(response.errorBasic?.redirectUri)
        } else if (response.errorBasic != null) {
            response.errorBasic?.let {
                view?.showThrowable(ApiException(it))
            }
        }
    }

    private fun onValidate() {
        view?.returnSuccessValidation(
            RedirectUrl,
            if (username.nonNullNoEmpty()) username?.trim() else "",
            if (pass.nonNullNoEmpty()) pass?.trim() else "",
            "web_validation",
            savePassword
        )
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

    override fun onGuiResumed() {
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