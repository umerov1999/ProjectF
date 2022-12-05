package dev.ragnarok.filegallery.activity.slidr

import android.app.Activity
import dev.ragnarok.filegallery.activity.slidr.model.SlidrConfig

internal class ConfigPanelSlideListener(activity: Activity, private val config: SlidrConfig) :
    ColorPanelSlideListener(activity, false, true) {
    override fun onStateChanged(state: Int) {
        config.listener?.onSlideStateChanged(state)
    }

    override fun onClosed() {
        if (config.listener?.onSlideClosed() == true) {
            return
        }
        super.onClosed()
    }

    override fun onOpened() {
        config.listener?.onSlideOpened()
    }

    override fun onSlideChange(percent: Float) {
        super.onSlideChange(percent)
        config.listener?.onSlideChange(percent)
    }

    val isFromUnColoredToColoredStatusBar: Boolean
        get() = config.isFromUnColoredToColoredStatusBar()
    val isUseAlpha: Boolean
        get() = config.isAlphaForView()
}