package dev.ragnarok.filegallery.materialpopupmenu.internal.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenu

internal class PopupMenuSwitchItemUi(context: Context, parent: ViewGroup) {
    val root: View =
        LayoutInflater.from(context).inflate(R.layout.mpm_popup_menu_switch_item, parent, false)
    private val icon: AppCompatImageView = root.findViewById(R.id.mpm_popup_menu_item_icon)
    private val label: MaterialTextView = root.findViewById(R.id.mpm_popup_menu_item_label)
    private val switch: MaterialSwitch = root.findViewById(R.id.mpm_popup_menu_item_switch)

    fun bind(item: MaterialPopupMenu.PopupMenuSwitchItem) {
        val data = item.data
        item.bindToViews(icon, label, switch)
        root.setOnClickListener {
            data.onSelectListener?.run()
            if (data.isItemEligibleForToggling(switch))
                switch.toggle()
            item.dismissMenuIfAllowed()
        }
    }
}
