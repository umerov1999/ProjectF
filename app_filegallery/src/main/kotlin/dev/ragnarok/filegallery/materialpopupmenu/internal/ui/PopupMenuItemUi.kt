package dev.ragnarok.filegallery.materialpopupmenu.internal.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.textview.MaterialTextView
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenu
import dev.ragnarok.filegallery.materialpopupmenu.internal.MenuNavStack

internal class PopupMenuItemUi(context: Context, parent: ViewGroup) {
    val root: View =
        LayoutInflater.from(context).inflate(R.layout.mpm_popup_menu_item, parent, false)
    private val icon: AppCompatImageView = root.findViewById(R.id.mpm_popup_menu_item_icon)
    private val label: MaterialTextView = root.findViewById(R.id.mpm_popup_menu_item_label)
    private val nestedIcon: AppCompatImageView =
        root.findViewById(R.id.mpm_popup_menu_item_nested_icon)

    fun bind(item: MaterialPopupMenu.PopupMenuItem) {
        val data = item.data
        val menu = data.subMenu
        item.bindToViews(icon, label)
        if (menu != null) {
            nestedIcon.visibility = View.VISIBLE
            root.setOnClickListener {
                data.onSelectListener?.run()
                MenuNavStack.navigateForward(menu)
            }
        } else {
            root.setOnClickListener {
                data.onSelectListener?.run()
                item.dismissMenuIfAllowed()
            }
        }
    }
}
