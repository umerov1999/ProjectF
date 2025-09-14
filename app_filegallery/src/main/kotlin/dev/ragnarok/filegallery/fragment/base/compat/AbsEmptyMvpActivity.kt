package dev.ragnarok.filegallery.fragment.base.compat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.fragment.base.core.IPresenter
import dev.ragnarok.filegallery.fragment.base.core.IToastView
import dev.ragnarok.filegallery.fragment.base.core.IToolbarView

abstract class AbsEmptyMvpActivity<P : IPresenter<V>, V : IMvpView> : AppCompatActivity(),
    ViewHostDelegate.IFactoryProvider<P, V>, IErrorView,
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

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        delegate.onRestoreViewState()
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