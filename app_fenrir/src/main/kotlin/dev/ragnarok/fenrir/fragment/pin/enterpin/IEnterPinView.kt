package dev.ragnarok.fenrir.fragment.pin.enterpin

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView

interface IEnterPinView : IMvpView, IErrorView, IToastView {
    fun displayPin(value: IntArray, noValue: Int)
    fun sendSuccessAndClose()
    fun displayErrorAnimation()
    fun displayAvatarFromUrl(url: String?)
    fun displayDefaultAvatar()
    fun showBiometricPrompt()
}