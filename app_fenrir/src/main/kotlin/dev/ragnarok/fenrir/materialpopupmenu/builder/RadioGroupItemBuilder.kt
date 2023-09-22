package dev.ragnarok.fenrir.materialpopupmenu.builder

import dev.ragnarok.fenrir.materialpopupmenu.MaterialPopupMenu
import dev.ragnarok.fenrir.materialpopupmenu.MaterialPopupMenuBuilder

class RadioGroupItemBuilder : AbstractItemBuilder<RadioGroupItemBuilder>() {
    override val data = MaterialPopupMenuBuilder.RadioGroupItem.Data()
    fun addItem(item: MaterialPopupMenu.PopupMenuRadioButtonItem) =
        apply { data.radioButtonItems.add(item) }

    override fun self() = this
    override fun build(): MaterialPopupMenu.PopupMenuRadioGroupItem {
        require(data.radioButtonItems.size >= 2) { "Radio groups must have 2 or more items" }
        return MaterialPopupMenu.PopupMenuRadioGroupItem(data, resolveOnShowCallback())
    }
}