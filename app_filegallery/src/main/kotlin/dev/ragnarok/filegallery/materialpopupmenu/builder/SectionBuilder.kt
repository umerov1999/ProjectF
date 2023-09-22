package dev.ragnarok.filegallery.materialpopupmenu.builder

import androidx.annotation.StringRes
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenu
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenuBuilder

class SectionBuilder {
    private val data = MaterialPopupMenuBuilder.Section.Data()
    private val itemList = mutableListOf<MaterialPopupMenu.AbstractPopupMenuItem>()
    fun setTitle(title: CharSequence) = apply { data.title = title }
    fun setTitle(@StringRes titleRes: Int) = apply { data.titleRes = titleRes }
    fun shouldBeHiddenIf(condition: Boolean) = apply { data.shouldBeHidden = condition }

    /**
     * Use the [ItemBuilder] class to build the item.
     */
    fun addItem(item: MaterialPopupMenu.PopupMenuItem) = apply { itemList.add(item) }

    /**
     * Use the [CheckboxItemBuilder] class to build the item.
     */
    fun addCheckboxItem(item: MaterialPopupMenu.PopupMenuCheckboxItem) =
        apply { itemList.add(item) }

    /**
     * Use the [CustomItemBuilder] class to build the item.
     */
    fun addCustomItem(item: MaterialPopupMenu.PopupMenuCustomItem) = apply { itemList.add(item) }

    /**
     * Use the [RadioGroupItemBuilder] class to build the item.
     */
    fun addRadioGroupItem(item: MaterialPopupMenu.PopupMenuRadioGroupItem) =
        apply { itemList.add(item) }

    /**
     * Use the [SwitchItemBuilder] class to build the item.
     */
    fun addSwitchItem(item: MaterialPopupMenu.PopupMenuSwitchItem) = apply { itemList.add(item) }
    internal fun addNavBackItem(item: MaterialPopupMenu.PopupMenuNavBackItem) =
        apply { itemList.add(item) }

    fun build(): MaterialPopupMenu.PopupMenuSection {
        require(itemList.isNotEmpty()) { "Section has no items!" }
        return MaterialPopupMenu.PopupMenuSection(data, itemList.filter { !it.data.shouldBeHidden })
    }
}