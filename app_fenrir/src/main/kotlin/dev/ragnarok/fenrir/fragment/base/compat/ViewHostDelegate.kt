package dev.ragnarok.fenrir.fragment.base.compat

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IPresenter
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import java.lang.ref.WeakReference

class ViewHostDelegate<P : IPresenter<V>, V : IMvpView> {

    private var lastKnownPresenterState: Bundle? = null

    private var viewCreated: Boolean = false

    var presenter: P? = null

    private var viewReference: WeakReference<V?> = WeakReference(null)

    private val onReceivePresenterActions = ArrayList<P.() -> Unit>()

    @Volatile
    private var app: Context? = null

    fun onCreate(
        context: Context,
        view: V,
        factoryProvider: IFactoryProvider<P, V>,
        loaderManager: androidx.loader.app.LoaderManager,
        savedInstanceState: Bundle?
    ) {
        this.viewReference = WeakReference(view)

        if (savedInstanceState != null) {
            this.lastKnownPresenterState = savedInstanceState.getBundle(SAVE_PRESENTER_STATE)
        }

        app = context.applicationContext
        val loader = loaderManager.initLoader(
            LOADER_ID,
            lastKnownPresenterState,
            object : androidx.loader.app.LoaderManager.LoaderCallbacks<P> {
                override fun onCreateLoader(
                    id: Int,
                    args: Bundle?
                ): androidx.loader.content.Loader<P> {
                    return SimplePresenterLoader(app!!, factoryProvider.getPresenterFactory(args))
                }

                override fun onLoadFinished(loader: androidx.loader.content.Loader<P>, data: P) {

                }

                override fun onLoaderReset(loader: androidx.loader.content.Loader<P>) {
                    presenter = null
                }
            })

        @Suppress("UNCHECKED_CAST")
        @SuppressWarnings("unchecked")
        presenter = (loader as SimplePresenterLoader<P, V>).get()
        presenter?.run {
            attachViewHost(view)
            for (action in onReceivePresenterActions) {
                apply(action)
            }
            onReceivePresenterActions.clear()
        }
    }

    fun onDestroy() {
        viewReference = WeakReference(null)
        presenter?.detachViewHost()
    }

    fun onViewCreated() {
        if (viewCreated) {
            return
        }

        viewCreated = true
        presenter?.createView(viewReference.get() ?: return)
    }

    fun onDestroyView() {
        viewCreated = false
        presenter?.destroyView()
    }

    fun lazyPresenter(block: P.() -> Unit) {
        presenter?.apply(block) ?: run {
            onReceivePresenterActions.add(block)
        }
    }

    fun onResume() {
        presenter?.resumeView()
    }

    fun onPause() {
        presenter?.pauseView()
    }

    fun onSaveInstanceState(outState: Bundle) {
        presenter?.run {
            lastKnownPresenterState = Bundle()
            saveState(lastKnownPresenterState ?: return@run)
        }

        outState.putBundle(SAVE_PRESENTER_STATE, lastKnownPresenterState)
    }

    interface IFactoryProvider<P : IPresenter<V>, V : IMvpView> {
        fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<P>
    }

    companion object {
        private const val SAVE_PRESENTER_STATE = "save-presenter-state"
        private const val LOADER_ID = 101
    }
}