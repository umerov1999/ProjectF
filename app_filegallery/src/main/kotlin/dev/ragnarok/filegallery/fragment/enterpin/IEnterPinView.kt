package dev.ragnarok.filegallery.fragment.enterpin

import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.fragment.base.core.IToastView

interface IEnterPinView : IMvpView, IErrorView, IToastView {
    fun displayPin(value: IntArray, noValue: Int)
    fun sendSuccessAndClose()
    fun displayErrorAnimation()
    fun showBiometricPrompt()
}