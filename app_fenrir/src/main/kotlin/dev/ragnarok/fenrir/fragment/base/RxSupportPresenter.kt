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
import dev.ragnarok.fenrir.util.InstancesCounter
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.rxutils.RxUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

abstract class RxSupportPresenter<V : IMvpView>(savedInstanceState: Bundle?) :
    AbsPresenter<V>(savedInstanceState) {
    protected var instanceId = 0L
    protected val compositeDisposable = CompositeDisposable()
    private var tempDataUsage = false
    var viewCreationCount = 0
        private set

    protected fun fireTempDataUsage() {
        tempDataUsage = true
    }

    public override fun onGuiCreated(viewHost: V) {
        viewCreationCount++
        super.onGuiCreated(viewHost)
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putLong(SAVE_INSTANCE_ID, instanceId)
        outState.putBoolean(SAVE_TEMP_DATA_USAGE, tempDataUsage)
    }

    override fun onDestroyed() {
        compositeDisposable.dispose()
        if (tempDataUsage) {
            RxUtils.subscribeOnIOAndIgnore(
                Stores.instance
                    .tempStore()
                    .deleteTemporaryData(instanceId)
            )
            tempDataUsage = false
        }
        super.onDestroyed()
    }

    fun appendDisposable(disposable: Disposable) {
        compositeDisposable.add(disposable)
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
        if (Settings.get().other().isDeveloper_mode) {
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
            ?.showToast(titleTes, params)
    }

    protected fun showError(view: IErrorView?, throwable: Throwable?) {
        view ?: return
        throwable ?: return
        val lThrowable = Utils.getCauseIfRuntime(throwable)
        if (Constants.IS_DEBUG) {
            lThrowable.printStackTrace()
        }
        if (Settings.get().other().isDeveloper_mode) {
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
        private const val SAVE_INSTANCE_ID = "save_instance_id"
        private const val SAVE_TEMP_DATA_USAGE = "save_temp_data_usage"
        private val instancesCounter = InstancesCounter()
    }

    init {
        savedInstanceState?.let {
            instanceId = savedInstanceState.getLong(SAVE_INSTANCE_ID)
            instancesCounter.fireExists(javaClass, instanceId)
        } ?: run { instanceId = instancesCounter.incrementAndGet(javaClass) }
    }
}
