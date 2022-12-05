package dev.ragnarok.fenrir.fragment.addproxy

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView

interface IAddProxyView : IMvpView, IErrorView {
    fun setAuthFieldsEnabled(enabled: Boolean)
    fun setAuthChecked(checked: Boolean)
    fun goBack()
}