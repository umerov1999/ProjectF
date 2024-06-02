package dev.ragnarok.fenrir.util.toast

import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.util.Utils.isColorDark

class CustomSnackbars private constructor(private val view: View, private val anchorView: View?) {
    @BaseTransientBottomBar.Duration
    private var duration: Int = BaseTransientBottomBar.LENGTH_SHORT
    fun setDurationSnack(@BaseTransientBottomBar.Duration duration: Int): CustomSnackbars {
        this.duration = duration
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
            if (isColorDark(color)) "#ffffff".toColor() else "#000000".toColor()
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
    }
}
