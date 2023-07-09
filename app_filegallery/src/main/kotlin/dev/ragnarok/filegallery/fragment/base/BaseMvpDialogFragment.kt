package dev.ragnarok.filegallery.fragment.base

import android.graphics.Color
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import dev.ragnarok.filegallery.Includes.provideApplicationContext
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.fragment.base.compat.AbsMvpDialogFragment
import dev.ragnarok.filegallery.fragment.base.core.AbsPresenter
import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.fragment.base.core.IToastView
import dev.ragnarok.filegallery.util.ErrorLocalizer
import dev.ragnarok.filegallery.util.toast.AbsCustomToast
import dev.ragnarok.filegallery.util.toast.CustomSnackbars
import dev.ragnarok.filegallery.util.toast.CustomToast
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class BaseMvpDialogFragment<P : AbsPresenter<V>, V : IMvpView> :
    AbsMvpDialogFragment<P, V>(), IMvpView, IErrorView, IToastView {

    override fun showError(errorText: String?) {
        if (isAdded) {
            customToast?.showToastError(errorText)
        }
    }

    override fun showError(@StringRes titleTes: Int, vararg params: Any?) {
        if (isAdded) {
            showError(getString(titleTes, *params))
        }
    }

    override fun showThrowable(throwable: Throwable?) {
        if (isAdded) {
            CustomSnackbars.createCustomSnackbars(view)?.let {
                val snack = it.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG).coloredSnack(
                    ErrorLocalizer.localizeThrowable(provideApplicationContext(), throwable),
                    Color.parseColor("#eeff0000")
                )
                if (throwable !is SocketTimeoutException && throwable !is UnknownHostException) {
                    snack.setAction(R.string.more_info) {
                        val text = StringBuilder()
                        text.append(
                            ErrorLocalizer.localizeThrowable(
                                provideApplicationContext(),
                                throwable
                            )
                        )
                        text.append("\r\n")
                        for (stackTraceElement in (throwable ?: return@setAction).stackTrace) {
                            text.append("    ")
                            text.append(stackTraceElement)
                            text.append("\r\n")
                        }
                        MaterialAlertDialogBuilder(requireActivity())
                            .setIcon(R.drawable.ic_error)
                            .setMessage(text)
                            .setTitle(R.string.more_info)
                            .setPositiveButton(R.string.button_ok, null)
                            .setCancelable(true)
                            .show()
                    }
                }
                snack.show()
            } ?: showError(ErrorLocalizer.localizeThrowable(provideApplicationContext(), throwable))
        }
    }

    override val customToast: AbsCustomToast?
        get() = if (isAdded) {
            CustomToast.createCustomToast(requireActivity(), view)
        } else null
}
