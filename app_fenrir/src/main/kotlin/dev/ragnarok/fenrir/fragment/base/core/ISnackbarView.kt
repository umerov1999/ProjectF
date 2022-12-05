package dev.ragnarok.fenrir.fragment.base.core

import androidx.annotation.StringRes

interface ISnackbarView {
    fun showSnackbar(@StringRes res: Int, isLong: Boolean)
}