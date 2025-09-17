package dev.ragnarok.fenrir.fragment.base

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import dev.ragnarok.fenrir.dialog.BottomSheetErrorDialog
import dev.ragnarok.fenrir.fragment.base.compat.AbsMvpDialogFragment
import dev.ragnarok.fenrir.fragment.base.core.AbsPresenter
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IProgressView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.service.ErrorLocalizer.localizeThrowable
import dev.ragnarok.fenrir.util.spots.SpotsDialog
import dev.ragnarok.fenrir.util.toast.AbsCustomToast
import dev.ragnarok.fenrir.util.toast.CustomToast
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class BaseMvpDialogFragment<P : AbsPresenter<V>, V : IMvpView> :
    AbsMvpDialogFragment<P, V>(), IMvpView, IProgressView, IErrorView, IToastView {
    private var mLoadingProgressDialog: AlertDialog? = null
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
            customToast?.showToastThrowable(throwable)
        }
    }

    override fun showBottomSheetError(title: String?, description: String?) {
        if (isAdded) {
            val dialog = BottomSheetErrorDialog()
            val bundle = Bundle()
            bundle.putString(Extra.TITLE, title)
            bundle.putString(Extra.DATA, description)
            dialog.arguments = bundle
            dialog.show(parentFragmentManager, "BottomSheetErrorDialog")
        }
    }

    override fun showBottomSheetError(throwable: Throwable?) {
        if (isAdded) {
            val text = StringBuilder()
            if (throwable !is SocketTimeoutException && throwable !is UnknownHostException) {
                for (stackTraceElement in (throwable ?: return).stackTrace) {
                    text.append("    ")
                    text.append(stackTraceElement)
                    text.append("\r\n")
                }
            }

            var stackTraceString = text.toString()
            if (stackTraceString.length > 500) {
                val disclaimer = " [stack trace too large]"
                stackTraceString = stackTraceString.substring(
                    0,
                    500 - disclaimer.length
                ) + disclaimer
            }

            showBottomSheetError(
                localizeThrowable(provideApplicationContext(), throwable),
                stackTraceString
            )
        }
    }

    override fun displayProgressDialog(
        @StringRes title: Int,
        @StringRes message: Int,
        cancelable: Boolean
    ) {
        dismissProgressDialog()
        mLoadingProgressDialog = SpotsDialog.Builder().setContext(requireActivity())
            .setMessage(getString(title) + ": " + getString(message)).setCancelable(cancelable)
            .build()
        mLoadingProgressDialog?.show()
    }

    override fun dismissProgressDialog() {
        if (mLoadingProgressDialog?.isShowing == true) {
            mLoadingProgressDialog?.cancel()
        }
    }

    override val customToast: AbsCustomToast?
        get() = if (isAdded) {
            CustomToast.createCustomToast(requireActivity(), view)
        } else null
}
