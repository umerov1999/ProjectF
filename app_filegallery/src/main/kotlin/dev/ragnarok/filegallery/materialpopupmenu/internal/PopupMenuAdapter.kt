package dev.ragnarok.filegallery.materialpopupmenu.internal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenu
import dev.ragnarok.filegallery.materialpopupmenu.internal.ui.PopupMenuCheckboxItemUi
import dev.ragnarok.filegallery.materialpopupmenu.internal.ui.PopupMenuItemUi
import dev.ragnarok.filegallery.materialpopupmenu.internal.ui.PopupMenuNavBackItemUi
import dev.ragnarok.filegallery.materialpopupmenu.internal.ui.PopupMenuRadioGroupUi
import dev.ragnarok.filegallery.materialpopupmenu.internal.ui.PopupMenuSwitchItemUi

/**
 * RecyclerView adapter used for displaying popup menu items grouped in sections.
 *
 * @author Piotr Zawadzki
 */
internal class PopupMenuAdapter(
    private val sections: List<MaterialPopupMenu.PopupMenuSection>,
    private val dismissPopupCallback: () -> Unit
) : SectionedRecyclerViewAdapter<PopupMenuAdapter.SectionHeaderViewHolder, PopupMenuAdapter.AbstractItemViewHolder>() {

    private val customItems = mutableListOf<MaterialPopupMenu.PopupMenuCustomItem>()

    init {
        setHasStableIds(false)
        for (section in sections)
            for (item in section.items)
                if (item is MaterialPopupMenu.PopupMenuCustomItem)
                    customItems.add(item)
    }

    override fun getItemCountForSection(section: Int): Int = sections[section].items.size

    override val sectionCount: Int
        get() = sections.size

    override fun onCreateSectionHeaderViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SectionHeaderViewHolder =
        SectionHeaderViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.mpm_popup_menu_section_header, parent, false)
        )

    override fun getSectionItemViewType(section: Int, position: Int): Int {
        return when (val popupMenuItem = sections[section].items[position]) {
            is MaterialPopupMenu.PopupMenuCustomItem -> customItems.indexOf(popupMenuItem)
            is MaterialPopupMenu.PopupMenuCheckboxItem -> TYPE_CHECKBOX_ITEM
            is MaterialPopupMenu.PopupMenuItem -> TYPE_ITEM
            is MaterialPopupMenu.PopupMenuNavBackItem -> TYPE_NAV_BACK_ITEM
            is MaterialPopupMenu.PopupMenuRadioGroupItem -> TYPE_RADIO_GROUP_ITEM
            is MaterialPopupMenu.PopupMenuRadioButtonItem -> throw IllegalArgumentException("Radio button items should not be added by themselves")
            is MaterialPopupMenu.PopupMenuSwitchItem -> TYPE_SWITCH_ITEM
        }
    }

    override fun onCreateItemViewHolder(parent: ViewGroup, viewType: Int): AbstractItemViewHolder {
        return when (viewType) {
            TYPE_ITEM -> ItemViewHolder(
                PopupMenuItemUi(parent.context, parent),
                dismissPopupCallback
            )

            TYPE_CHECKBOX_ITEM -> CheckboxItemViewHolder(
                PopupMenuCheckboxItemUi(
                    parent.context,
                    parent
                ), dismissPopupCallback
            )

            TYPE_NAV_BACK_ITEM -> NavBackItemViewHolder(
                PopupMenuNavBackItemUi(
                    parent.context,
                    parent
                ), dismissPopupCallback
            )

            TYPE_RADIO_GROUP_ITEM -> RadioGroupViewHolder(
                PopupMenuRadioGroupUi(parent.context),
                dismissPopupCallback
            )

            TYPE_RADIO_BUTTON_ITEM -> throw IllegalArgumentException("Radio button items should not be added by themselves")
            TYPE_SWITCH_ITEM -> SwitchItemViewHolder(
                PopupMenuSwitchItemUi(parent.context, parent),
                dismissPopupCallback
            )

            else -> CustomItemViewHolder(customItems[viewType].data.view, dismissPopupCallback)
        }
    }

    override fun onBindSectionHeaderViewHolder(
        holder: SectionHeaderViewHolder,
        sectionPosition: Int
    ) {
        val sectionHeader = sections[sectionPosition]
        val title = sectionHeader.data.title
        val titleRes = sectionHeader.data.titleRes
        when {
            title != null -> holder.label.text = title
            titleRes != 0 -> holder.label.setText(titleRes)
            else -> holder.label.visibility = View.GONE
        }
        holder.separator.visibility = if (sectionPosition == 0) View.GONE else View.VISIBLE
    }

    override fun onBindItemViewHolder(holder: AbstractItemViewHolder, section: Int, position: Int) =
        holder.bindItem(sections[section].items[position])

    internal sealed class AbstractItemViewHolder(
        itemView: View,
        private val dismissPopupCallback: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        @CallSuper
        open fun bindItem(popupMenuItem: MaterialPopupMenu.AbstractPopupMenuItem) {
            popupMenuItem.onShowCallback.dismissPopupAction = dismissPopupCallback
            popupMenuItem.onShowCallback.call()
        }
    }

    internal class ItemViewHolder(
        private val ui: PopupMenuItemUi,
        dismissPopupCallback: () -> Unit
    ) : AbstractItemViewHolder(ui.root, dismissPopupCallback) {
        override fun bindItem(popupMenuItem: MaterialPopupMenu.AbstractPopupMenuItem) {
            super.bindItem(popupMenuItem)
            ui.bind(popupMenuItem as MaterialPopupMenu.PopupMenuItem)
        }
    }

    internal class CheckboxItemViewHolder(
        private val ui: PopupMenuCheckboxItemUi,
        dismissPopupCallback: () -> Unit
    ) : AbstractItemViewHolder(ui.root, dismissPopupCallback) {
        override fun bindItem(popupMenuItem: MaterialPopupMenu.AbstractPopupMenuItem) {
            super.bindItem(popupMenuItem)
            ui.bind(popupMenuItem as MaterialPopupMenu.PopupMenuCheckboxItem)
        }
    }

    internal class SwitchItemViewHolder(
        private val ui: PopupMenuSwitchItemUi,
        dismissPopupCallback: () -> Unit
    ) : AbstractItemViewHolder(ui.root, dismissPopupCallback) {
        override fun bindItem(popupMenuItem: MaterialPopupMenu.AbstractPopupMenuItem) {
            super.bindItem(popupMenuItem)
            ui.bind(popupMenuItem as MaterialPopupMenu.PopupMenuSwitchItem)
        }
    }

    internal class NavBackItemViewHolder(
        private val ui: PopupMenuNavBackItemUi,
        dismissPopupCallback: () -> Unit
    ) : AbstractItemViewHolder(ui.root, dismissPopupCallback) {
        override fun bindItem(popupMenuItem: MaterialPopupMenu.AbstractPopupMenuItem) {
            super.bindItem(popupMenuItem)
            ui.bind(popupMenuItem as MaterialPopupMenu.PopupMenuNavBackItem)
        }
    }

    internal class CustomItemViewHolder(
        itemView: View,
        dismissPopupCallback: () -> Unit
    ) : AbstractItemViewHolder(itemView, dismissPopupCallback) {
        override fun bindItem(popupMenuItem: MaterialPopupMenu.AbstractPopupMenuItem) {
            super.bindItem(popupMenuItem)
            val item = popupMenuItem as MaterialPopupMenu.PopupMenuCustomItem
            if (item.data.disableDefaultClickHandlers.not())
                item.data.view.setOnClickListener {
                    item.data.onSelectListener?.run()
                    item.dismissMenuIfAllowed()
                }
        }
    }

    internal class RadioGroupViewHolder(
        private val ui: PopupMenuRadioGroupUi,
        private val dismissPopupCallback: () -> Unit
    ) : AbstractItemViewHolder(ui.root, dismissPopupCallback) {
        override fun bindItem(popupMenuItem: MaterialPopupMenu.AbstractPopupMenuItem) {
            super.bindItem(popupMenuItem)
            val item = popupMenuItem as MaterialPopupMenu.PopupMenuRadioGroupItem
            ui.bindItems(item.data.radioButtonItems, dismissPopupCallback)
        }
    }

    internal class SectionHeaderViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        val label: MaterialTextView = root.findViewById(R.id.mpm_popup_menu_section_header_label)
        val separator: View = root.findViewById(R.id.mpm_popup_menu_section_separator)
    }
}
