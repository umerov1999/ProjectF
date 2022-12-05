package dev.ragnarok.filegallery.util.toast

import android.graphics.Color
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dev.ragnarok.filegallery.Includes
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.settings.CurrentTheme
import dev.ragnarok.filegallery.util.ErrorLocalizer
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
        @StringRes resId: Int, vararg params: Any?
    ): Snackbar {
        return defaultSnack(view.resources.getString(resId, params))
    }

    fun defaultSnack(
        text: String?
    ): Snackbar {
        val ret = Snackbar.make(view, text.orEmpty(), duration).setAnchorView(anchorView)
        return ret.setOnClickListener { ret.dismiss() }
    }

    fun coloredSnack(
        @StringRes resId: Int,
        @ColorInt color: Int, vararg params: Any?
    ): Snackbar {
        return coloredSnack(view.resources.getString(resId, params), color)
    }

    fun coloredSnack(
        text: String?,
        @ColorInt color: Int
    ): Snackbar {
        val text_color =
            if (isColorDark(color)) Color.parseColor("#ffffff") else Color.parseColor("#000000")
        val ret = Snackbar.make(view, text.orEmpty(), duration).setBackgroundTint(color)
            .setActionTextColor(text_color).setTextColor(text_color).setAnchorView(anchorView)
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
            if (isColorDark(color)) Color.parseColor("#ffffff") else Color.parseColor("#000000")
        val ret = Snackbar.make(view, text.orEmpty(), duration).setBackgroundTint(color)
            .setActionTextColor(text_color).setTextColor(text_color).setAnchorView(anchorView)
        return ret.setOnClickListener { ret.dismiss() }
    }

    companion object {
        fun createCustomSnackbars(view: View?, anchorView: View? = null): CustomSnackbars? {
            return view?.let { CustomSnackbars(it, anchorView) }
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
        defaultSnack(message).show()
    }

    override fun showToast(@StringRes message: Int, vararg params: Any?) {
        defaultSnack(message, params).show()
    }

    override fun showToastSuccessBottom(message: String?) {
        coloredSnack(message, Color.parseColor("#AA48BE2D")).show()
    }

    override fun showToastSuccessBottom(@StringRes message: Int, vararg params: Any?) {
        coloredSnack(message, Color.parseColor("#AA48BE2D"), params).show()
    }

    override fun showToastWarningBottom(message: String?) {
        coloredSnack(message, Color.parseColor("#AAED760E")).show()
    }

    override fun showToastWarningBottom(@StringRes message: Int, vararg params: Any?) {
        coloredSnack(message, Color.parseColor("#AAED760E"), params).show()
    }

    override fun showToastInfo(message: String?) {
        coloredSnack(message, CurrentTheme.getColorSecondary(view.context)).show()
    }

    override fun showToastInfo(@StringRes message: Int, vararg params: Any?) {
        coloredSnack(message, CurrentTheme.getColorSecondary(view.context), params).show()
    }

    override fun showToastError(message: String?) {
        coloredSnack(message, Color.parseColor("#F44336")).show()
    }

    override fun showToastError(message: Int, vararg params: Any?) {
        coloredSnack(message, Color.parseColor("#F44336"), params).show()
    }

    override fun showToastThrowable(throwable: Throwable?) {
        val ret = coloredSnack(
            ErrorLocalizer.localizeThrowable(
                Includes.provideApplicationContext(),
                throwable
            ), Color.parseColor("#F44336")
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
