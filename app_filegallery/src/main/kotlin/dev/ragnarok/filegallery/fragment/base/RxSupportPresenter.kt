package dev.ragnarok.filegallery.fragment.base

import android.content.Context
import androidx.annotation.StringRes
import dev.ragnarok.filegallery.App.Companion.instance
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.Includes.provideApplicationContext
import dev.ragnarok.filegallery.fragment.base.core.AbsPresenter
import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.settings.Settings.get
import dev.ragnarok.filegallery.util.ErrorLocalizer
import dev.ragnarok.filegallery.util.Utils
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

abstract class RxSupportPresenter<V : IMvpView> :
    AbsPresenter<V>() {
    protected val compositeDisposable = CompositeDisposable()
    var viewCreationCount = 0
        private set

    override fun onGuiCreated(viewHost: V) {
        viewCreationCount++
        super.onGuiCreated(viewHost)
    }

    override fun onDestroyed() {
        compositeDisposable.dispose()
        super.onDestroyed()
    }

    fun appendDisposable(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

    protected fun showError(view: IErrorView?, throwable: Throwable?) {
        view ?: return
        throwable ?: return
        val lThrowable = Utils.getCauseIfRuntime(throwable)
        if (Constants.IS_DEBUG) {
            lThrowable.printStackTrace()
        }
        if (get().main().isDeveloper_mode) {
            view.showThrowable(lThrowable)
        } else {
            view.showError(ErrorLocalizer.localizeThrowable(applicationContext, lThrowable))
        }
    }

    protected val applicationContext: Context
        get() = provideApplicationContext()

    protected fun getString(@StringRes res: Int): String {
        return instance.getString(res)
    }

    protected fun getString(@StringRes res: Int, vararg params: Any?): String {
        return instance.getString(res, *params)
    }
}
