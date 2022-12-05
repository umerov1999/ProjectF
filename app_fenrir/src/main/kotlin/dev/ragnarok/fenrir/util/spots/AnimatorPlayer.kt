package dev.ragnarok.fenrir.util.spots

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet

internal class AnimatorPlayer(private val animators: ArrayList<Animator>) :
    AnimatorListenerAdapter() {
    private var interrupted = false
    override fun onAnimationEnd(animation: Animator) {
        if (!interrupted) animation.start()
    }

    fun play() {
        animate()
    }

    fun stop() {
        interrupted = true
    }

    private fun animate() {
        if (animators.isEmpty()) {
            return
        }
        val set = AnimatorSet()
        set.playTogether(animators)
        set.addListener(this)
        set.start()
    }
}