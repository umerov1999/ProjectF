package dev.ragnarok.filegallery.materialpopupmenu.builder

import android.view.View
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenu
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenuBuilder

/**
 * Builder class to create a [MaterialPopupMenu.PopupMenuCustomItem].
 */
class CustomItemBuilder(view: View) : AbstractItemBuilder<CustomItemBuilder>() {
    override val data = MaterialPopupMenuBuilder.CustomItem.Data(view)

    /**
     * When this is called, all default on-click settings normally set by the library will be disabled.
     * Specifically:
     * - The menu won't be dismissed when clicking the item.
     * - [setOnSelectListener] and [setDismissOnSelect] will have no effect.
     */
    fun disableDefaultClickHandlers() = apply { data.disableDefaultClickHandlers = true }
    override fun self() = this
    override fun build(): MaterialPopupMenu.PopupMenuCustomItem =
        MaterialPopupMenu.PopupMenuCustomItem(data, resolveOnShowCallback())
}