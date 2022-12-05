package dev.ragnarok.filegallery.view.media

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.media.music.MusicPlaybackService

class RepeatButton(context: Context, attrs: AttributeSet?) : AppCompatImageButton(
    context, attrs
), View.OnClickListener {
    override fun onClick(v: View) {
        MusicPlaybackController.cycleRepeat()
        updateRepeatState()
    }

    fun updateRepeatState() {
        when (MusicPlaybackController.repeatMode) {
            MusicPlaybackService.REPEAT_ALL -> setImageDrawable(
                AppCompatResources.getDrawable(
                    context, R.drawable.repeat
                )
            )
            MusicPlaybackService.REPEAT_CURRENT -> setImageDrawable(
                AppCompatResources.getDrawable(
                    context, R.drawable.repeat_once
                )
            )
            MusicPlaybackService.REPEAT_NONE -> setImageDrawable(
                AppCompatResources.getDrawable(
                    context, R.drawable.repeat_off
                )
            )
            else -> {}
        }
    }

    init {
        setOnClickListener(this)
    }
}