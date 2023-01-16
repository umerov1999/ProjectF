package dev.ragnarok.fenrir.fragment.audio

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.*
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso3.BitmapTarget
import com.squareup.picasso3.Picasso
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.AudioSearchCriteria
import dev.ragnarok.fenrir.materialpopupmenu.MaterialPopupMenuBuilder
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.media.music.PlayerStatus
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.picasso.transforms.BlurTransformation
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.service.ErrorLocalizer
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.AppPerms.requestPermissionsAbs
import dev.ragnarok.fenrir.util.DownloadWorkUtils.TrackIsDownloaded
import dev.ragnarok.fenrir.util.DownloadWorkUtils.doDownloadAudio
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.CustomSeekBar
import dev.ragnarok.fenrir.view.media.*
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieShapeableImageView
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
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
    private var mGetLyrics: ImageView? = null

    // Progress
    private var mProgress: CustomSeekBar? = null

    // VK Additional action
    private var ivAdd: ImageView? = null
    private var ivSave: RepeatingImageButton? = null
    private var tvTitle: TextView? = null
    private var tvAlbum: TextView? = null
    private var tvSubtitle: TextView? = null
    private var ivCoverPager: ViewPager2? = null
    private var ivBackground: View? = null

    // Handler used to update the current time
    private var mTimeHandler: TimeHandler? = null
    private var mStartSeekPos: Long = 0
    private var mLastSeekEventTime: Long = 0
    private var coverAdapter: CoverAdapter? = null
    private lateinit var mPlayerProgressStrings: Array<String>
    private var currentPage = -1
    private var playDispose = Disposable.disposed()
    private var isDragging = false

    private val requestEqualizer = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        CustomSnackbars.createCustomSnackbars(view, mPlayPauseButton)
            ?.setDurationSnack(Snackbar.LENGTH_LONG)?.themedSnack(R.string.equalizer_closed)?.show()
    }

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
    private var mAudioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
    private var mAccountId = 0L
    private val mBroadcastDisposable = CompositeDisposable()
    private val mCompositeDisposable = CompositeDisposable()
    private fun appendDisposable(disposable: Disposable) {
        mCompositeDisposable.add(disposable)
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

    private val requestWriteQRPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        createCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text)
    }

    @Suppress("DEPRECATION")
    private fun fireAudioQR() {
        val audio = MusicPlaybackController.currentAudio ?: return
        val qr = Utils.generateQR(
            "https://vk.com/audio/" + audio.ownerId + "_" + audio.id,
            requireActivity()
        )
        val dlgAlert = MaterialAlertDialogBuilder(requireActivity())
        dlgAlert.setCancelable(true)
        dlgAlert.setNegativeButton(R.string.button_cancel, null)
        dlgAlert.setPositiveButton(R.string.save) { _, _ ->
            if (!AppPerms.hasReadWriteStoragePermission(requireActivity())) {
                requestWriteQRPermission.launch()
            } else {
                val path = Environment.getExternalStorageDirectory().absolutePath
                val fOutputStream: OutputStream
                val file = File(path, "qr_fenrir_audio_" + audio.ownerId + "_" + audio.id + ".png")
                try {
                    fOutputStream = FileOutputStream(file)
                    qr?.compress(Bitmap.CompressFormat.PNG, 100, fOutputStream)
                    fOutputStream.flush()
                    fOutputStream.close()
                    requireActivity().sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(file)
                        )
                    )
                    createCustomToast(requireActivity()).showToast(R.string.success)
                } catch (e: IOException) {
                    e.printStackTrace()
                    createCustomToast(requireActivity()).showToastError("Save Failed")
                }
            }
        }
        dlgAlert.setIcon(R.drawable.qr_code)
        val view: View = LayoutInflater.from(requireActivity()).inflate(R.layout.qr, null)
        dlgAlert.setTitle(R.string.show_qr)
        val imageView: ShapeableImageView = view.findViewById(R.id.qr)
        imageView.setImageBitmap(qr)
        dlgAlert.setView(view)
        dlgAlert.show()
    }

    private val requestWriteAudioPermission = requestPermissionsAbs(
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) {
        onSaveButtonClick(requireView())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_audio_player, container, false)
        mAccountId = requireArguments().getLong(Extra.ACCOUNT_ID)
        mPlayerProgressStrings = resources.getStringArray(R.array.player_progress_state)
        mProgress = root.findViewById(R.id.seek_player_pos)
        mPlayPauseButton = root.findViewById(R.id.action_button_play)
        mShuffleButton = root.findViewById(R.id.action_button_shuffle)
        mRepeatButton = root.findViewById(R.id.action_button_repeat)
        val mAdditional = root.findViewById<ImageView>(R.id.goto_button)
        mAdditional.setOnClickListener {
            val popupMenu = MaterialPopupMenuBuilder()
            popupMenu.section {
                if (isEqualizerAvailable) {
                    item {
                        labelRes = R.string.equalizer
                        icon = R.drawable.preferences
                        iconColor = CurrentTheme.getColorSecondary(requireActivity())
                        callback = {
                            startEffectsPanel()
                        }
                    }
                }
                item {
                    labelRes = R.string.playlist
                    icon = R.drawable.ic_menu_24_white
                    iconColor = CurrentTheme.getColorSecondary(requireActivity())
                    callback = {
                        val tmpList = MusicPlaybackController.queue
                        if (tmpList.nonNullNoEmpty()) {
                            PlaylistFragment.newInstance(ArrayList(tmpList))
                                .show(childFragmentManager, "audio_playlist")
                        }
                    }
                }
                item {
                    labelRes = R.string.copy_track_info
                    icon = R.drawable.content_copy
                    iconColor = CurrentTheme.getColorSecondary(requireActivity())
                    callback = {
                        val clipboard =
                            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                        var Artist =
                            if (MusicPlaybackController.artistName != null) MusicPlaybackController.artistName else ""
                        if (MusicPlaybackController.albumName != null) Artist += " (" + MusicPlaybackController.albumName + ")"
                        val Name =
                            if (MusicPlaybackController.trackName != null) MusicPlaybackController.trackName else ""
                        val clip = ClipData.newPlainText("response", "$Artist - $Name")
                        clipboard?.setPrimaryClip(clip)
                        createCustomToast(requireActivity()).showToast(R.string.copied_to_clipboard)
                    }
                }
                item {
                    labelRes = R.string.show_qr
                    icon = R.drawable.qr_code
                    iconColor = CurrentTheme.getColorSecondary(requireActivity())
                    callback = {
                        fireAudioQR()
                    }
                }
                item {
                    labelRes = R.string.search_by_artist
                    icon = R.drawable.magnify
                    iconColor = CurrentTheme.getColorSecondary(requireActivity())
                    callback = {
                        PlaceFactory.getSingleTabSearchPlace(
                            mAccountId,
                            SearchContentType.AUDIOS,
                            AudioSearchCriteria(
                                MusicPlaybackController.artistName,
                                by_artist = true,
                                in_main_page = false
                            )
                        ).tryOpenWith(requireActivity())
                        dismissAllowingStateLoss()
                    }
                }
            }
            popupMenu.build().show(requireActivity(), it)
        }
        val mPreviousButton: RepeatingImageButton = root.findViewById(R.id.action_button_previous)
        val mNextButton: RepeatingImageButton = root.findViewById(R.id.action_button_next)
        ivCoverPager = root.findViewById(R.id.cover_pager)
        ivBackground = root.findViewById(R.id.cover_background)
        mCurrentTime = root.findViewById(R.id.audio_player_current_time)
        mTotalTime = root.findViewById(R.id.audio_player_total_time)
        tvTitle = root.findViewById(R.id.audio_player_title)
        tvAlbum = root.findViewById(R.id.audio_player_album)
        tvSubtitle = root.findViewById(R.id.audio_player_subtitle)
        mGetLyrics = root.findViewById(R.id.audio_player_get_lyrics)
        mGetLyrics?.setOnClickListener { onLyrics() }

        coverAdapter = CoverAdapter()
        ivCoverPager?.adapter = coverAdapter
        ivCoverPager?.setPageTransformer(
            Utils.createPageTransform(
                Settings.get().main().player_cover_transform
            )
        )
        ivCoverPager?.offscreenPageLimit = 1
        ivCoverPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (currentPage != position) {
                    currentPage = position
                    playDispose.dispose()
                    playDispose = Observable.just(Object())
                        .delay(400, TimeUnit.MILLISECONDS)
                        .toMainThread()
                        .subscribe { MusicPlaybackController.skip(position) }
                    ivCoverPager?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            }
        })

        //to animate running text
        tvTitle?.isSelected = true
        tvSubtitle?.isSelected = true
        tvAlbum?.isSelected = true
        mPreviousButton.setRepeatListener(mRewindListener)
        mNextButton.setRepeatListener(mFastForwardListener)
        mProgress?.setCustomSeekBarListener(this)
        ivSave = root.findViewById(R.id.audio_save)
        ivSave?.setOnClickListener {
            run {
                if (!AppPerms.hasReadWriteStoragePermission(requireActivity())) {
                    requestWriteAudioPermission.launch()
                } else {
                    onSaveButtonClick(it)
                }
            }
        }
        ivAdd = root.findViewById(R.id.audio_add)
        if (Settings.get().main().isPlayer_support_volume) {
            ivAdd?.setImageResource(R.drawable.volume_minus)
            ivAdd?.setOnClickListener {
                val audio =
                    requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                audio?.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audio.getStreamVolume(AudioManager.STREAM_MUSIC) - 1,
                    0
                )
            }
        } else {
            ivAdd?.setImageResource(R.drawable.plus)
            ivAdd?.setOnClickListener { onAddButtonClick() }
        }
        val ivShare: ImageView = root.findViewById(R.id.audio_share)
        if (Settings.get().main().isPlayer_support_volume) {
            ivShare.setImageResource(R.drawable.volume_plus)
            ivShare.setOnClickListener {
                val audio =
                    requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                audio?.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audio.getStreamVolume(AudioManager.STREAM_MUSIC) + 1,
                    0
                )
            }
        } else {
            ivShare.setImageResource(R.drawable.ic_outline_share)
            ivShare.setOnClickListener { shareAudio() }
        }

        resolveAddButton()

        mTimeHandler = TimeHandler(this)
        appendDisposable(MusicPlaybackController.observeServiceBinding()
            .toMainThread()
            .subscribe { onServiceBindEvent(it) })
        return root
    }

    @SuppressLint("ShowToast")
    private fun onSaveButtonClick(v: View) {
        val audio = MusicPlaybackController.currentAudio ?: return
        when (doDownloadAudio(
            requireActivity(),
            audio,
            mAccountId,
            false,
            isLocal = audio.isLocalServer
        )) {
            0 -> {
                createCustomToast(requireActivity()).showToastBottom(R.string.saved_audio)
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
                            mAccountId,
                            true,
                            isLocal = audio.isLocalServer
                        )
                    }?.show()
            }
            2 -> {
                CustomSnackbars.createCustomSnackbars(v, mPlayPauseButton)
                    ?.setDurationSnack(Snackbar.LENGTH_LONG)
                    ?.themedSnack(R.string.audio_force_download_pc)?.setAction(
                        R.string.button_yes
                    ) {
                        doDownloadAudio(
                            requireActivity(),
                            audio,
                            mAccountId,
                            true,
                            isLocal = audio.isLocalServer
                        )
                    }?.show()
                ivSave?.setImageResource(R.drawable.succ)
            }
            else -> createCustomToast(requireActivity()).showToastBottom(R.string.error_audio)
        }
    }

    private fun onAddButtonClick() {
        val audio = MusicPlaybackController.currentAudio ?: return
        if (audio.isLocal || audio.isLocalServer) {
            createCustomToast(requireActivity()).showToastError(R.string.not_supported)
            return
        }
        if (audio.ownerId == mAccountId) {
            if (!audio.isDeleted) {
                delete(mAccountId, audio)
            } else {
                restore(mAccountId, audio)
            }
        } else {
            add(mAccountId, audio)
        }
    }

    private fun showErrorInAdapter(throwable: Throwable) {
        if (!isAdded || view == null) {
            return
        }
        CustomSnackbars.createCustomSnackbars(view)?.let {
            val snack = it.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG).coloredSnack(
                ErrorLocalizer.localizeThrowable(Includes.provideApplicationContext(), throwable),
                Color.parseColor("#eeff0000")
            )
            if (throwable !is ApiException && throwable !is SocketTimeoutException && throwable !is UnknownHostException) {
                snack.setAction(R.string.more_info) {
                    val text = StringBuilder()
                    text.append(
                        ErrorLocalizer.localizeThrowable(
                            Includes.provideApplicationContext(),
                            throwable
                        )
                    )
                    text.append("\r\n")
                    for (stackTraceElement in throwable.stackTrace) {
                        text.append("    ")
                        text.append(stackTraceElement)
                        text.append("\r\n")
                    }
                    MaterialAlertDialogBuilder(requireActivity())
                        .setIcon(R.drawable.ic_error)
                        .setMessage(text)
                        .setTitle(R.string.more_info)
                        .setPositiveButton(R.string.button_ok, null)
                        .setCancelable(true)
                        .show()
                }
            }
            snack.show()
        } ?: createCustomToast(requireActivity()).showToastThrowable(
            throwable
        )
    }

    private fun onLyrics() {
        val audio = MusicPlaybackController.currentAudio ?: return
        get_lyrics(audio)
    }

    private fun add(accountId: Long, audio: Audio) {
        appendDisposable(
            mAudioInteractor.add(accountId, audio, null)
                .fromIOToMain()
                .subscribe({ onAudioAdded() }) { showErrorInAdapter(it) })
    }

    private fun onAudioAdded() {
        createCustomToast(requireActivity()).showToast(R.string.added)
        resolveAddButton()
    }

    private fun delete(accountId: Long, audio: Audio) {
        val id = audio.id
        val ownerId = audio.ownerId
        appendDisposable(
            mAudioInteractor.delete(accountId, id, ownerId)
                .fromIOToMain()
                .subscribe({
                    onAudioDeletedOrRestored(
                        id,
                        ownerId,
                        true
                    )
                }) { showErrorInAdapter(it) })
    }

    private fun restore(accountId: Long, audio: Audio) {
        val id = audio.id
        val ownerId = audio.ownerId
        appendDisposable(
            mAudioInteractor.restore(accountId, id, ownerId)
                .fromIOToMain()
                .subscribe({
                    onAudioDeletedOrRestored(
                        id,
                        ownerId,
                        false
                    )
                }) { showErrorInAdapter(it) })
    }

    private fun get_lyrics(audio: Audio) {
        appendDisposable(
            mAudioInteractor.getLyrics(mAccountId, audio.lyricsId)
                .fromIOToMain()
                .subscribe({ Text -> onAudioLyricsReceived(Text) }) { showErrorInAdapter(it) })
    }

    private fun onAudioLyricsReceived(Text: String) {
        var title: String? = null
        if (MusicPlaybackController.currentAudio != null) title =
            MusicPlaybackController.currentAudio?.artistAndTitle
        val dlgAlert = MaterialAlertDialogBuilder(requireActivity())
        dlgAlert.setIcon(R.drawable.dir_song)
        dlgAlert.setMessage(Text)
        dlgAlert.setTitle(title ?: requireActivity().getString(R.string.get_lyrics))
        dlgAlert.setPositiveButton(R.string.button_ok, null)
        dlgAlert.setNeutralButton(requireActivity().getString(R.string.copy_text)) { _: DialogInterface, _: Int ->
            val clipboard =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText("response", Text)
            clipboard?.setPrimaryClip(clip)
            createCustomToast(requireActivity()).showToast(R.string.copied_to_clipboard)
        }
        dlgAlert.setCancelable(true)
        dlgAlert.create().show()
    }

    private fun onAudioDeletedOrRestored(id: Int, ownerId: Long, deleted: Boolean) {
        if (deleted) {
            createCustomToast(requireActivity()).showToast(R.string.deleted)
        } else {
            createCustomToast(requireActivity()).showToast(R.string.restored)
        }
        val current = MusicPlaybackController.currentAudio
        if (current != null && current.id == id && current.ownerId == ownerId) {
            current.setDeleted(deleted)
        }
        resolveAddButton()
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
        MusicPlaybackController.notifyForegroundStateChanged(
            requireActivity(),
            MusicPlaybackController.isPlaying
        )
    }

    /**
     * {@inheritDoc}
     */
    override fun onStop() {
        super.onStop()
        MusicPlaybackController.notifyForegroundStateChanged(requireActivity(), false)
    }

    /**
     * {@inheritDoc}
     */
    override fun onDestroy() {
        playDispose.dispose()
        mCompositeDisposable.dispose()
        mTimeHandler?.removeMessages(REFRESH_TIME)
        mTimeHandler = null
        mBroadcastDisposable.dispose()
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
        if (mGetLyrics != null) {
            if (audioTrack != null && audioTrack.lyricsId != 0) mGetLyrics?.visibility =
                View.VISIBLE else mGetLyrics?.visibility = View.GONE
        }
        if (tvAlbum != null) {
            var album = ""
            if (audioTrack?.album_title.nonNullNoEmpty()) album += requireActivity().getString(R.string.album) + " " + audioTrack?.album_title
            tvAlbum?.text = album
        }
        tvTitle?.text = audioTrack?.artist
        tvSubtitle?.text = audioTrack?.title

        if (Settings.get().other().isPlayer_Has_Background) {
            val coverUrl =
                firstNonEmptyString(
                    audioTrack?.thumb_image_big,
                    audioTrack?.thumb_image_little
                )
            if (coverUrl != null) {
                PicassoInstance.with()
                    .load(coverUrl)
                    .tag(PLAYER_TAG)
                    .transform(
                        BlurTransformation(
                            Settings.get().other().playerCoverBackgroundSettings.blur.toFloat(),
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

        resolveAddButton()
        val current = MusicPlaybackController.currentAudio
        if (current != null) {
            when {
                TrackIsDownloaded(current) == 1 -> {
                    ivSave?.setImageResource(R.drawable.succ)
                }
                TrackIsDownloaded(current) == 2 -> {
                    ivSave?.setImageResource(R.drawable.remote_cloud)
                }
                current.url.isNullOrEmpty() -> {
                    ivSave?.setImageResource(R.drawable.audio_died)
                }
                ("https://vk.com/mp3/audio_api_unavailable.mp3" == current.url) -> {
                    ivSave?.setImageResource(R.drawable.report)
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
        } catch (ignored: ActivityNotFoundException) {
            CustomSnackbars.createCustomSnackbars(view, mPlayPauseButton)
                ?.setDurationSnack(Snackbar.LENGTH_LONG)
                ?.coloredSnack(R.string.no_system_equalizer, Color.parseColor("#eeff0000"))?.show()
        }
    }

    @Suppress("deprecation")
    private val isEqualizerAvailable: Boolean
        get() {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            val manager = requireActivity().packageManager
            val info = if (Utils.hasTiramisu()) manager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0)
            ) else manager.queryIntentActivities(intent, 0)
            return info.size > 0
        }

    private fun shareAudio() {
        val current = MusicPlaybackController.currentAudio ?: return
        if (current.isLocal || current.isLocalServer) {
            createCustomToast(requireActivity()).showToastError(R.string.not_supported)
            return
        }
        SendAttachmentsActivity.startForSendAttachments(requireActivity(), mAccountId, current)
    }

    private fun resolveAddButton() {
        if (Settings.get().main().isPlayer_support_volume) return
        if (!isAdded) return
        val currentAudio = MusicPlaybackController.currentAudio ?: return
        //ivAdd.setVisibility(currentAudio == null ? View.INVISIBLE : View.VISIBLE);
        val myAudio = currentAudio.ownerId == mAccountId
        val icon =
            if (myAudio && !currentAudio.isDeleted) R.drawable.ic_outline_delete else R.drawable.plus
        ivAdd?.setImageResource(icon)
    }

    /**
     * @param delay When to update
     */
    internal fun queueNextRefresh(delay: Long) {
        val message = mTimeHandler?.obtainMessage(REFRESH_TIME)
        mTimeHandler?.removeMessages(REFRESH_TIME)
        if (message != null) {
            mTimeHandler?.sendMessageDelayed(message, delay)
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

    internal fun refreshCurrentTime(): Long {
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
        } catch (ignored: Exception) {
        }
        return 500
    }

    /**
     * Used to update the current time string
     */
    private class TimeHandler(player: AudioPlayerFragment) : Handler(Looper.getMainLooper()) {
        private val mAudioPlayer: WeakReference<AudioPlayerFragment> = WeakReference(player)
        override fun handleMessage(msg: Message) {
            if (msg.what == REFRESH_TIME) {
                mAudioPlayer.get()?.let { it.queueNextRefresh(it.refreshCurrentTime()) }
            }
        }

    }

    private inner class CoverViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: RLottieShapeableImageView = view.findViewById(R.id.cover)

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
                            Common.getPlayerNullArtAnimation(Settings.get().other().paganSymbol),
                            450,
                            450,
                            intArrayOf(
                                0x333333,
                                CurrentTheme.getColorSurface(requireActivity()),
                                0x777777,
                                CurrentTheme.getColorOnSurface(requireActivity())
                            )
                        )
                        ivCover.playAnimation()
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
            val coverUrl =
                firstNonEmptyString(
                    audioTrack.thumb_image_big,
                    audioTrack.thumb_image_little
                )
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
                        Common.getPlayerNullArtAnimation(Settings.get().other().paganSymbol),
                        450,
                        450,
                        intArrayOf(
                            0x333333,
                            CurrentTheme.getColorSurface(requireActivity()),
                            0x777777,
                            CurrentTheme.getColorOnSurface(requireActivity())
                        )
                    )
                    ivCover.playAnimation()
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
            if (holder.ivCover.drawable is Animatable) {
                (holder.ivCover.drawable as Animatable).stop()
            }
        }

        override fun onViewAttachedToWindow(holder: CoverViewHolder) {
            super.onViewAttachedToWindow(holder)
            if (holder.ivCover.drawable is Animatable) {
                (holder.ivCover.drawable as Animatable).start()
            }
        }

        override fun onBindViewHolder(holder: CoverViewHolder, position: Int) {
            val audio = mAudios[position]
            holder.bind(audio)
        }

        override fun getItemCount(): Int {
            return mAudios.size
        }
    }

    companion object {
        // Message to refresh the time
        private const val REFRESH_TIME = 1


        fun buildArgs(accountId: Long): Bundle {
            val bundle = Bundle()
            bundle.putLong(Extra.ACCOUNT_ID, accountId)
            return bundle
        }

        fun newInstance(accountId: Long): AudioPlayerFragment {
            return newInstance(buildArgs(accountId))
        }


        fun newInstance(args: Bundle?): AudioPlayerFragment {
            val fragment = AudioPlayerFragment()
            fragment.arguments = args
            return fragment
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
