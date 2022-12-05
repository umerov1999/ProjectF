package dev.ragnarok.filegallery.media.exo

import com.google.android.exoplayer2.ExoPlayer

object ExoUtil {
    fun pausePlayer(player: ExoPlayer?) {
        player?.playWhenReady = false
        player?.playbackState
    }

    fun startPlayer(player: ExoPlayer?) {
        player?.playWhenReady = true
        player?.playbackState
    }
}