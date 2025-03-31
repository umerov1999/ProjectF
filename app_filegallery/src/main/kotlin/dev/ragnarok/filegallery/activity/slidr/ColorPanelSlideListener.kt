package dev.ragnarok.filegallery.activity.slidr

import android.animation.ArgbEvaluator
import android.app.Activity
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
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
    private val statusBarNonColored: Int =
        if (Utils.hasVanillaIceCreamTarget()) Color.BLACK else getStatusBarNonColored(activity)

    @ColorInt
    private val statusBarColored: Int =
        if (Utils.hasVanillaIceCreamTarget()) Color.WHITE else getStatusBarColor(activity)

    @ColorInt
    private val navigationBarNonColored: Int = Color.BLACK

    @ColorInt
    private val navigationBarColored: Int =
        if (Utils.hasVanillaIceCreamTarget()) Color.WHITE else getNavigationBarColor(activity)

    override fun onStateChanged(state: Int) {
        // Unused.
    }

    override fun onClosed() {
        Utils.finishActivityImmediate(activity)
    }

    override fun onOpened() {
        // Unused.
    }

    private fun isDark(@ColorInt color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    override fun onSlideChange(percent: Float) {
        try {
            if (isFromUnColoredToColoredStatusBar) {
                val w = activity.window
                if (w != null) {
                    val invertIcons: Boolean
                    if (Utils.hasVanillaIceCreamTarget()) {
                        val statusColor =
                            evaluator.evaluate(percent, Color.WHITE, Color.BLACK) as Int
                        invertIcons = !isDark(statusColor)
                    } else {
                        val statusColor = evaluator.evaluate(
                            percent,
                            statusBarColored,
                            statusBarNonColored
                        ) as Int
                        val navigationColor = evaluator.evaluate(
                            percent,
                            navigationBarColored,
                            navigationBarNonColored
                        ) as Int
                        @Suppress("deprecation")
                        w.statusBarColor = statusColor
                        @Suppress("deprecation")
                        w.navigationBarColor = navigationColor
                        invertIcons = !isDark(statusColor)
                    }
                    val ins = WindowInsetsControllerCompat(w, w.decorView)
                    ins.isAppearanceLightStatusBars = invertIcons
                    ins.isAppearanceLightNavigationBars = invertIcons
                }
            }
            if (isUseAlpha) {
                activity.window.decorView.rootView.alpha = Utils.clamp(percent, 0f, 1f)
            }
        } catch (_: Exception) {
        }
    }

}