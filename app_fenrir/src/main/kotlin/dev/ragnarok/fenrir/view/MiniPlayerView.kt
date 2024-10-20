package dev.ragnarok.fenrir.view

import android.annotation.SuppressLint
import android.content.Context
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
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.media.music.PlayerStatus
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.PolyTransformation
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import dev.ragnarok.fenrir.view.media.MediaActionDrawable

class MiniPlayerView : FrameLayout, CustomSeekBar.CustomSeekBarListener {
    private var mPlayerDisposable = CancelableJob()
    private var mAccountDisposable = CancelableJob()
    private var mRefreshDisposable = CancelableJob()
    private var mAccountId = 0L
    private lateinit var visual: ImageView
    private lateinit var playCover: ImageView
    private lateinit var title: TextView
    private lateinit var mProgress: CustomSeekBar
    private lateinit var root: View
    private lateinit var mVisualDrawable: MediaActionDrawable

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
        if (isInEditMode) {
            return
        }
        mVisualDrawable = MediaActionDrawable()
        root = LayoutInflater.from(context).inflate(R.layout.mini_player, this)
        val play = root.findViewById<View>(R.id.item_audio_play)
        playCover = root.findViewById(R.id.item_audio_play_cover)
        visual = root.findViewById(R.id.item_audio_visual)
        root.visibility =
            if (MusicPlaybackController.miniPlayerVisibility) VISIBLE else GONE
        val mPClosePlay = root.findViewById<ImageButton>(R.id.close_player)
        mPClosePlay.setOnClickListener {
            MusicPlaybackController.closeMiniPlayer()
            root.visibility = GONE
        }
        mPClosePlay.setOnLongClickListener {
            MusicPlaybackController.stop()
            true
        }
        mVisualDrawable.setIcon(MediaActionDrawable.ICON_EMPTY, false)
        visual.setImageDrawable(mVisualDrawable)
        play.setOnClickListener {
            MusicPlaybackController.playOrPause()
            if (MusicPlaybackController.isPlaying) {
                mVisualDrawable.setIcon(MediaActionDrawable.ICON_PAUSE, true)
                playCover.setColorFilter("#44000000".toColor())
            } else {
                mVisualDrawable.setIcon(MediaActionDrawable.ICON_PLAY, true)
                playCover.clearColorFilter()
            }
        }
        play.setOnLongClickListener {
            MusicPlaybackController.next()
            true
        }
        val mOpenPlayer = root.findViewById<ImageButton>(R.id.open_player)
        mOpenPlayer.setOnClickListener {
            PlaceFactory.getPlayerPlace(mAccountId).tryOpenWith(
                context
            )
        }
        title = root.findViewById(R.id.mini_artist)
        title.isSelected = true
        mProgress = root.findViewById(R.id.seek_player_pos)
        mProgress.setCustomSeekBarListener(this)
    }

    private fun queueNextRefresh() {
        mRefreshDisposable.cancel()
        mRefreshDisposable.set(delayTaskFlow(500)
            .toMain {
                refreshCurrentTime()
                queueNextRefresh()
            }
        )
    }

    private val transformCover: Transformation
        get() = if (Settings.get()
                .main().isAudio_round_icon
        ) RoundTransformation() else PolyTransformation()

    @get:DrawableRes
    private val audioCoverSimple: Int
        get() = if (Settings.get()
                .main().isAudio_round_icon
        ) R.drawable.audio_button else R.drawable.audio_button_material

    private fun updatePlaybackControls() {
        if (MusicPlaybackController.isPlaying) {
            mVisualDrawable.setIcon(MediaActionDrawable.ICON_PAUSE, true)
            playCover.setColorFilter("#44000000".toColor())
        } else {
            mVisualDrawable.setIcon(MediaActionDrawable.ICON_PLAY, true)
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
        if (audio != null && placeholder != null && audio.thumb_image_little.nonNullNoEmpty()) {
            with()
                .load(audio.thumb_image_little)
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

    private fun refreshCurrentTime() {
        if (!MusicPlaybackController.isInitialized) {
            mProgress.updateFullState(-1, -1, -1)
            return
        }
        try {
            val pos = MusicPlaybackController.position()
            val duration = MusicPlaybackController.duration()
            if (pos >= 0 && duration > 0) {
                mProgress.updateFullState(duration, pos, MusicPlaybackController.bufferPosition())
                if (!MusicPlaybackController.isPlaying) {
                    return
                }
            } else {
                mProgress.updateFullState(-1, -1, -1)
                return
            }
        } catch (_: Exception) {
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInEditMode) {
            return
        }
        receiveFullAudioInfo()
        mAccountId = Settings.get()
            .accounts()
            .current
        mAccountDisposable.set(Settings.get()
            .accounts()
            .observeChanges
            .sharedFlowToMain { mAccountId = it })
        refreshCurrentTime()
        queueNextRefresh()
        mPlayerDisposable.set(MusicPlaybackController.observeServiceBinding()
            .sharedFlowToMain { onServiceBindEvent(it) })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isInEditMode) {
            return
        }
        mPlayerDisposable.cancel()
        mAccountDisposable.cancel()
        mRefreshDisposable.cancel()
    }

    override fun onSeekBarDrag(position: Long) {
        MusicPlaybackController.seek(position)
    }

    override fun onSeekBarMoving(position: Long) {

    }
}
