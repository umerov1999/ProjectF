package dev.ragnarok.fenrir.fragment.base

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.App.Companion.instance
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.fragment.base.core.AbsPresenter
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.service.ErrorLocalizer
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicLong

abstract class RxSupportPresenter<V : IMvpView>(savedInstanceState: Bundle?) :
    AbsPresenter<V>() {
    private var generatedId = -1L
    protected val compositeJob = CompositeJob()
    var viewCreationCount = 0
        private set

    protected fun fireTempDataUsage(): Long {
        if (generatedId < 0) {
            generatedId = IDGEN.incrementAndGet()
        }
        return generatedId
    }

    override fun onGuiCreated(viewHost: V) {
        viewCreationCount++
        super.onGuiCreated(viewHost)
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putLong(SAVE_TEMP_DATA_USAGE_ID, generatedId)
    }

    override fun onDestroyed() {
        compositeJob.cancel()
        if (generatedId >= 0) {
            Stores.instance
                .tempStore()
                .deleteTemporaryData(generatedId)
                .hiddenIO()
            generatedId = -1
        }
        super.onDestroyed()
    }

    fun appendJob(job: Job) {
        compositeJob.add(job)
    }

    protected fun showError(throwable: Throwable?) {
        resumedView ?: return
        throwable ?: return
        if (resumedView !is IErrorView) {
            return
        }
        val eView = resumedView as IErrorView
        val lThrowable = Utils.getCauseIfRuntime(throwable)
        if (Constants.IS_DEBUG) {
            lThrowable.printStackTrace()
        }
        if (Settings.get().main().isDeveloper_mode) {
            eView.showThrowable(lThrowable)
        } else {
            eView.showError(ErrorLocalizer.localizeThrowable(applicationContext, lThrowable))
        }
    }

    protected fun showToast(@StringRes titleTes: Int, isLong: Boolean, vararg params: Any?) {
        view ?: return
        if (view !is IToastView) {
            return
        }
        val tView = resumedView as IToastView
        tView.customToast?.setDuration(if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
            ?.showToast(titleTes, *params)
    }

    protected fun showError(view: IErrorView?, throwable: Throwable?) {
        view ?: return
        throwable ?: return
        val lThrowable = Utils.getCauseIfRuntime(throwable)
        if (Constants.IS_DEBUG) {
            lThrowable.printStackTrace()
        }
        if (Settings.get().main().isDeveloper_mode) {
            view.showThrowable(lThrowable)
        } else {
            view.showError(ErrorLocalizer.localizeThrowable(applicationContext, lThrowable))
        }
    }

    protected val applicationContext: Context
        get() = Includes.provideApplicationContext()

    protected fun getString(@StringRes res: Int): String {
        return instance.getString(res)
    }

    protected fun getString(@StringRes res: Int, vararg params: Any?): String {
        return instance.getString(res, *params)
    }

    companion object {
        private val IDGEN = AtomicLong()
        private const val SAVE_TEMP_DATA_USAGE_ID = "save_temp_data_usage_id"
    }

    init {
        if (savedInstanceState != null) {
            generatedId = savedInstanceState.getLong(SAVE_TEMP_DATA_USAGE_ID, -1)
            if (generatedId >= 0 && generatedId >= IDGEN.get()) {
                IDGEN.set(generatedId + 1)
            }
        }
    }
}
