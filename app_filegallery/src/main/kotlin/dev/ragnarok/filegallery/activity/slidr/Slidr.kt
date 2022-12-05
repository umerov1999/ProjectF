package dev.ragnarok.filegallery.activity.slidr

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.activity.slidr.model.SlidrConfig
import dev.ragnarok.filegallery.activity.slidr.model.SlidrInterface
import dev.ragnarok.filegallery.activity.slidr.widget.SliderPanel

/**
 * This attacher class is used to attach the sliding mechanism to any [android.app.Activity]
 * that lets the user slide (or swipe) the activity away as a form of back or up action. The action
 * causes [android.app.Activity.finish] to be called.
 */
object Slidr {
    /**
     * Attach a slideable mechanism to an activity that adds the slide to dismiss functionality
     *
     * @param activity the activity to attach the slider to
     * @return a [dev.ragnarok.filegallery.activity.slidr.model.SlidrInterface] that allows
     * the user to lock/unlock the sliding mechanism for whatever purpose.
     */
    fun attach(
        activity: Activity,
        fromUnColoredToColoredStatusBar: Boolean = false,
        useAlpha: Boolean = true
    ): SlidrInterface {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.setTranslucent(true)
        }
        activity.window.setBackgroundDrawableResource(R.color.transparent)

        // Setup the slider panel and attach it to the decor
        val panel = attachSliderPanel(activity, null)

        // Set the panel slide listener for when it becomes closed or opened
        panel.setOnPanelSlideListener(
            ColorPanelSlideListener(
                activity,
                fromUnColoredToColoredStatusBar,
                useAlpha
            )
        )

        // Return the lock interface
        return panel.defaultInterface
    }

    /**
     * Attach a slider mechanism to an activity based on the passed [dev.ragnarok.filegallery.activity.slidr.model.SlidrConfig]
     *
     * @param activity the activity to attach the slider to
     * @param config   the slider configuration to make
     * @return a [dev.ragnarok.filegallery.activity.slidr.model.SlidrInterface] that allows
     * the user to lock/unlock the sliding mechanism for whatever purpose.
     */
    fun attach(activity: Activity, config: SlidrConfig): SlidrInterface {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.setTranslucent(true)
        }
        activity.window.setBackgroundDrawableResource(R.color.transparent)
        // Setup the slider panel and attach it to the decor
        val panel = attachSliderPanel(activity, config)

        // Set the panel slide listener for when it becomes closed or opened
        panel.setOnPanelSlideListener(ConfigPanelSlideListener(activity, config))

        // Return the lock interface
        return panel.defaultInterface
    }

    /**
     * Attach a new [SliderPanel] to the root of the activity's content
     */
    private fun attachSliderPanel(activity: Activity, config: SlidrConfig?): SliderPanel {
        // Hijack the decorview
        val decorView = activity.window.decorView as ViewGroup
        val oldScreen = decorView.getChildAt(0)
        decorView.removeViewAt(0)

        // Setup the slider panel and attach it to the decor
        val panel = SliderPanel(activity, oldScreen, config)
        panel.id = R.id.slidable_panel
        oldScreen.id = R.id.slidable_content
        panel.addView(oldScreen)
        decorView.addView(panel, 0)
        return panel
    }

    /**
     * Attach a slider mechanism to a fragment view replacing an internal view
     *
     * @param oldScreen the view within a fragment to replace
     * @param config    the slider configuration to attach with
     * @return a [dev.ragnarok.filegallery.activity.slidr.model.SlidrInterface] that allows
     * the user to lock/unlock the sliding mechanism for whatever purpose.
     */
    fun replace(oldScreen: View, config: SlidrConfig): SlidrInterface {
        val parent = oldScreen.parent as ViewGroup
        val params = oldScreen.layoutParams
        parent.removeView(oldScreen)

        // Setup the slider panel and attach it
        val panel = SliderPanel(oldScreen.context, oldScreen, config)
        panel.id = R.id.slidable_panel
        oldScreen.id = R.id.slidable_content
        panel.addView(oldScreen)
        parent.addView(panel, 0, params)

        // Set the panel slide listener for when it becomes closed or opened
        panel.setOnPanelSlideListener(FragmentPanelSlideListener(oldScreen, config))

        // Return the lock interface
        return panel.defaultInterface
    }
}