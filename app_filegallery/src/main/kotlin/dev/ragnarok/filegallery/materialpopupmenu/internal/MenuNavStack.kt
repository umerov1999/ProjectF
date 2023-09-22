package dev.ragnarok.filegallery.materialpopupmenu.internal

import dev.ragnarok.filegallery.materialpopupmenu.MaterialPopupMenu

internal object MenuNavStack {

    private val stack = ArrayDeque<MaterialPopupMenu>()

    /**
     * Only takes effect if [menu] is the root menu.
     */
    fun init(menu: MaterialPopupMenu) {
        if (menu.isRootMenu()) {
            stack.clear()
            stack.addLast(menu)
        }
    }

    fun navigateBackwards() {
        stack.removeLast().dismiss()
        stack.last().show()
    }

    fun navigateForward(menu: MaterialPopupMenu) {
        stack.last().dismiss()
        stack.addLast(menu)
        menu.show()
    }

}