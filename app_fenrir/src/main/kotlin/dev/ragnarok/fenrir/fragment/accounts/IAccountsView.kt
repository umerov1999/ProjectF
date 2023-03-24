package dev.ragnarok.fenrir.fragment.accounts

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Account

interface IAccountsView : IMvpView, IErrorView, IToastView {
    fun displayData(accounts: List<Account>)
    fun isLoading(loading: Boolean)
    fun resolveEmptyText(isEmpty: Boolean)
    fun invalidateMenu()
    fun startDirectLogin()

    fun notifyDataSetChanged()
    fun notifyItemRemoved(position: Int)
    fun notifyItemChanged(position: Int)
    fun notifyItemRangeChanged(positionStart: Int, count: Int)
    fun notifyItemRangeRemoved(positionStart: Int, count: Int)
    fun notifyItemRangeInserted(positionStart: Int, count: Int)
    fun showColoredSnack(text: String?, @ColorInt color: Int)
    fun showColoredSnack(@StringRes resId: Int, @ColorInt color: Int, vararg params: Any?)
}