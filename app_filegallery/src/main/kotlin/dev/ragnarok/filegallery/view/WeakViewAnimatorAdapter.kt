package dev.ragnarok.filegallery.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import java.lang.ref.WeakReference

abstract class WeakViewAnimatorAdapter<V : View?>(ref: V) : AnimatorListenerAdapter() {
    private val ref: WeakReference<V> = WeakReference(ref)
    override fun onAnimationEnd(animation: Animator) {
        val view = ref.get()
        view?.let { onAnimationEnd(it) }
    }

    override fun onAnimationStart(animation: Animator) {
        val view = ref.get()
        view?.let { onAnimationStart(it) }
    }

    override fun onAnimationCancel(animation: Animator) {
        val view = ref.get()
        view?.let { onAnimationCancel(it) }
    }

    protected open fun onAnimationCancel(view: V) {}
    abstract fun onAnimationEnd(view: V)
    open fun onAnimationStart(view: V) {}

}