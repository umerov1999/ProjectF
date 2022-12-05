package dev.ragnarok.filegallery.activity.slidr

import android.view.View
import androidx.fragment.app.FragmentActivity
import dev.ragnarok.filegallery.activity.slidr.model.SlidrConfig
import dev.ragnarok.filegallery.activity.slidr.widget.SliderPanel.OnPanelSlideListener

internal class FragmentPanelSlideListener(private val view: View, private val config: SlidrConfig) :
    OnPanelSlideListener {
    override fun onStateChanged(state: Int) {
        config.listener?.onSlideStateChanged(state)
    }

    override fun onClosed() {
        if (config.listener?.onSlideClosed() == true) {
            return
        }

        // Ensure that we are attached to a FragmentActivity
        if (view.context is FragmentActivity) {
            val activity = view.context as FragmentActivity
            if (activity.supportFragmentManager.backStackEntryCount == 0) {
                activity.finish()
                activity.overridePendingTransition(0, 0)
            } else {
                activity.supportFragmentManager.popBackStack()
            }
        }
    }

    override fun onOpened() {
        config.listener?.onSlideOpened()
    }

    override fun onSlideChange(percent: Float) {
        config.listener?.onSlideChange(percent)
    }
}