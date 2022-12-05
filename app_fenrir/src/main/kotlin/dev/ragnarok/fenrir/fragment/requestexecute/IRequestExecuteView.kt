package dev.ragnarok.fenrir.fragment.requestexecute

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IProgressView
import dev.ragnarok.fenrir.fragment.base.core.IToastView

interface IRequestExecuteView : IMvpView, IErrorView, IProgressView,
    IToastView {
    fun displayBody(body: String?)
    fun hideKeyboard()
    fun requestWriteExternalStoragePermission()
}