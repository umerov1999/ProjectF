package dev.ragnarok.fenrir.materialpopupmenu.builder

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.materialpopupmenu.MaterialPopupMenu
import dev.ragnarok.fenrir.materialpopupmenu.MaterialPopupMenuBuilder

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