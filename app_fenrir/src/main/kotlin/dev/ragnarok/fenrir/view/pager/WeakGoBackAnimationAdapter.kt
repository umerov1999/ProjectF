package dev.ragnarok.fenrir.view.pager

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import java.lang.ref.WeakReference

class WeakGoBackAnimationAdapter(holder: GoBackCallback) : AnimatorListenerAdapter() {
    private val mReference: WeakReference<GoBackCallback> = WeakReference(holder)
    override fun onAnimationEnd(animation: Animator) {
        val callback = mReference.get()
        callback?.goBack()
    }

}