package dev.ragnarok.filegallery.view.media

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import dev.ragnarok.filegallery.media.music.MusicPlaybackController


class PlayPauseButton(context: Context, attrs: AttributeSet?) : MaterialPlayPauseFab(
    context, attrs
), View.OnClickListener {
    override fun onClick(v: View) {
        MusicPlaybackController.playOrPause()
        updateState()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scaleX = 0f
        scaleY = 0f
        rotation = 0f
        animate()
            .scaleX(1f)
            .scaleY(1f)
            .rotation(360f)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun updateState() {
        when {
            MusicPlaybackController.currentAudio == null -> {
                setIcon(MediaActionDrawable.ICON_EMPTY, true)
            }
            MusicPlaybackController.isPlaying -> {
                setIcon(MediaActionDrawable.ICON_PAUSE, true)
            }
            else -> {
                setIcon(MediaActionDrawable.ICON_PLAY, true)
            }
        }
    }

    init {
        setOnClickListener(this)
    }
}