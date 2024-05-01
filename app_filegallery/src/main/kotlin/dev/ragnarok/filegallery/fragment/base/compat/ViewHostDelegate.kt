package dev.ragnarok.filegallery.fragment.base.compat

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.fragment.base.core.IPresenter
import java.lang.ref.WeakReference

class ViewHostDelegate<P : IPresenter<V>, V : IMvpView> {

    private var lastKnownPresenterState: Bundle? = null

    private var viewCreated: Boolean = false

    var presenter: P? = null

    private var viewReference: WeakReference<V?> = WeakReference(null)

    private val onReceivePresenterActions = ArrayList<P.() -> Unit>()

    @Suppress("UNCHECKED_CAST")
    fun onCreate(
        view: V,
        factoryProvider: IFactoryProvider<P, V>,
        viewModelOwner: ViewModelStoreOwner,
        savedInstanceState: Bundle?
    ) {
        this.viewReference = WeakReference(view)

        if (savedInstanceState != null) {
            this.lastKnownPresenterState = savedInstanceState.getBundle(SAVE_PRESENTER_STATE)
        }

        val loader = ViewModelProvider(
            viewModelOwner,
            factoryPresenterLoader
        )["fenrirPresenters", PresenterLoader::class] as PresenterLoader<P, V>

        presenter = loader.make(lastKnownPresenterState, factoryProvider)
        presenter?.let {
            it.attachViewHost(view)
            for (action in onReceivePresenterActions) {
                it.apply(action)
            }
            onReceivePresenterActions.clear()
        }
    }

    fun onRestoreViewState() {
        presenter?.restoreViewState()
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
        fun getPresenterFactory(saveInstanceState: Bundle?): P
    }

    internal class PresenterLoader<P : IPresenter<V>, V : IMvpView> : ViewModel() {
        var presenter: P? = null

        fun make(savedInstanceState: Bundle?, factory: IFactoryProvider<P, V>): P? {
            if (presenter == null) {
                presenter = factory.getPresenterFactory(savedInstanceState)
            }
            return presenter
        }

        override fun onCleared() {
            presenter?.destroy()
            presenter = null
            super.onCleared()
        }
    }

    private val factoryPresenterLoader = object : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PresenterLoader<P, V>() as T
        }
    }

    companion object {
        private const val SAVE_PRESENTER_STATE = "save-presenter-state"
    }
}
