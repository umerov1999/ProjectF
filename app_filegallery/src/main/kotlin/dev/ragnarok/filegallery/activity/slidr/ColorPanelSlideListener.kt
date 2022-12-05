package dev.ragnarok.filegallery.activity.slidr

import android.animation.ArgbEvaluator
import android.app.Activity
import android.graphics.Color
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import dev.ragnarok.filegallery.activity.slidr.widget.SliderPanel.OnPanelSlideListener
import dev.ragnarok.filegallery.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.filegallery.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.filegallery.settings.CurrentTheme.getStatusBarNonColored
import dev.ragnarok.filegallery.util.Utils

internal open class ColorPanelSlideListener(
    private val activity: Activity,
    private val isFromUnColoredToColoredStatusBar: Boolean,
    private val isUseAlpha: Boolean
) : OnPanelSlideListener {
    private val evaluator = ArgbEvaluator()

    @ColorInt
    private val statusBarNonColored: Int = getStatusBarNonColored(activity)

    @ColorInt
    private val statusBarColored: Int = getStatusBarColor(activity)

    @ColorInt
    private val navigationBarNonColored: Int = Color.BLACK

    @ColorInt
    private val navigationBarColored: Int = getNavigationBarColor(activity)
    override fun onStateChanged(state: Int) {
        // Unused.
    }

    override fun onClosed() {
        activity.finish()
        activity.overridePendingTransition(0, 0)
    }

    override fun onOpened() {
        // Unused.
    }

    private fun isDark(@ColorInt color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    @Suppress("DEPRECATION")
    override fun onSlideChange(percent: Float) {
        try {
            if (isFromUnColoredToColoredStatusBar) {
                val statusColor =
                    evaluator.evaluate(percent, statusBarColored, statusBarNonColored) as Int
                val navigationColor = evaluator.evaluate(
                    percent,
                    navigationBarColored,
                    navigationBarNonColored
                ) as Int
                val w = activity.window
                if (w != null) {
                    w.statusBarColor = statusColor
                    w.navigationBarColor = navigationColor
                    val invertIcons = !isDark(statusColor)
                    if (Utils.hasMarshmallow()) {
                        var flags = w.decorView.systemUiVisibility
                        flags = if (invertIcons) {
                            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        } else {
                            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                        }
                        w.decorView.systemUiVisibility = flags
                    }
                    if (Utils.hasOreo()) {
                        var flags = w.decorView.systemUiVisibility
                        flags = if (invertIcons) {
                            flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                        } else {
                            flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                        }
                        w.decorView.systemUiVisibility = flags
                    }
                }
            }
            if (isUseAlpha) {
                activity.window.decorView.rootView.alpha = Utils.clamp(percent, 0f, 1f)
            }
        } catch (ignored: Exception) {
        }
    }

}