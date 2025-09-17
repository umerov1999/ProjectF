package dev.ragnarok.fenrir.util.toast

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.ColorUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.service.ErrorLocalizer
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.theme.ThemesController
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.util.Utils

class CustomToast private constructor(context: Context) : AbsCustomToast {
    private val mContext: Context
    private var duration: Int
    private var image: Bitmap? = null
    override fun setAnchorView(anchorView: View?): CustomToast {
        return this
    }

    override fun setDuration(duration: Int): CustomToast {
        this.duration = duration
        return this
    }

    override fun setBitmap(image: Bitmap?): CustomToast {
        this.image = image
        return this
    }

    override fun showToast(message: String?) {
        val t = getToast(mContext, message, CurrentTheme.getColorToast(mContext))
        t.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 15)
        t.show()
    }

    override fun showToast(@StringRes message: Int, vararg params: Any?) {
        showToast(mContext.resources.getString(message, *params))
    }

    override fun showToastBottom(message: String?) {
        val t = getToast(mContext, message, CurrentTheme.getColorToast(mContext))
        t.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 40)
        t.show()
    }

    override fun showToastBottom(@StringRes message: Int, vararg params: Any?) {
        showToastBottom(mContext.resources.getString(message, *params))
    }

    override fun showToastSuccessBottom(message: String?) {
        val t = getToast(mContext, message, "#AA48BE2D".toColor())
        t.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 40)
        t.show()
    }

    override fun showToastSuccessBottom(@StringRes message: Int, vararg params: Any?) {
        showToastSuccessBottom(mContext.resources.getString(message, *params))
    }

    override fun showToastWarningBottom(message: String?) {
        val t = getToast(mContext, message, "#AAED760E".toColor())
        t.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 40)
        t.show()
    }

    override fun showToastWarningBottom(@StringRes message: Int, vararg params: Any?) {
        showToastWarningBottom(mContext.resources.getString(message, *params))
    }

    override fun showToastInfo(message: String?) {
        val t = getToast(mContext, message, CurrentTheme.getColorSecondary(mContext))
        t.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 15)
        t.show()
    }

    override fun showToastInfo(@StringRes message: Int, vararg params: Any?) {
        showToastInfo(mContext.resources.getString(message, *params))
    }

    override fun showToastError(message: String?) {
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            showToastErrorS(message)
            return
        }
         */
        val view = View.inflate(mContext, R.layout.toast_error, null)
        val subtitle = view.findViewById<TextView>(R.id.text)
        val iconError =
            view.findViewById<ImageView>(R.id.icon_toast_error)
        if (image != null)
            iconError.setImageBitmap(image)
        subtitle.text = message
        val toast = Toast(mContext)
        toast.duration = duration
        @Suppress("deprecation")
        toast.view = view
        toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 0)
        toast.show()
    }

    override fun showToastThrowable(throwable: Throwable?) {
        var pThrowable = throwable ?: return
        pThrowable = Utils.getCauseIfRuntime(pThrowable)
        if (Constants.IS_DEBUG) {
            pThrowable.printStackTrace()
        }
        showToastError(ErrorLocalizer.localizeThrowable(mContext, pThrowable))
    }

    private fun showToastErrorS(message: String?) {
        val toast = Toast(mContext)
        toast.duration = duration
        toast.setText(message)
        toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 0)
        toast.show()
    }

    override fun showToastError(@StringRes message: Int, vararg params: Any?) {
        showToastError(mContext.resources.getString(message, *params))
    }

    companion object {
        fun createCustomToast(
            context: Context?,
            view: View?,
            anchorView: View? = null
        ): AbsCustomToast? {
            if (view != null && view.isAttachedToWindow && Snackbar.findSuitableParent(view) != null) {
                return CustomSnackbars.createCustomSnackbars(view, anchorView)
            }
            if (context is Activity && (context.isFinishing || context.isDestroyed)) {
                return null
            }
            return context?.let { CustomToast(it) }
        }
    }

    /*
    private fun getToastS(context: Context, message: String?): Toast {
        val toast = Toast(context)
        toast.setText(message)
        toast.duration = duration
        return toast
    }
     */

    private fun getToast(context: Context, message: String?, bgColor: Int): Toast {
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return getToastS(context, message)
        }
         */
        val toast = Toast(context)
        val view: View = View.inflate(context, R.layout.custom_toast_base, null)
        val cardView: MaterialCardView = view.findViewById(R.id.toast_card_view)
        cardView.setCardBackgroundColor(bgColor)
        val textView: TextView = view.findViewById(R.id.toast_text_view)
        if (message != null) textView.text = message
        if (isColorDark(bgColor)) textView.setTextColor(Color.WHITE)
        @Suppress("deprecation")
        toast.view = view
        val iconIV: ImageView = view.findViewById(R.id.toast_image_view)
        if (image != null)
            iconIV.setImageBitmap(image)
        else
            iconIV.setImageResource(R.mipmap.ic_launcher_round)
        toast.duration = duration
        return toast
    }

    private fun isColorDark(color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    init {
        duration = Toast.LENGTH_SHORT
        mContext =
            context as? Activity ?: ContextThemeWrapper(context, ThemesController.currentStyle())
    }
}
