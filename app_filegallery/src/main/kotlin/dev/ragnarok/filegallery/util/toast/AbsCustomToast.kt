package dev.ragnarok.filegallery.util.toast

import android.view.View
import androidx.annotation.StringRes

interface AbsCustomToast {
    fun setAnchorView(anchorView: View?): AbsCustomToast
    fun setDuration(duration: Int): AbsCustomToast
    fun showToast(message: String?)
    fun showToast(@StringRes message: Int, vararg params: Any?)
    fun showToastSuccessBottom(message: String?)
    fun showToastSuccessBottom(@StringRes message: Int, vararg params: Any?)
    fun showToastWarningBottom(message: String?)
    fun showToastWarningBottom(@StringRes message: Int, vararg params: Any?)
    fun showToastInfo(message: String?)
    fun showToastInfo(@StringRes message: Int, vararg params: Any?)
    fun showToastError(message: String?)
    fun showToastError(@StringRes message: Int, vararg params: Any?)
    fun showToastThrowable(throwable: Throwable?)
}
