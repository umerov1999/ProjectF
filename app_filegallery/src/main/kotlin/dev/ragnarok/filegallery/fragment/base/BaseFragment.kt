package dev.ragnarok.filegallery.fragment.base

import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

open class BaseFragment : Fragment() {
    private val mCompositeDisposable = CompositeDisposable()
    protected fun appendDisposable(disposable: Disposable) {
        mCompositeDisposable.add(disposable)
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
    }
}