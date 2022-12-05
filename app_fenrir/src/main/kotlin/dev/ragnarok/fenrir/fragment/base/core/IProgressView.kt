package dev.ragnarok.fenrir.fragment.base.core

import androidx.annotation.StringRes

interface IProgressView {
    fun displayProgressDialog(@StringRes title: Int, @StringRes message: Int, cancelable: Boolean)
    fun dismissProgressDialog()
}