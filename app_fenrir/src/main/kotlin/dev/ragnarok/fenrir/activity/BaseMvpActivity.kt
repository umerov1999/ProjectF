package dev.ragnarok.fenrir.activity

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import dev.ragnarok.fenrir.fragment.base.compat.AbsMvpActivity
import dev.ragnarok.fenrir.fragment.base.core.AbsPresenter
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.service.ErrorLocalizer.localizeThrowable
import dev.ragnarok.fenrir.util.ViewUtils
import dev.ragnarok.fenrir.util.spots.SpotsDialog
import dev.ragnarok.fenrir.util.toast.CustomToast
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast

abstract class BaseMvpActivity<P : AbsPresenter<V>, V : IMvpView> : AbsMvpActivity<P, V>(),
    IMvpView {
    private var mLoadingProgressDialog: AlertDialog? = null
    protected val arguments: Bundle?
        get() = intent?.extras

    protected fun requireArguments(): Bundle {
        return intent!!.extras!!
    }

    override fun showError(errorText: String?) {
        if (!isFinishing) {
            customToast.showToastError(errorText)
        }
    }

    override fun showThrowable(throwable: Throwable?) {
        if (!isFinishing) {
            showError(localizeThrowable(provideApplicationContext(), throwable))
        }
    }

    override val customToast: CustomToast
        get() = if (!isFinishing) {
            createCustomToast(this)
        } else createCustomToast(null)

    override fun showError(@StringRes titleTes: Int, vararg params: Any?) {
        if (!isFinishing) {
            showError(getString(titleTes, *params))
        }
    }

    override fun setToolbarSubtitle(subtitle: String?) {
        supportActionBar?.subtitle = subtitle
    }

    override fun setToolbarTitle(title: String?) {
        supportActionBar?.title = title
    }

    protected fun styleSwipeRefreshLayoutWithCurrentTheme(
        swipeRefreshLayout: SwipeRefreshLayout,
        needToolbarOffset: Boolean
    ) {
        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(
            this,
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
        mLoadingProgressDialog = SpotsDialog.Builder().setContext(this)
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
        fun safelySetChecked(button: CompoundButton?, checked: Boolean) {
            button?.isChecked = checked
        }

        fun safelySetText(target: TextView?, text: String?) {
            target?.text = text
        }

        fun safelySetText(target: TextView?, @StringRes text: Int) {
            target?.setText(text)
        }

        fun safelySetVisibleOrGone(target: View?, visible: Boolean) {
            target?.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
}