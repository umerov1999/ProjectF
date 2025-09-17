package dev.ragnarok.filegallery.util.toast

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dev.ragnarok.filegallery.Includes
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.settings.CurrentTheme
import dev.ragnarok.filegallery.toColor
import dev.ragnarok.filegallery.util.ErrorLocalizer
import dev.ragnarok.filegallery.util.Utils
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class CustomSnackbars private constructor(private val view: View, private var anchorView: View?) :
    AbsCustomToast {
    @BaseTransientBottomBar.Duration
    private var duration: Int = BaseTransientBottomBar.LENGTH_SHORT
    fun setDurationSnack(@BaseTransientBottomBar.Duration duration: Int): CustomSnackbars {
        this.duration = duration
        return this
    }

    override fun setAnchorView(anchorView: View?): CustomSnackbars {
        this.anchorView = anchorView
        return this
    }

    fun defaultSnack(
        @StringRes resId: Int, top: Boolean, vararg params: Any?
    ): Snackbar {
        return defaultSnack(view.resources.getString(resId, params), top)
    }

    fun defaultSnack(text: String?, top: Boolean): Snackbar {
        val ret = Snackbar.make(view, text.orEmpty(), duration).setAnchorView(anchorView)

        if (top) {
            val view: View = ret.getView()
            when (view.layoutParams) {
                is FrameLayout.LayoutParams -> {
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    params.topMargin += Utils.dp(50f)
                    view.setLayoutParams(params)
                }

                is CoordinatorLayout.LayoutParams -> {
                    val params = view.layoutParams as CoordinatorLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    params.topMargin += Utils.dp(50f)
                    view.setLayoutParams(params)
                }

                is LinearLayout.LayoutParams -> {
                    val params = view.layoutParams as LinearLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    params.topMargin += Utils.dp(50f)
                    view.setLayoutParams(params)
                }

                is RelativeLayout.LayoutParams -> {
                    val params = view.layoutParams as LinearLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    params.topMargin += Utils.dp(50f)
                    view.setLayoutParams(params)
                }
            }
        }

        return ret.setOnClickListener { ret.dismiss() }
    }

    fun coloredSnack(
        @StringRes resId: Int,
        @ColorInt color: Int, top: Boolean, vararg params: Any?
    ): Snackbar {
        return coloredSnack(view.resources.getString(resId, params), color, top)
    }

    fun coloredSnack(
        text: String?,
        @ColorInt color: Int,
        top: Boolean
    ): Snackbar {
        val text_color =
            if (isColorDark(color)) "#ffffff".toColor() else "#000000".toColor()
        val ret = Snackbar.make(view, text.orEmpty(), duration).setBackgroundTint(color)
            .setActionTextColor(text_color).setTextColor(text_color).setAnchorView(anchorView)

        if (top) {
            val view: View = ret.getView()
            when (view.layoutParams) {
                is FrameLayout.LayoutParams -> {
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    params.topMargin += Utils.dp(50f)
                    view.setLayoutParams(params)
                }

                is CoordinatorLayout.LayoutParams -> {
                    val params = view.layoutParams as CoordinatorLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    params.topMargin += Utils.dp(50f)
                    view.setLayoutParams(params)
                }

                is LinearLayout.LayoutParams -> {
                    val params = view.layoutParams as LinearLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    params.topMargin += Utils.dp(50f)
                    view.setLayoutParams(params)
                }

                is RelativeLayout.LayoutParams -> {
                    val params = view.layoutParams as LinearLayout.LayoutParams
                    params.gravity = Gravity.TOP
                    params.topMargin += Utils.dp(50f)
                    view.setLayoutParams(params)
                }
            }
        }

        return ret.setOnClickListener { ret.dismiss() }
    }

    fun themedSnack(
        @StringRes resId: Int, vararg params: Any?
    ): Snackbar {
        return themedSnack(view.resources.getString(resId, params))
    }

    fun themedSnack(
        text: String?
    ): Snackbar {
        val color = CurrentTheme.getColorPrimary(view.context)
        val text_color =
            if (isColorDark(color)) "#ffffff".toColor() else "#000000".toColor()
        val ret = Snackbar.make(view, text.orEmpty(), duration).setBackgroundTint(color)
            .setActionTextColor(text_color).setTextColor(text_color).setAnchorView(anchorView)
        return ret.setOnClickListener { ret.dismiss() }
    }

    companion object {
        fun createCustomSnackbars(
            view: View?,
            anchorView: View? = null,
            notAttached: Boolean = false
        ): CustomSnackbars? {
            return view?.let {
                if ((view.isAttachedToWindow || notAttached) && Snackbar.findSuitableParent(it) != null) CustomSnackbars(
                    it,
                    anchorView
                ) else null
            }
        }

        internal fun isColorDark(color: Int): Boolean {
            return ColorUtils.calculateLuminance(color) < 0.5
        }
    }

    override fun setDuration(duration: Int): AbsCustomToast {
        when (duration) {
            Toast.LENGTH_SHORT -> setDurationSnack(Snackbar.LENGTH_SHORT)
            Toast.LENGTH_LONG -> setDurationSnack(Snackbar.LENGTH_LONG)
            else -> setDurationSnack(Snackbar.LENGTH_SHORT)
        }
        return this
    }

    override fun showToast(message: String?) {
        defaultSnack(message, true).show()
    }

    override fun showToast(@StringRes message: Int, vararg params: Any?) {
        defaultSnack(message, true, params).show()
    }

    override fun showToastBottom(message: String?) {
        defaultSnack(message, false).show()
    }

    override fun showToastBottom(message: Int, vararg params: Any?) {
        defaultSnack(message, false, params).show()
    }

    override fun showToastSuccessBottom(message: String?) {
        coloredSnack(message, "#AA48BE2D".toColor(), false).show()
    }

    override fun showToastSuccessBottom(@StringRes message: Int, vararg params: Any?) {
        coloredSnack(message, "#AA48BE2D".toColor(), false, params).show()
    }

    override fun showToastWarningBottom(message: String?) {
        coloredSnack(message, "#AAED760E".toColor(), false).show()
    }

    override fun showToastWarningBottom(@StringRes message: Int, vararg params: Any?) {
        coloredSnack(message, "#AAED760E".toColor(), false, params).show()
    }

    override fun showToastInfo(message: String?) {
        coloredSnack(message, CurrentTheme.getColorSecondary(view.context), true).show()
    }

    override fun showToastInfo(@StringRes message: Int, vararg params: Any?) {
        coloredSnack(message, CurrentTheme.getColorSecondary(view.context), true, params).show()
    }

    override fun showToastError(message: String?) {
        coloredSnack(message, "#F44336".toColor(), true).show()
    }

    override fun showToastError(message: Int, vararg params: Any?) {
        coloredSnack(message, "#F44336".toColor(), true, params).show()
    }

    override fun showToastThrowable(throwable: Throwable?) {
        val ret = coloredSnack(
            ErrorLocalizer.localizeThrowable(
                Includes.provideApplicationContext(),
                throwable
            ), "#F44336".toColor(), true
        )
        if (throwable !is SocketTimeoutException && throwable !is UnknownHostException) {
            ret.setAction(R.string.more_info) {
                val text = StringBuilder()
                text.append(
                    ErrorLocalizer.localizeThrowable(
                        Includes.provideApplicationContext(),
                        throwable
                    )
                )
                text.append("\r\n")
                for (stackTraceElement in (throwable ?: return@setAction).stackTrace) {
                    text.append("    ")
                    text.append(stackTraceElement)
                    text.append("\r\n")
                }
                MaterialAlertDialogBuilder(view.context)
                    .setIcon(R.drawable.ic_error)
                    .setMessage(text)
                    .setTitle(R.string.more_info)
                    .setPositiveButton(R.string.button_ok, null)
                    .setCancelable(true)
                    .show()
            }
        }
        ret.show()
    }
}
