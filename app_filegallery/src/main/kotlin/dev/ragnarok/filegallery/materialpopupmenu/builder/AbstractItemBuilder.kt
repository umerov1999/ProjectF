package dev.ragnarok.filegallery.materialpopupmenu.builder

import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenu
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenuBuilder
import dev.ragnarok.filegallery.materialpopupmenu.OnShowCallback
import java.util.function.Consumer

/**
 * Base builder class.
 *
 * It's public because it's illegal for a public subclass to extend an internal superclass.
 */
sealed class AbstractItemBuilder<T : AbstractItemBuilder<T>> {
    internal abstract val data: MaterialPopupMenuBuilder.AbstractItem.Data

    @JvmField
    protected var onShowCallback: Consumer<OnShowCallback>? = null
    fun setOnSelectListener(listener: Runnable) = self().apply { data.onSelectListener = listener }
    open fun setDismissOnSelect(dismiss: Boolean) = self().apply { data.dismissOnSelect = dismiss }
    fun shouldBeHiddenIf(condition: Boolean) = self().apply { data.shouldBeHidden = condition }
    fun setOnShowCallback(callback: Consumer<OnShowCallback>) =
        self().apply { onShowCallback = callback }

    abstract fun build(): MaterialPopupMenu.AbstractPopupMenuItem
    protected open fun resolveOnShowCallback() = OnShowCallback { onShowCallback?.accept(this) }
    protected abstract fun self(): T
}