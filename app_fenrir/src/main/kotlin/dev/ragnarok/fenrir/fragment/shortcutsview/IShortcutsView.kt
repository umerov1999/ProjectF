package dev.ragnarok.fenrir.fragment.shortcutsview

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.ShortcutStored

interface IShortcutsView : IMvpView, IErrorView {
    fun displayData(shortcuts: List<ShortcutStored>)
    fun notifyItemRemoved(position: Int)
    fun notifyDataSetChanged()
}
