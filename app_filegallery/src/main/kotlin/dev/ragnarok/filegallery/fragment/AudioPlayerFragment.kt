package dev.ragnarok.filegallery.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso3.BitmapTarget
import com.squareup.picasso3.Picasso
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.materialpopupmenu.popupMenu
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.media.music.PlayerStatus
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.picasso.PicassoInstance
import dev.ragnarok.filegallery.picasso.transforms.BlurTransformation
import dev.ragnarok.filegallery.settings.CurrentTheme
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.toColor
import dev.ragnarok.filegallery.util.DownloadWorkUtils.TrackIsDownloaded
import dev.ragnarok.filegallery.util.DownloadWorkUtils.doDownloadAudio
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.coroutines.CancelableJob
import dev.ragnarok.filegallery.util.coroutines.CompositeJob
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.toMain
import dev.ragnarok.filegallery.util.toast.CustomSnackbars
import dev.ragnarok.filegallery.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.filegallery.view.CustomSeekBar
import dev.ragnarok.filegallery.view.media.AudioPlayerBackgroundDrawable
import dev.ragnarok.filegallery.view.media.AudioPlayerCoverDrawable
import dev.ragnarok.filegallery.view.media.PlayPauseButton
import dev.ragnarok.filegallery.view.media.RepeatButton
import dev.ragnarok.filegallery.view.media.RepeatingImageButton
import dev.ragnarok.filegallery.view.media.ShuffleButton
import dev.ragnarok.filegallery.view.natives.animation.ThorVGLottieShapeableView
import kotlinx.coroutines.Job
import kotlin.math.min

class AudioPlayerFragment : BottomSheetDialogFragment(), CustomSeekBar.CustomSeekBarListener {
    private val PLAYER_TAG = "PicassoPlayerTag"

    // Play and pause button
    private var mPlayPauseButton: PlayPauseButton? = null

    // Repeat button
    private var mRepeatButton: RepeatButton? = null

    // Shuffle button
    private var mShuffleButton: ShuffleButton? = null

    // Current time
    private var mCurrentTime: TextView? = null

    // Total time
    private var mTotalTime: TextView? = null

    // Progress
    private var mProgress: CustomSeekBar? = null

    // VK Additional action
    private var ivVolumePlus: ImageView? = null
    private var ivVolumeMinus: ImageView? = null
    private var ivSave: RepeatingImageButton? = null
    private var tvTitle: TextView? = null
    private var tvSubtitle: TextView? = null
    private var ivCoverPager: ViewPager2? = null
    private var ivBackground: View? = null

    // Handler used to update the current time
    private var mRefreshJob = CancelableJob()
    private var mStartSeekPos: Long = 0
    private var mLastSeekEventTime: Long = 0
    private var coverAdapter: CoverAdapter? = null
    private lateinit var mPlayerProgressStrings: Array<String>
    private var currentPage = -1
    private var playDispose = CancelableJob()
    private var isDragging = false

    private val requestEqualizer = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    /**
     * Used to scan backwards through the track
     */
    private val mRewindListener = object : RepeatingImageButton.RepeatListener {
        override fun onRepeat(v: View, duration: Long, repeatcount: Int) {
            scanBackward(
                repeatcount,
                duration
            )
        }

    }

    /**
     * Used to scan ahead through the track
     */
    private val mFastForwardListener = object : RepeatingImageButton.RepeatListener {
        override fun onRepeat(v: View, duration: Long, repeatcount: Int) {
            scanForward(
                repeatcount,
                duration
            )
        }

    }
    private val mCompositeJob = CompositeJob()
    private fun appendJob(job: Job) {
        mCompositeJob.add(job)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity(), theme)
        val behavior = dialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        return dialog
    }

    private fun onServiceBindEvent(@PlayerStatus status: Int) {
        when (status) {
            PlayerStatus.UPDATE_TRACK_INFO -> {
                updatePlaybackControls()
                updateNowPlayingInfo()
                resolveControlViews()
            }

            PlayerStatus.UPDATE_PLAY_PAUSE -> {
                updatePlaybackControls()
                resolveTotalTime()
                resolveControlViews()
                ivBackground?.let {
                    if (it.background is Animatable) {
                        (it.background as Animatable).apply {
                            if (MusicPlaybackController.isPlaying) {
                                start()
                            } else {
                                stop()
                            }
                        }
                    }
                }
            }

            PlayerStatus.REPEATMODE_CHANGED -> {
                mRepeatButton?.updateRepeatState()
            }

            PlayerStatus.SHUFFLEMODE_CHANGED -> {
                mShuffleButton?.updateShuffleState()
                updateCovers()
            }

            PlayerStatus.UPDATE_PLAY_LIST -> {
                updateCovers()
            }

            PlayerStatus.SERVICE_KILLED -> {
                updatePlaybackControls()
                updateNowPlayingInfo()
                resolveControlViews()
                updateCovers()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_audio_player, container, false)
        mPlayerProgressStrings = resources.getStringArray(R.array.player_progress_state)
        mProgress = root.findViewById(R.id.seek_player_pos)
        mPlayPauseButton = root.findViewById(R.id.action_button_play)
        mShuffleButton = root.findViewById(R.id.action_button_shuffle)
        mRepeatButton = root.findViewById(R.id.action_button_repeat)
        val mAdditional = root.findViewById<ImageView>(R.id.goto_button)
        mAdditional.setOnClickListener {
            it.popupMenu(requireActivity()) {
                section {
                    if (isEqualizerAvailable) {
                        item(R.string.equalizer) {
                            icon = R.drawable.preferences
                            iconColor = CurrentTheme.getColorSecondary(requireActivity())
                            onSelect {
                                startEffectsPanel()
                            }
                        }
                    }
                    item(R.string.copy_track_info) {
                        icon = R.drawable.content_copy
                        iconColor = CurrentTheme.getColorSecondary(requireActivity())
                        onSelect {
                            val clipboard =
                                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                            var Artist =
                                if (MusicPlaybackController.albumName != null) MusicPlaybackController.albumName else ""
                            if (MusicPlaybackController.albumName != null) Artist += " (" + MusicPlaybackController.albumName + ")"
                            val Name =
                                if (MusicPlaybackController.trackName != null) MusicPlaybackController.trackName else ""
                            val clip = ClipData.newPlainText("response", "$Artist - $Name")
                            clipboard?.setPrimaryClip(clip)
                            createCustomToast(
                                requireActivity(),
                                view
                            )?.showToast(R.string.copied_to_clipboard)
                        }
                    }
                }
            }
        }
        val mPreviousButton: RepeatingImageButton = root.findViewById(R.id.action_button_previous)
        val mNextButton: RepeatingImageButton = root.findViewById(R.id.action_button_next)
        ivCoverPager = root.findViewById(R.id.cover_pager)
        ivBackground = root.findViewById(R.id.cover_background)
        mCurrentTime = root.findViewById(R.id.audio_player_current_time)
        mTotalTime = root.findViewById(R.id.audio_player_total_time)
        tvTitle = root.findViewById(R.id.audio_player_title)
        tvSubtitle = root.findViewById(R.id.audio_player_subtitle)

        coverAdapter = CoverAdapter()
        ivCoverPager?.adapter = coverAdapter
        ivCoverPager?.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().player_cover_transform
            )
        )
        ivCoverPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (currentPage != position) {
                    currentPage = position
                    playDispose.cancel()
                    playDispose += delayTaskFlow(400)
                        .toMain { MusicPlaybackController.skip(position) }
                    ivCoverPager?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }
        })

        //to animate running text
        tvTitle?.isSelected = true
        tvSubtitle?.isSelected = true
        mPreviousButton.setRepeatListener(mRewindListener)
        mNextButton.setRepeatListener(mFastForwardListener)
        mProgress?.setCustomSeekBarListener(this)
        ivSave = root.findViewById(R.id.audio_save)
        ivSave?.setOnClickListener {
            onSaveButtonClick(it)
        }
        ivVolumePlus = root.findViewById(R.id.volume_plus)
        ivVolumePlus?.setOnClickListener {
            val audio =
                requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            audio?.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audio.getStreamVolume(AudioManager.STREAM_MUSIC) + 1,
                0
            )
        }
        ivVolumeMinus = root.findViewById(R.id.volume_minus)
        ivVolumeMinus?.setOnClickListener {
            val audio =
                requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            audio?.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audio.getStreamVolume(AudioManager.STREAM_MUSIC) - 1,
                0
            )
        }

        appendJob(
            MusicPlaybackController.observeServiceBinding()
                .sharedFlowToMain { onServiceBindEvent(it) }
        )
        return root
    }

    @SuppressLint("ShowToast")
    private fun onSaveButtonClick(v: View) {
        val audio = MusicPlaybackController.currentAudio ?: return
        when (doDownloadAudio(
            requireActivity(),
            audio,
            false
        )) {
            0 -> {
                createCustomToast(requireActivity(), view)?.showToast(R.string.saved_audio)
                ivSave?.setImageResource(R.drawable.succ)
            }

            1 -> {
                CustomSnackbars.createCustomSnackbars(v, mPlayPauseButton)
                    ?.setDurationSnack(Snackbar.LENGTH_LONG)
                    ?.themedSnack(R.string.audio_force_download)?.setAction(
                        R.string.button_yes
                    ) {
                        doDownloadAudio(
                            requireActivity(),
                            audio,
                            true
                        )
                    }?.show()
            }

            else -> createCustomToast(requireActivity(), view)?.showToast(R.string.error_audio)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set the playback drawables
        updatePlaybackControls()
        // Current info
        updateCovers()
        updateNowPlayingInfo()

        resolveControlViews()
    }

    /**
     * {@inheritDoc}
     */
    override fun onStart() {
        super.onStart()
        // Refresh the current time
        val next = refreshCurrentTime()
        queueNextRefresh(next)
    }

    /**
     * {@inheritDoc}
     */
    override fun onDestroy() {
        playDispose.cancel()
        mCompositeJob.cancel()
        mRefreshJob.cancel()
        PicassoInstance.with().cancelTag(PLAYER_TAG)
        super.onDestroy()
    }

    private fun updateCovers() {
        coverAdapter?.updateAudios(MusicPlaybackController.queue)
    }

    val target = object : BitmapTarget {
        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
            if (isAdded) {
                ivBackground?.let {
                    AudioPlayerBackgroundDrawable.setBitmap(
                        it,
                        bitmap,
                        MusicPlaybackController.isPlaying,
                        CurrentTheme.getColorSurface(requireActivity())
                    )
                }
            }
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        }

        override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
            if (isAdded) {
                if (ivBackground?.background is Animatable) {
                    (ivBackground?.background as Animatable).stop()
                }
                ivBackground?.background = null
            }
        }
    }

    /**
     * Sets the track name, album name, and album art.
     */
    private fun updateNowPlayingInfo() {
        val audioTrack = MusicPlaybackController.currentAudio
        tvTitle?.text = audioTrack?.artist
        tvSubtitle?.text = audioTrack?.title

        if (Settings.get().main().isPlayer_Has_Background) {
            val coverUrl = audioTrack?.thumb_image
            if (coverUrl != null) {
                PicassoInstance.with()
                    .load(coverUrl)
                    .tag(PLAYER_TAG)
                    .transform(
                        BlurTransformation(
                            Settings.get().main().playerCoverBackgroundSettings.blur.toFloat(),
                            requireActivity()
                        )
                    )
                    .into(target)
            } else {
                PicassoInstance.with().cancelRequest(target)
                if (ivBackground?.background is Animatable) {
                    (ivBackground?.background as Animatable).stop()
                }
                ivBackground?.background = null
            }
        }

        MusicPlaybackController.currentAudioPos?.let {
            currentPage = it
            ivCoverPager?.setCurrentItem(it, false)
        }

        val current = MusicPlaybackController.currentAudio
        if (current != null) {
            when {
                TrackIsDownloaded(current) -> {
                    ivSave?.setImageResource(R.drawable.succ)
                }

                current.url.isNullOrEmpty() -> {
                    ivSave?.setImageResource(R.drawable.audio_died)
                }

                else -> ivSave?.setImageResource(R.drawable.save)
            }
        } else ivSave?.setImageResource(R.drawable.save)

        // Set the total time
        resolveTotalTime()
    }

    private fun resolveTotalTime() {
        if (!isAdded || mTotalTime == null) {
            return
        }
        if (MusicPlaybackController.isInitialized) {
            mTotalTime?.text =
                MusicPlaybackController.makeTimeString(
                    requireActivity(),
                    MusicPlaybackController.duration() / 1000
                )
        }
    }

    /**
     * Sets the correct drawable states for the playback controls.
     */
    private fun updatePlaybackControls() {
        if (!isAdded) {
            return
        }

        // Set the play and pause image
        mPlayPauseButton?.updateState()

        // Set the shuffle image
        mShuffleButton?.updateShuffleState()

        // Set the repeat image
        mRepeatButton?.updateRepeatState()
    }

    private fun startEffectsPanel() {
        try {
            val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            effects.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireActivity().packageName)
            effects.putExtra(
                AudioEffect.EXTRA_AUDIO_SESSION,
                MusicPlaybackController.audioSessionId
            )
            effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            requestEqualizer.launch(effects)
        } catch (_: ActivityNotFoundException) {
            view?.let {
                CustomSnackbars.createCustomSnackbars(view, mPlayPauseButton)
                    ?.setDurationSnack(Snackbar.LENGTH_LONG)
                    ?.coloredSnack(R.string.no_system_equalizer, "#eeff0000".toColor())
                    ?.show()
            }
        }
    }

    private val isEqualizerAvailable: Boolean
        get() {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            val manager = requireActivity().packageManager
            val info =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) manager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                ) else manager.queryIntentActivities(intent, 0)
            return info.isNotEmpty()
        }

    /**
     * @param delay When to update
     */
    private fun queueNextRefresh(delay: Long) {
        mRefreshJob.cancel()
        mRefreshJob += delayTaskFlow(delay)
            .toMain {
                queueNextRefresh(refreshCurrentTime())
            }
    }

    private fun resolveControlViews() {
        if (!isAdded || mProgress == null) return
        val preparing = MusicPlaybackController.isPreparing
        val initialized = MusicPlaybackController.isInitialized
        mProgress?.isEnabled = !preparing && initialized
        //mProgress?.isIndeterminate = preparing
    }

    /**
     * Used to scan backwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param deltal  The long press duration
     */
    internal fun scanBackward(repcnt: Int, deltal: Long) {
        var delta = deltal
        if (MusicPlaybackController.mService == null) {
            return
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicPlaybackController.position()
            mLastSeekEventTime = 0
        } else {
            delta = if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta * 10
            } else {
                // seek at 40x after that
                50000 + (delta - 5000) * 40
            }
            var newpos = mStartSeekPos - delta
            if (newpos < 0) {
                // move to previous track
                MusicPlaybackController.previous(requireActivity())
                val duration = MusicPlaybackController.duration()
                mStartSeekPos += duration
                newpos += duration
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicPlaybackController.seek(newpos)
                mLastSeekEventTime = delta
            }
            refreshCurrentTime()
        }
    }

    /**
     * Used to scan forwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param deltal  The long press duration
     */
    internal fun scanForward(repcnt: Int, deltal: Long) {
        var delta = deltal
        if (MusicPlaybackController.mService == null) {
            return
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicPlaybackController.position()
            mLastSeekEventTime = 0
        } else {
            delta = if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta * 10
            } else {
                // seek at 40x after that
                50000 + (delta - 5000) * 40
            }
            var newpos = mStartSeekPos + delta
            val duration = MusicPlaybackController.duration()
            if (newpos >= duration) {
                // move to next track
                MusicPlaybackController.next()
                mStartSeekPos -= duration // is OK to go negative
                newpos -= duration
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicPlaybackController.seek(newpos)
                mLastSeekEventTime = delta
            }
            refreshCurrentTime()
        }
    }

    private fun refreshCurrentTimeText(pos: Long) {
        mCurrentTime?.text = MusicPlaybackController.makeTimeString(requireActivity(), pos / 1000)
    }

    @SuppressLint("SetTextI18n")
    private fun refreshCurrentTime(): Long {
        if (!MusicPlaybackController.isInitialized) {
            mCurrentTime?.text = "--:--"
            mTotalTime?.text = "--:--"
            mProgress?.updateFullState(-1, -1, -1)
            return 500
        }
        try {
            val pos = MusicPlaybackController.position()
            val duration = MusicPlaybackController.duration()
            if (pos >= 0 && duration > 0) {
                if (!isDragging) {
                    refreshCurrentTimeText(pos)
                }
                mProgress?.updateFullState(duration, pos, MusicPlaybackController.bufferPosition())
                when {
                    MusicPlaybackController.isPlaying -> {
                        mCurrentTime?.visibility = View.VISIBLE
                    }

                    else -> {
                        // blink the counter
                        val vis = mCurrentTime?.visibility
                        mCurrentTime?.visibility =
                            if (vis == View.INVISIBLE) View.VISIBLE else View.INVISIBLE
                        return 500
                    }
                }
            } else {
                mCurrentTime?.text = "--:--"
                mProgress?.updateFullState(-1, -1, -1)
                val current = if (mTotalTime?.tag == null) 0 else mTotalTime?.tag as Int
                val next = if (current == mPlayerProgressStrings.size - 1) 0 else current + 1
                mTotalTime?.tag = next
                mTotalTime?.text = mPlayerProgressStrings[next]
                return 500
            }

            // calculate the number of milliseconds until the next full second,
            // so
            // the counter can be updated at just the right time
            val remaining = duration - pos % duration

            // approximate how often we would need to refresh the slider to
            // move it smoothly
            var width = mProgress?.width ?: 0
            if (width == 0) {
                width = 320
            }
            val smoothrefreshtime = duration / width
            if (smoothrefreshtime > remaining) {
                return min(remaining, 500)
            }
            return if (smoothrefreshtime < 20) {
                20
            } else min(smoothrefreshtime, 500)
        } catch (_: Exception) {
        }
        return 500
    }

    private inner class CoverViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ThorVGLottieShapeableView = view.findViewById(R.id.cover)

        val holderTarget = object : BitmapTarget {
            override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                if (isAdded) {
                    ivCover.scaleType = ImageView.ScaleType.FIT_START
                    AudioPlayerCoverDrawable.setBitmap(ivCover, bitmap)
                }
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }

            override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
                if (isAdded) {
                    ivCover.scaleType = ImageView.ScaleType.CENTER
                    if (FenrirNative.isNativeLoaded) {
                        ivCover.fromRes(
                            R.raw.auidio_no_cover,
                            intArrayOf(
                                0x333333,
                                CurrentTheme.getColorSurface(requireActivity()),
                                0x777777,
                                CurrentTheme.getColorOnSurface(requireActivity())
                            )
                        )
                        ivCover.startAnimation()
                    } else {
                        ivCover.setImageResource(R.drawable.itunes)
                        ivCover.drawable?.setTint(
                            CurrentTheme.getColorOnSurface(
                                requireActivity()
                            )
                        )
                    }
                }
            }
        }

        fun bind(audioTrack: Audio) {
            val coverUrl = audioTrack.thumb_image
            if (coverUrl != null) {
                PicassoInstance.with()
                    .load(coverUrl)
                    .tag(PLAYER_TAG)
                    .into(holderTarget)
            } else {
                PicassoInstance.with().cancelRequest(holderTarget)
                ivCover.scaleType = ImageView.ScaleType.CENTER
                if (FenrirNative.isNativeLoaded) {
                    ivCover.fromRes(
                        R.raw.auidio_no_cover,
                        intArrayOf(
                            0x333333,
                            CurrentTheme.getColorSurface(requireActivity()),
                            0x777777,
                            CurrentTheme.getColorOnSurface(requireActivity())
                        )
                    )
                    ivCover.startAnimation()
                } else {
                    ivCover.setImageResource(R.drawable.itunes)
                    ivCover.drawable.setTint(CurrentTheme.getColorOnSurface(requireActivity()))
                }
            }
        }
    }

    private inner class CoverAdapter : RecyclerView.Adapter<CoverViewHolder>() {
        private val mAudios = ArrayList<Audio>()

        @SuppressLint("ClickableViewAccessibility")
        override fun onCreateViewHolder(container: ViewGroup, viewType: Int): CoverViewHolder {
            return CoverViewHolder(
                LayoutInflater.from(container.context)
                    .inflate(R.layout.player_cover_picture, container, false)
            )
        }

        @SuppressLint("NotifyDataSetChanged")
        fun updateAudios(audios: List<Audio>?) {
            mAudios.clear()
            if (audios.nonNullNoEmpty()) {
                mAudios.addAll(audios)
            }
            notifyDataSetChanged()
        }

        override fun onViewDetachedFromWindow(holder: CoverViewHolder) {
            super.onViewDetachedFromWindow(holder)
            PicassoInstance.with().cancelRequest(holder.ivCover)
        }

        override fun onBindViewHolder(holder: CoverViewHolder, position: Int) {
            val audio = mAudios[position]
            holder.bind(audio)
        }

        override fun getItemCount(): Int {
            return mAudios.size
        }
    }

    override fun onSeekBarDrag(position: Long) {
        isDragging = false
        MusicPlaybackController.seek(position)
    }

    override fun onSeekBarMoving(position: Long) {
        if (!isDragging) {
            isDragging = true
        }
        refreshCurrentTimeText(position)
    }
}
