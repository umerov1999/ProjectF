package dev.ragnarok.filegallery.materialpopupmenu.internal

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.PopupWindow
import androidx.core.view.doOnLayout
import dev.ragnarok.filegallery.materialpopupmenu.PopupAnimation

internal class MaterialPopupWindow(
    context: Context,
    private val customAnimation: PopupAnimation?
) : PopupWindow(context, null, 0) {

    override fun showAsDropDown(anchor: View?) {
        prepareAnimation()
        super.showAsDropDown(anchor)
        startAnimation()
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int) {
        prepareAnimation()
        super.showAsDropDown(anchor, xoff, yoff)
        startAnimation()
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        prepareAnimation()
        super.showAsDropDown(anchor, xoff, yoff, gravity)
        startAnimation()
    }

    override fun dismiss() {
        if (customAnimation != null) {
            customAnimation.onHide(this) { super.dismiss() }
        } else {
            super.dismiss()
        }
        contentView = null
    }

    private fun prepareAnimation() = customAnimation?.let {
        disableAnimations()
        it.onPrepare(this)
    }

    private fun startAnimation() = customAnimation?.let { anim ->
        disableAnimations()
        anim.onPrepare(this)
        contentView?.doOnLayout { anim.onShow(this) }
    }

    private fun disableAnimations() {
        animationStyle = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            enterTransition = null
            exitTransition = null
        }
    }
}
