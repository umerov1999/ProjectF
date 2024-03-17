package dev.ragnarok.fenrir.fragment.accounts.processauthcode

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.menu.AdvancedItem

interface IProcessAuthCodeView : IMvpView, IErrorView {
    fun displayData(shortcuts: List<AdvancedItem>)
    fun notifyItemRemoved(position: Int)
    fun notifyDataSetChanged()
    fun success()
}
