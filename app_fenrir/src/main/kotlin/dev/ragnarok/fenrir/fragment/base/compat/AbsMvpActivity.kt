package dev.ragnarok.fenrir.fragment.base.compat

import android.os.Bundle
import dev.ragnarok.fenrir.activity.NoMainActivity
import dev.ragnarok.fenrir.fragment.base.core.*

abstract class AbsMvpActivity<P : IPresenter<V>, V : IMvpView> : NoMainActivity(),
    ViewHostDelegate.IFactoryProvider<P, V>, IProgressView, IErrorView,
    IToastView, IToolbarView {

    private val delegate = ViewHostDelegate<P, V>()

    protected val presenter: P?
        get() = delegate.presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate.onCreate(
            getViewHost(),
            this,
            this,
            savedInstanceState
        )
    }

    // Override in case of fragment not implementing IPresenter<View> interface
    @Suppress("UNCHECKED_CAST")
    @SuppressWarnings("unchecked")
    private fun getViewHost(): V = this as V

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delegate.onViewCreated()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        delegate.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        delegate.onPause()
    }

    override fun onResume() {
        super.onResume()
        delegate.onResume()
    }

    override fun onDestroy() {
        delegate.onDestroyView()
        delegate.onDestroy()
        super.onDestroy()
    }

    fun lazyPresenter(block: P.() -> Unit) {
        delegate.lazyPresenter(block)
    }
}