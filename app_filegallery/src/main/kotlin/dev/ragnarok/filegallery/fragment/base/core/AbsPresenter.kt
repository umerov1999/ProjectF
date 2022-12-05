package dev.ragnarok.filegallery.fragment.base.core

import android.os.Bundle
import androidx.annotation.CallSuper
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

abstract class AbsPresenter<V : IMvpView>(savedInstanceState: Bundle?) : IPresenter<V> {

    /**
     * V (View Host - это не значит, что существуют реальные вьюхи,
     * с которыми можно работать и менять их состояние. Это значит,
     * что существует, к примеру, фрагмент, но нет гарантии, кто метод onCreateView был выполнен.
     * К примеру, фрагмент находится в стэке, он существует, может хранить какие-то данные,
     * но при перевороте его вьюв был уничтожен, но не был заново создан, ибо фрагмент не в топе
     * контейнера и не added)
     */
    private var viewReference: WeakReference<V> = WeakReference<V>(null)

    private var isGuiReady: Boolean = false

    val guiIsReady: Boolean
        get() = isGuiReady

    val guiIsResumed: Boolean
        get() = isGuiResumed

    var id: Int

    private var isDestroyed: Boolean = false

    private var isGuiResumed: Boolean = false

    val isViewHostAttached: Boolean
        get() = viewReference.get() != null

    val view: V?
        get() = if (isGuiReady) viewReference.get() else null

    val resumedView: V?
        get() = if (isGuiResumed) viewReference.get() else null

    val viewHost: V?
        get() = viewReference.get()

    init {
        if (savedInstanceState != null) {
            id = savedInstanceState.getInt(SAVE_ID)
            if (id >= IDGEN.get()) {
                IDGEN.set(id + 1)
            }
        } else {
            id = IDGEN.incrementAndGet()
        }
    }

    @CallSuper
    protected open fun onViewHostAttached(view: V) {

    }

    @CallSuper
    protected fun onViewHostDetached() {

    }

    @CallSuper
    protected open fun onGuiCreated(viewHost: V) {

    }

    @CallSuper
    protected open fun onGuiDestroyed() {

    }

    @CallSuper
    protected open fun onGuiResumed() {

    }

    @CallSuper
    protected open fun onGuiPaused() {

    }

    override fun destroy() {
        isDestroyed = true
        onDestroyed()
    }

    override fun resumeView() {
        isGuiResumed = true
        onGuiResumed()
    }

    override fun pauseView() {
        isGuiResumed = false
        onGuiPaused()
    }

    override fun attachViewHost(view: V) {
        viewReference = WeakReference(view)
        onViewHostAttached(view)
    }

    override fun detachViewHost() {
        viewReference = WeakReference<V>(null)
        onViewHostDetached()
    }

    final override fun createView(view: V) {
        isGuiReady = true
        onGuiCreated(view)
    }

    override fun destroyView() {
        isGuiReady = false
        onGuiDestroyed()
    }

    @CallSuper
    open fun onDestroyed() {

    }

    @CallSuper
    override fun saveState(outState: Bundle) {
        outState.putInt(SAVE_ID, id)
    }

    companion object {
        private val IDGEN = AtomicInteger()
        private const val SAVE_ID = "save_presenter_id"
    }
}
