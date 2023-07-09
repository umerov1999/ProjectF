package dev.ragnarok.filegallery.fragment.base.compat

import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.fragment.base.core.IPresenter

abstract class AbsMvpBottomSheetDialogFragment<P : IPresenter<V>, V : IMvpView> :
    BottomSheetDialogFragment(), ViewHostDelegate.IFactoryProvider<P, V> {

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

    fun lazyPresenter(block: P.() -> Unit) {
        delegate.lazyPresenter(block)
    }
}
