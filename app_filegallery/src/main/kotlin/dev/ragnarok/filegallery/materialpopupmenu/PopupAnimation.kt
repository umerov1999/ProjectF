package dev.ragnarok.filegallery.materialpopupmenu

import android.view.View
import android.widget.PopupWindow

interface PopupAnimation {

    /**
     * Called when the menu is about to be shown.
     */
    fun onPrepare(popup: PopupWindow) {
        popup.contentView.visibility = View.INVISIBLE
    }

    /**
     * Called when the menu is shown.
     */
    fun onShow(popup: PopupWindow)

    /**
     * Called when the menu is dismissed.
     *
     * For now [onHidden] just dismisses the popup.
     */
    fun onHide(popup: PopupWindow, onHidden: () -> Unit)
}