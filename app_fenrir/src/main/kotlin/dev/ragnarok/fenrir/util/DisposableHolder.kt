package dev.ragnarok.fenrir.util

import dev.ragnarok.fenrir.notDisposed
import io.reactivex.rxjava3.disposables.Disposable

class DisposableHolder<T> {
    private var disposable: Disposable? = null
    var tag: T? = null
        private set

    @JvmOverloads
    fun append(disposable: Disposable?, tag: T? = null) {
        dispose()
        this.disposable = disposable
        this.tag = tag
    }

    fun dispose() {
        if (disposable != null) {
            if (!(disposable ?: return).isDisposed) {
                (disposable ?: return).dispose()
            }
            disposable = null
        }
    }

    val isActive: Boolean
        get() = disposable?.notDisposed() == true
}