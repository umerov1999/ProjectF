package dev.ragnarok.filegallery.fragment.base

import android.graphics.Color
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import dev.ragnarok.filegallery.Includes.provideApplicationContext
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.activity.ActivityUtils.setToolbarSubtitle
import dev.ragnarok.filegallery.activity.ActivityUtils.setToolbarTitle
import dev.ragnarok.filegallery.fragment.base.compat.AbsMvpFragment
import dev.ragnarok.filegallery.fragment.base.core.AbsPresenter
import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.fragment.base.core.IToastView
import dev.ragnarok.filegallery.fragment.base.core.IToolbarView
import dev.ragnarok.filegallery.util.ErrorLocalizer.localizeThrowable
import dev.ragnarok.filegallery.util.ViewUtils
import dev.ragnarok.filegallery.util.toast.AbsCustomToast
import dev.ragnarok.filegallery.util.toast.CustomSnackbars
import dev.ragnarok.filegallery.util.toast.CustomToast.Companion.createCustomToast
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class BaseMvpFragment<P : AbsPresenter<V>, V : IMvpView> : AbsMvpFragment<P, V>(),
    IMvpView, IErrorView, IToastView, IToolbarView {
    protected fun hasHideToolbarExtra(): Boolean {
        return arguments?.getBoolean(EXTRA_HIDE_TOOLBAR) == true
    }

    override fun showError(errorText: String?) {
        customToast?.showToastError(errorText)
    }

    override fun showThrowable(throwable: Throwable?) {
        if (isAdded) {
            CustomSnackbars.createCustomSnackbars(view)?.let {
                val snack = it.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG).coloredSnack(
                    localizeThrowable(provideApplicationContext(), throwable),
                    Color.parseColor("#eeff0000")
                )
                if (throwable !is SocketTimeoutException && throwable !is UnknownHostException) {
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

    override val customToast: AbsCustomToast?
        get() = if (isAdded) {
            createCustomToast(requireActivity(), view)
        } else null

    override fun showError(@StringRes titleTes: Int, vararg params: Any?) {
        if (isAdded) {
            showError(getString(titleTes, *params))
        }
    }

    override fun setToolbarSubtitle(subtitle: String?) {
        setToolbarSubtitle(this, subtitle)
    }

    override fun setToolbarTitle(title: String?) {
        setToolbarTitle(this, title)
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