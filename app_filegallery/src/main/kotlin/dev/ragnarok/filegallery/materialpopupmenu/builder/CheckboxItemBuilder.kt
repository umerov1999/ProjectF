package dev.ragnarok.filegallery.materialpopupmenu.builder

import androidx.annotation.StringRes
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenu
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenuBuilder

/**
 * Builder class to create a [MaterialPopupMenu.PopupMenuCheckboxItem].
 */
class CheckboxItemBuilder private constructor(override val data: MaterialPopupMenuBuilder.CheckboxItem.Data) :
    ToggleItemBuilder<CheckboxItemBuilder>() {
    constructor(label: CharSequence) : this(MaterialPopupMenuBuilder.CheckboxItem.Data(label))
    constructor(@StringRes labelRes: Int) : this(MaterialPopupMenuBuilder.CheckboxItem.Data(labelRes))

    override fun self() = this
    override fun build(): MaterialPopupMenu.PopupMenuCheckboxItem =
        MaterialPopupMenu.PopupMenuCheckboxItem(data, resolveOnShowCallback())
}