package dev.ragnarok.fenrir.materialpopupmenu

/**
 * Callback to be invoked once an item gets shown.
 */
class OnShowCallback internal constructor(
    private val callback: (OnShowCallback.() -> Unit)?
) {

    internal var dismissPopupAction: () -> Unit = {
        throw IllegalStateException("Dismiss popup action has not been initialized. Make sure that you invoke dismissPopup function only after the popup has been shown.")
    }

    /**
     * Dismisses the shown popup.
     */
    fun dismissPopup() = dismissPopupAction()

    internal fun call() {
        callback?.invoke(this)
    }

}
