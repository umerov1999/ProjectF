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
import dev.ragnarok.filegallery.util.coroutines.CompositeJob
import kotlinx.coroutines.Job

abstract class RxSupportPresenter<V : IMvpView> :
    AbsPresenter<V>() {
    protected val compositeJob = CompositeJob()
    var viewCreationCount = 0
        private set

    override fun onGuiCreated(viewHost: V) {
        viewCreationCount++
        super.onGuiCreated(viewHost)
    }

    override fun onDestroyed() {
        compositeJob.cancel()
        super.onDestroyed()
    }

    fun appendJob(job: Job) {
        compositeJob.add(job)
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
