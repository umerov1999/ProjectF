package dev.ragnarok.fenrir.dialog.directauth

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView

interface IDirectAuthView : IMvpView, IErrorView {
    fun setLoginButtonEnabled(enabled: Boolean)
    fun setSmsRootVisible(visible: Boolean)
    fun setAppCodeRootVisible(visible: Boolean)
    fun moveFocusToSmsCode()
    fun moveFocusToAppCode()
    fun displayLoading(loading: Boolean)
    fun setCaptchaRootVisible(visible: Boolean)
    fun displayCaptchaImage(img: String?)
    fun moveFocusToCaptcha()
    fun hideKeyboard()
    fun returnSuccessToParent(
        userId: Long,
        accessToken: String?,
        Login: String?,
        Password: String?,
        twoFA: String?,
        isSave: Boolean
    )

    fun returnSuccessValidation(
        url: String?,
        Login: String?,
        Password: String?,
        twoFA: String?,
        isSave: Boolean
    )

    fun returnLoginViaWebAction()
}