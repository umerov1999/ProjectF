package dev.ragnarok.fenrir.fragment.sheet

import android.os.Bundle
import android.view.View
import androidx.loader.app.LoaderManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.ragnarok.fenrir.fragment.base.compat.ViewHostDelegate
import dev.ragnarok.fenrir.fragment.base.compat.ViewHostDelegate.IFactoryProvider
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IPresenter

abstract class AbsPresenterBottomSheetFragment<P : IPresenter<V>, V : IMvpView> :
    BottomSheetDialogFragment(), IFactoryProvider<P, V> {
    private val delegate = ViewHostDelegate<P, V>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate.onCreate(
            requireActivity(),
            presenterViewHost,
            this,
            LoaderManager.getInstance(this),
            savedInstanceState
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fireViewCreated()
    }

    fun fireViewCreated() {
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

    override fun onDestroyView() {
        super.onDestroyView()
        delegate.onDestroyView()
    }

    override fun onDestroy() {
        delegate.onDestroy()
        super.onDestroy()
    }

    // Override in case of fragment not implementing IPresenter<View> interface
    @Suppress("UNCHECKED_CAST")
    protected val presenterViewHost: V
        get() = this as V
    protected val presenter: P?
        get() = delegate.presenter

    fun lazyPresenter(block: P.() -> Unit) {
        delegate.lazyPresenter(block)
    }
}