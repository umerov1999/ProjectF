package dev.ragnarok.filegallery.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.squareup.picasso3.Transformation
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.media.music.PlayerStatus
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.picasso.PicassoInstance.Companion.with
import dev.ragnarok.filegallery.picasso.transforms.PolyTransformation
import dev.ragnarok.filegallery.picasso.transforms.RoundTransformation
import dev.ragnarok.filegallery.place.PlaceFactory
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.toMainThread
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.view.natives.rlottie.RLottieImageView
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.ref.WeakReference

class MiniPlayerView : FrameLayout, CustomSeekBar.CustomSeekBarListener {
    private var mPlayerDisposable = Disposable.disposed()
    private lateinit var visual: RLottieImageView
    private lateinit var playCover: ImageView
    private lateinit var title: TextView
    private lateinit var mProgress: CustomSeekBar
    private lateinit var root: View
    private var mTimeHandler: TimeHandler? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    ) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init()
    }

    private fun init() {
        root = LayoutInflater.from(context).inflate(R.layout.mini_player, this)
        val play = root.findViewById<View>(R.id.item_audio_play)
        playCover = root.findViewById(R.id.item_audio_play_cover)
        visual = root.findViewById(R.id.item_audio_visual)
        root.visibility =
            if (isInEditMode || MusicPlaybackController.miniPlayerVisibility) VISIBLE else GONE
        val mPClosePlay = root.findViewById<ImageButton>(R.id.close_player)
        mPClosePlay.setOnClickListener {
            MusicPlaybackController.closeMiniPlayer()
            root.visibility = GONE
        }
        mPClosePlay.setOnLongClickListener {
            MusicPlaybackController.stop()
            true
        }
        play.setOnClickListener {
            MusicPlaybackController.playOrPause()
            if (MusicPlaybackController.isPlaying) {
                Utils.doWavesLottie(visual, true)
                playCover.setColorFilter(Color.parseColor("#44000000"))
            } else {
                Utils.doWavesLottie(visual, false)
                playCover.clearColorFilter()
            }
        }
        play.setOnLongClickListener {
            MusicPlaybackController.next()
            true
        }
        val mOpenPlayer = root.findViewById<ImageButton>(R.id.open_player)
        mOpenPlayer.setOnClickListener {
            PlaceFactory.getPlayerPlace().tryOpenWith(
                context
            )
        }
        title = root.findViewById(R.id.mini_artist)
        title.isSelected = true
        mProgress = root.findViewById(R.id.seek_player_pos)
        mProgress.setCustomSeekBarListener(this)
    }

    internal fun queueNextRefresh(delay: Long) {
        mTimeHandler?.let {
            val message = it.obtainMessage(REFRESH_TIME)
            it.removeMessages(REFRESH_TIME)
            it.sendMessageDelayed(message, delay)
        }
    }

    private val transformCover: Transformation
        get() = if (Settings.get()
                .main().isAudio_round_icon()
        ) RoundTransformation() else PolyTransformation()

    @get:DrawableRes
    private val audioCoverSimple: Int
        get() = if (Settings.get()
                .main().isAudio_round_icon()
        ) R.drawable.audio_button else R.drawable.audio_button_material

    private fun updatePlaybackControls() {
        if (MusicPlaybackController.isPlaying) {
            Utils.doWavesLottie(visual, true)
            playCover.setColorFilter(Color.parseColor("#44000000"))
        } else {
            Utils.doWavesLottie(visual, false)
            playCover.clearColorFilter()
        }
    }

    private fun receiveFullAudioInfo() {
        updateVisibility()
        updateNowPlayingInfo()
        updatePlaybackControls()
        resolveControlViews()
    }

    private fun onServiceBindEvent(@PlayerStatus status: Int) {
        when (status) {
            PlayerStatus.UPDATE_TRACK_INFO -> {
                updateVisibility()
                updateNowPlayingInfo()
                updatePlaybackControls()
                resolveControlViews()
            }
            PlayerStatus.UPDATE_PLAY_PAUSE -> {
                updateVisibility()
                updatePlaybackControls()
                resolveControlViews()
            }
            PlayerStatus.SERVICE_KILLED -> {
                updateVisibility()
                updatePlaybackControls()
                updateNowPlayingInfo()
                resolveControlViews()
            }
            PlayerStatus.REPEATMODE_CHANGED, PlayerStatus.SHUFFLEMODE_CHANGED, PlayerStatus.UPDATE_PLAY_LIST -> {
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateNowPlayingInfo() {
        val artist = MusicPlaybackController.artistName
        val trackName = MusicPlaybackController.trackName
        title.text = Utils.firstNonEmptyString(
            artist,
            " "
        ) + " - " + Utils.firstNonEmptyString(trackName, " ")
        val audio = MusicPlaybackController.currentAudio
        val placeholder = AppCompatResources.getDrawable(context, audioCoverSimple)
        if (audio != null && placeholder != null && audio.thumb_image.nonNullNoEmpty()) {
            with()
                .load(audio.thumb_image)
                .placeholder(placeholder)
                .transform(transformCover)
                .into(playCover)
        } else {
            with().cancelRequest(playCover)
            playCover.setImageResource(audioCoverSimple)
        }
        //queueNextRefresh(1);
    }

    private fun updateVisibility() {
        root.visibility =
            if (MusicPlaybackController.miniPlayerVisibility) VISIBLE else GONE
    }

    private fun resolveControlViews() {
        val preparing = MusicPlaybackController.isPreparing
        val initialized = MusicPlaybackController.isInitialized
        mProgress.isEnabled = !preparing && initialized
        //mProgress.setIndeterminate(preparing);
    }

    internal fun refreshCurrentTime(): Long {
        if (!MusicPlaybackController.isInitialized) {
            mProgress.updateFullState(-1, -1, -1)
            return 500
        }
        try {
            val pos = MusicPlaybackController.position()
            val duration = MusicPlaybackController.duration()
            if (pos >= 0 && duration > 0) {
                mProgress.updateFullState(duration, pos, MusicPlaybackController.bufferPosition())
                if (!MusicPlaybackController.isPlaying) {
                    return 500
                }
            } else {
                mProgress.updateFullState(-1, -1, -1)
                return 500
            }
            return 300
        } catch (ignored: Exception) {
        }
        return 500
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        receiveFullAudioInfo()
        mTimeHandler = TimeHandler(this)
        val next = refreshCurrentTime()
        queueNextRefresh(next)
        mPlayerDisposable = MusicPlaybackController.observeServiceBinding()
            .toMainThread()
            .subscribe { onServiceBindEvent(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mPlayerDisposable.dispose()
        mTimeHandler?.removeMessages(REFRESH_TIME)
        mTimeHandler = null
    }

    private class TimeHandler(player: MiniPlayerView?) :
        Handler(Looper.getMainLooper()) {
        private val mAudioPlayer: WeakReference<MiniPlayerView?> = WeakReference(player)
        override fun handleMessage(msg: Message) {
            if (msg.what == REFRESH_TIME) {
                mAudioPlayer.get()?.let { it.queueNextRefresh(it.refreshCurrentTime()) }
            }
        }

    }

    companion object {
        private const val REFRESH_TIME = 1
    }

    override fun onSeekBarDrag(position: Long) {
        MusicPlaybackController.seek(position)
    }

    override fun onSeekBarMoving(position: Long) {

    }
}
