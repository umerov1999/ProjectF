package dev.ragnarok.fenrir.activity.slidr

import android.animation.ArgbEvaluator
import android.app.Activity
import android.graphics.Color
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import dev.ragnarok.fenrir.activity.slidr.widget.SliderPanel.OnPanelSlideListener
import dev.ragnarok.fenrir.settings.CurrentTheme.getNavigationBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarColor
import dev.ragnarok.fenrir.settings.CurrentTheme.getStatusBarNonColored
import dev.ragnarok.fenrir.util.Utils

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
        Utils.finishActivityImmediate(activity)
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

                    val ins = WindowInsetsControllerCompat(w, w.decorView)
                    ins.isAppearanceLightStatusBars = invertIcons
                    ins.isAppearanceLightNavigationBars = invertIcons

                    if (!Utils.hasMarshmallow()) {
                        w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
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