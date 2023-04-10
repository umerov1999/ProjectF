package dev.ragnarok.filegallery.media.exo

import androidx.media3.exoplayer.ExoPlayer

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