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
import androidx.cardview.widget.CardView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.service.ErrorLocalizer
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.theme.ThemesController
import dev.ragnarok.fenrir.util.Utils

class CustomToast private constructor(context: Context?) {
    private val mContext: Context?
    private var duration: Int
    private var image: Bitmap? = null
    fun setDuration(duration: Int): CustomToast {
        this.duration = duration
        return this
    }

    fun setBitmap(Timage: Bitmap?): CustomToast {
        image = Timage
        return this
    }

    fun showToast(message: String?) {
        if (mContext == null) return
        val t = getToast(mContext, message, CurrentTheme.getColorToast(mContext))
        t.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 15)
        t.show()
    }

    fun showToast(@StringRes message: Int, vararg params: Any?) {
        if (mContext == null) return
        showToast(mContext.resources.getString(message, *params))
    }

    fun showToastBottom(message: String?) {
        if (mContext == null) return
        val t = getToast(mContext, message, CurrentTheme.getColorToast(mContext))
        t.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 40)
        t.show()
    }

    fun showToastBottom(@StringRes message: Int, vararg params: Any?) {
        if (mContext == null) return
        showToastBottom(mContext.resources.getString(message, *params))
    }

    fun showToastSuccessBottom(message: String?) {
        if (mContext == null) return
        val t = getToast(mContext, message, Color.parseColor("#AA48BE2D"))
        t.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 40)
        t.show()
    }

    fun showToastSuccessBottom(@StringRes message: Int, vararg params: Any?) {
        if (mContext == null) return
        showToastSuccessBottom(mContext.resources.getString(message, *params))
    }

    fun showToastWarningBottom(message: String?) {
        if (mContext == null) return
        val t = getToast(mContext, message, Color.parseColor("#AAED760E"))
        t.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 40)
        t.show()
    }

    fun showToastWarningBottom(@StringRes message: Int, vararg params: Any?) {
        if (mContext == null) return
        showToastWarningBottom(mContext.resources.getString(message, *params))
    }

    fun showToastInfo(message: String?) {
        if (mContext == null) return
        val t = getToast(mContext, message, ThemesController.toastColor(true))
        t.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 15)
        t.show()
    }

    fun showToastInfo(@StringRes message: Int, vararg params: Any?) {
        if (mContext == null) return
        showToastInfo(mContext.resources.getString(message, *params))
    }

    @Suppress("DEPRECATION")
    fun showToastError(message: String?) {
        if (mContext == null) return
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            showToastErrorS(message)
            return
        }
         */
        val view = View.inflate(mContext, R.layout.toast_error, null)
        val subtitle = view.findViewById<TextView>(R.id.text)
        val imagev =
            view.findViewById<ImageView>(R.id.icon_toast_error)
        if (image != null)
            imagev.setImageBitmap(image)
        subtitle.text = message
        val toast = Toast(mContext)
        toast.duration = duration
        toast.view = view
        toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 0)
        toast.show()
    }

    private fun showToastErrorS(message: String?) {
        if (mContext == null) return
        val toast = Toast(mContext)
        toast.duration = duration
        toast.setText(message)
        toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, 0)
        toast.show()
    }

    fun showToastError(@StringRes message: Int, vararg params: Any?) {
        if (mContext == null) return
        showToastError(mContext.resources.getString(message, *params))
    }

    fun showToastThrowable(throwable: Throwable?) {
        mContext ?: return
        var pThrowable = throwable ?: return
        pThrowable = Utils.getCauseIfRuntime(pThrowable)
        if (Constants.IS_DEBUG) {
            pThrowable.printStackTrace()
        }
        showToastError(ErrorLocalizer.localizeThrowable(mContext, pThrowable))
    }

    companion object {
        fun createCustomToast(context: Context?): CustomToast {
            if (context is Activity && (context.isFinishing || context.isDestroyed)) {
                return CustomToast(null)
            }
            return CustomToast(context)
        }
    }

    private fun getToastS(context: Context, message: String?): Toast {
        val toast = Toast(context)
        toast.setText(message)
        toast.duration = duration
        return toast
    }

    @Suppress("DEPRECATION")
    private fun getToast(context: Context, message: String?, bgColor: Int): Toast {
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return getToastS(context, message)
        }
         */
        val toast = Toast(context)
        val view: View = View.inflate(context, R.layout.custom_toast_base, null)
        val cardView: CardView = view.findViewById(R.id.toast_card_view)
        cardView.setCardBackgroundColor(bgColor)
        val textView: TextView = view.findViewById(R.id.toast_text_view)
        if (message != null) textView.text = message
        if (Utils.isColorDark(bgColor)) textView.setTextColor(Color.WHITE)
        toast.view = view
        val iconIV: ImageView = view.findViewById(R.id.toast_image_view)
        if (image != null)
            iconIV.setImageBitmap(image)
        else
            iconIV.setImageResource(R.mipmap.ic_launcher_round)
        toast.duration = duration
        return toast
    }

    init {
        duration = Toast.LENGTH_SHORT
        mContext = context
    }
}