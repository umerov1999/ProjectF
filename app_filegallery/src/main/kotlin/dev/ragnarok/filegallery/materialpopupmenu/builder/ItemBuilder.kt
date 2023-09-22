package dev.ragnarok.filegallery.materialpopupmenu.builder

import androidx.annotation.StringRes
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenu
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenuBuilder

/**
 * Builder class to create a [MaterialPopupMenu.PopupMenuItem].
 */
class ItemBuilder private constructor(override val data: MaterialPopupMenuBuilder.Item.Data) :
    NormalItemBuilder<ItemBuilder>() {
    constructor(label: CharSequence) : this(MaterialPopupMenuBuilder.Item.Data(label))
    constructor(@StringRes labelRes: Int) : this(MaterialPopupMenuBuilder.Item.Data(labelRes))

    /**
     * Set another menu that will be opened when this item is clicked. This item will
     * have a "nested" icon shown at the end.
     *
     * The menu will have a "go back" item automatically "injected" to
     * the top, which brings you back to this menu when clicked.
     *
     * **Note:** [setDismissOnSelect] will have no effect.
     */
    @JvmOverloads
    fun setSubMenu(
        menu: MaterialPopupMenu,
        navBackItem: MaterialPopupMenu.PopupMenuNavBackItem = NavBackItemBuilder().build()
    ) = apply { data.subMenu = menu.setIsSubMenu(navBackItem) }

    override fun self() = this
    override fun build(): MaterialPopupMenu.PopupMenuItem =
        MaterialPopupMenu.PopupMenuItem(data, resolveOnShowCallback())
}