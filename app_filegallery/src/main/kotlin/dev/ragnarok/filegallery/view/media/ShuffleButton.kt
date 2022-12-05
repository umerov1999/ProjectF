package dev.ragnarok.filegallery.view.media

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageButton
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.media.music.MusicPlaybackService

class ShuffleButton(context: Context, attrs: AttributeSet?) : AppCompatImageButton(
    context, attrs
), View.OnClickListener {
    override fun onClick(v: View) {
        MusicPlaybackController.cycleShuffle()
        updateShuffleState()
    }

    fun updateShuffleState() {
        when (MusicPlaybackController.shuffleMode) {
            MusicPlaybackService.SHUFFLE -> setImageResource(R.drawable.shuffle)
            MusicPlaybackService.SHUFFLE_NONE -> setImageResource(R.drawable.shuffle_disabled)
            else -> {}
        }
    }

    init {
        setOnClickListener(this)
    }
}