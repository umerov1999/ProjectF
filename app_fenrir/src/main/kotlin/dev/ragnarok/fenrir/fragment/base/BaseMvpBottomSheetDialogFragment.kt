package dev.ragnarok.fenrir.fragment.base

import android.graphics.Color
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.ActivityUtils
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.fragment.base.compat.AbsMvpBottomSheetDialogFragment
import dev.ragnarok.fenrir.fragment.base.core.*
import dev.ragnarok.fenrir.service.ErrorLocalizer.localizeThrowable
import dev.ragnarok.fenrir.util.ViewUtils
import dev.ragnarok.fenrir.util.spots.SpotsDialog
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class BaseMvpBottomSheetDialogFragment<P : AbsPresenter<V>, V : IMvpView> :
    AbsMvpBottomSheetDialogFragment<P, V>(), IMvpView, IProgressView,
    IErrorView, IToastView, IToolbarView {
    private var mLoadingProgressDialog: AlertDialog? = null

    override fun showError(errorText: String?) {
        if (isAdded) {
            customToast.showToastError(errorText)
        }
    }

    override val customToast: CustomToast
        get() = if (isAdded) {
            createCustomToast(requireActivity())
        } else createCustomToast(null)

    override fun showError(@StringRes titleTes: Int, vararg params: Any?) {
        if (isAdded) {
            showError(getString(titleTes, *params))
        }
    }

    override fun showThrowable(throwable: Throwable?) {
        if (isAdded) {
            CustomSnackbars.createCustomSnackbars(view)?.let {
                val snack = it.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG).coloredSnack(
                    localizeThrowable(provideApplicationContext(), throwable),
                    Color.parseColor("#eeff0000")
                )
                if (throwable !is ApiException && throwable !is SocketTimeoutException && throwable !is UnknownHostException) {
                    snack.setAction(R.string.more_info) {
                        val text = StringBuilder()
                        text.append(
                            localizeThrowable(
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
            } ?: showError(localizeThrowable(provideApplicationContext(), throwable))
        }
    }

    override fun setToolbarSubtitle(subtitle: String?) {
        ActivityUtils.setToolbarSubtitle(this, subtitle)
    }

    override fun setToolbarTitle(title: String?) {
        ActivityUtils.setToolbarTitle(this, title)
    }

    protected fun styleSwipeRefreshLayoutWithCurrentTheme(
        swipeRefreshLayout: SwipeRefreshLayout,
        needToolbarOffset: Boolean
    ) {
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(
            requireActivity(),
            swipeRefreshLayout,
            needToolbarOffset
        )
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

    companion object {
        const val EXTRA_HIDE_TOOLBAR = "extra_hide_toolbar"


        protected fun safelySetChecked(button: CompoundButton?, checked: Boolean) {
            button?.isChecked = checked
        }


        protected fun safelySetText(target: TextView?, text: String?) {
            target?.text = text
        }


        protected fun safelySetText(target: TextView?, @StringRes text: Int) {
            target?.setText(text)
        }


        protected fun safelySetVisibleOrGone(target: View?, visible: Boolean) {
            target?.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
}
