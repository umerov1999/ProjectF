package dev.ragnarok.fenrir.fragment.audio.audios

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.fragment.search.SearchContentType
import dev.ragnarok.fenrir.fragment.search.criteria.AudioSearchCriteria
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.canPlayAfterCurrent
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.currentAudio
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.isNowPlayingOrPreparingOrPaused
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.observeServiceBinding
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.playAfterCurrent
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.playOrPause
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.playerStatus
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.stop
import dev.ragnarok.fenrir.media.music.PlayerStatus
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.Option
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.menu.options.AudioOption
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.PolyTransformation
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.place.PlaceFactory.SearchByAudioPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getArtistPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getAudiosInAlbumPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.place.PlaceFactory.getSingleTabSearchPlace
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.toMainThread
import dev.ragnarok.fenrir.util.AppPerms.hasReadWriteStoragePermission
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.DownloadWorkUtils.TrackIsDownloaded
import dev.ragnarok.fenrir.util.DownloadWorkUtils.doDownloadAudio
import dev.ragnarok.fenrir.util.Mp3InfoHelper.getBitrate
import dev.ragnarok.fenrir.util.Mp3InfoHelper.getLength
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.hls.M3U8
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.WeakViewAnimatorAdapter
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import io.reactivex.rxjava3.disposables.Disposable

class AudioRecyclerAdapter(
    context: Context,
    data: MutableList<Audio>,
    private val not_show_my: Boolean,
    iSSelectMode: Boolean,
    playlist_id: Int?
) : RecyclerBindableAdapter<Audio, AudioRecyclerAdapter.AudioHolder>(data) {
    private val mContext: Context = context
    private val mAudioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
    private val playlist_id: Int?
    private val isLongPressDownload: Boolean = Settings.get().main().isUse_long_click_download
    private val isAudio_round_icon: Boolean = Settings.get().main().isAudio_round_icon
    private var audioListDisposable = Disposable.disposed()
    private var mPlayerDisposable = Disposable.disposed()
    private var iSSelectMode: Boolean
    private var mClickListener: ClickListener? = null
    private var currAudio: Audio?
    internal fun deleteTrack(accountId: Long, audio: Audio, position: Int) {
        audioListDisposable = if (playlist_id == null) {
            mAudioInteractor.delete(accountId, audio.id, audio.ownerId).fromIOToMain().subscribe(
                {
                    createCustomToast(mContext).showToast(R.string.deleted)
                    mClickListener?.onDelete(position)
                }) { t -> createCustomToast(mContext).showToastThrowable(t) }
        } else {
            mAudioInteractor.removeFromPlaylist(
                accountId, audio.ownerId, playlist_id, listOf(
                    AccessIdPair(
                        audio.id, audio.ownerId, audio.accessKey
                    )
                )
            ).fromIOToMain().subscribe(
                { t: Int ->
                    if (t != 0) {
                        createCustomToast(mContext).showToast(R.string.deleted)
                        mClickListener?.onDelete(position)
                    }
                }) { t -> createCustomToast(mContext).showToastThrowable(t) }
        }
    }

    internal fun addTrack(accountId: Long, audio: Audio) {
        audioListDisposable = mAudioInteractor.add(accountId, audio, null).fromIOToMain().subscribe(
            { createCustomToast(mContext).showToast(R.string.added) }) { t ->
            createCustomToast(mContext).showToastThrowable(t)
        }
    }

    internal fun getMp3AndBitrate(accountId: Long, audio: Audio) {
        val mode = audio.needRefresh()
        if (mode.first) {
            audioListDisposable =
                mAudioInteractor.getByIdOld(accountId, listOf(audio), mode.second).fromIOToMain()
                    .subscribe({ t -> getBitrate(t[0]) }) {
                        getBitrate(
                            audio
                        )
                    }
        } else {
            getBitrate(audio)
        }
    }

    private fun getBitrate(audio: Audio) {
        val pUrl = audio.url
        if (pUrl.isNullOrEmpty()) {
            return
        }
        audioListDisposable = if (audio.isHLS) {
            M3U8(pUrl).length.fromIOToMain()
                .subscribe(
                    { r: Long ->
                        createCustomToast(mContext).showToast(
                            getBitrate(
                                mContext,
                                audio.duration,
                                r
                            )
                        )
                    }
                ) { e -> createCustomToast(mContext).showToastThrowable(e) }
        } else {
            getLength(pUrl).fromIOToMain()
                .subscribe(
                    { r: Long ->
                        createCustomToast(mContext).showToast(
                            getBitrate(
                                mContext,
                                audio.duration,
                                r
                            )
                        )
                    }
                ) { e -> createCustomToast(mContext).showToastThrowable(e) }
        }
    }

    fun toggleSelectMode(iSSelectMode: Boolean) {
        this.iSSelectMode = iSSelectMode
    }

    internal fun get_lyrics(audio: Audio) {
        audioListDisposable =
            mAudioInteractor.getLyrics(Settings.get().accounts().current, audio)
                .fromIOToMain()
                .subscribe({ t ->
                    onAudioLyricsReceived(
                        t,
                        audio
                    )
                }) { t -> createCustomToast(mContext).showToastThrowable(t) }
    }

    private fun onAudioLyricsReceived(Text: String, audio: Audio) {
        val title = audio.artistAndTitle
        MaterialAlertDialogBuilder(mContext)
            .setIcon(R.drawable.dir_song)
            .setMessage(Text)
            .setTitle(title)
            .setPositiveButton(R.string.button_ok, null)
            .setNeutralButton(R.string.copy_text) { _: DialogInterface?, _: Int ->
                val clipboard = mContext.getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager?
                val clip = ClipData.newPlainText("response", Text)
                clipboard?.setPrimaryClip(clip)
                createCustomToast(mContext).showToast(R.string.copied_to_clipboard)
            }
            .setCancelable(true)
            .show()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mPlayerDisposable = observeServiceBinding()
            .toMainThread()
            .subscribe { onServiceBindEvent(it) }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mPlayerDisposable.dispose()
        audioListDisposable.dispose()
    }

    private fun onServiceBindEvent(@PlayerStatus status: Int) {
        when (status) {
            PlayerStatus.UPDATE_TRACK_INFO, PlayerStatus.SERVICE_KILLED, PlayerStatus.UPDATE_PLAY_PAUSE -> {
                updateAudio(currAudio)
                currAudio = currentAudio
                updateAudio(currAudio)
            }

            PlayerStatus.REPEATMODE_CHANGED, PlayerStatus.SHUFFLEMODE_CHANGED, PlayerStatus.UPDATE_PLAY_LIST -> {}
        }
    }

    private fun updateAudio(audio: Audio?) {
        audio ?: return
        val pos = indexOfAdapter(audio)
        if (pos != -1) {
            notifyItemChanged(pos)
        }
    }

    /*
    private void onServiceBindEvent(@PlayerStatus int status) {
        switch (status) {
            case PlayerStatus.UPDATE_TRACK_INFO:
                Audio old = currAudio;
                currAudio = MusicPlaybackController.getCurrentAudio();
                if (!Objects.equals(old, currAudio)) {
                    updateAudio(old);
                    updateAudio(currAudio);
                }
                break;
            case PlayerStatus.UPDATE_PLAY_PAUSE:
                updateAudio(currAudio);
                break;
            case PlayerStatus.SERVICE_KILLED:
                Audio del = currAudio;
                currAudio = null;
                if (del != null) {
                    updateAudio(del);
                }
                break;
            case PlayerStatus.REPEATMODE_CHANGED:
            case PlayerStatus.SHUFFLEMODE_CHANGED:
            case PlayerStatus.UPDATE_PLAY_LIST:
                break;
        }
    }
     */
    @get:DrawableRes
    private val audioCoverSimple: Int
        get() = if (isAudio_round_icon) R.drawable.audio_button else R.drawable.audio_button_material

    private val transformCover: Transformation by lazy {
        if (isAudio_round_icon) RoundTransformation() else PolyTransformation()
    }

    private fun updateAudioStatus(holder: AudioHolder, audio: Audio) {
        if (audio != currAudio) {
            holder.visual.setImageResource(audio.songIcon)
            holder.play_cover.clearColorFilter()
            return
        }
        when (playerStatus()) {
            1 -> {
                Utils.doWavesLottie(holder.visual, true)
                holder.play_cover.setColorFilter(Color.parseColor("#44000000"))
            }

            2 -> {
                Utils.doWavesLottie(holder.visual, false)
                holder.play_cover.setColorFilter(Color.parseColor("#44000000"))
            }
        }
    }

    internal fun updateDownloadState(holder: AudioHolder, audio: Audio) {
        if (audio.downloadIndicator == 2) {
            holder.saved.setImageResource(R.drawable.remote_cloud)
            Utils.setColorFilter(holder.saved, CurrentTheme.getColorSecondary(mContext))
        } else {
            holder.saved.setImageResource(R.drawable.save)
            Utils.setColorFilter(holder.saved, CurrentTheme.getColorPrimary(mContext))
        }
        holder.saved.visibility =
            if (audio.downloadIndicator != 0) View.VISIBLE else View.GONE
    }

    private fun doMenu(holder: AudioHolder, position: Int, view: View, audio: Audio) {
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(
            OptionRequest(
                AudioOption.play_item_audio,
                mContext.getString(R.string.play),
                R.drawable.play,
                true
            )
        )
        if (canPlayAfterCurrent(audio)) {
            menus.add(
                OptionRequest(
                    AudioOption.play_item_after_current_audio,
                    mContext.getString(R.string.play_audio_after_current),
                    R.drawable.play_next,
                    false
                )
            )
        }
        if (audio.ownerId != Settings.get().accounts().current) {
            menus.add(
                OptionRequest(
                    AudioOption.add_item_audio,
                    mContext.getString(R.string.action_add),
                    R.drawable.list_add,
                    true
                )
            )
            menus.add(
                OptionRequest(
                    AudioOption.add_and_download_button,
                    mContext.getString(R.string.add_and_download_button),
                    R.drawable.add_download,
                    false
                )
            )
        } else {
            if (playlist_id == null) {
                menus.add(
                    OptionRequest(
                        AudioOption.add_item_audio,
                        mContext.getString(R.string.delete),
                        R.drawable.ic_outline_delete,
                        true
                    )
                )
            } else {
                menus.add(
                    OptionRequest(
                        AudioOption.add_item_audio,
                        mContext.getString(R.string.delete_from_playlist),
                        R.drawable.ic_outline_delete,
                        false
                    )
                )
            }
            menus.add(
                OptionRequest(
                    AudioOption.edit_track,
                    mContext.getString(R.string.edit),
                    R.drawable.about_writed,
                    true
                )
            )
        }
        menus.add(
            OptionRequest(
                AudioOption.share_button,
                mContext.getString(R.string.share),
                R.drawable.ic_outline_share,
                true
            )
        )
        menus.add(
            OptionRequest(
                AudioOption.save_item_audio,
                mContext.getString(R.string.save),
                R.drawable.save,
                true
            )
        )
        if (audio.albumId != 0) menus.add(
            OptionRequest(
                AudioOption.open_album,
                mContext.getString(R.string.open_album),
                R.drawable.audio_album,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioOption.get_recommendation_by_audio,
                mContext.getString(R.string.get_recommendation_by_audio),
                R.drawable.music_mic,
                false
            )
        )
        if (audio.main_artists.nonNullNoEmpty()) menus.add(
            OptionRequest(
                AudioOption.goto_artist,
                mContext.getString(R.string.audio_goto_artist),
                R.drawable.artist_icon,
                false
            )
        )
        if (audio.lyricsId != 0) menus.add(
            OptionRequest(
                AudioOption.get_lyrics_menu,
                mContext.getString(R.string.get_lyrics_menu),
                R.drawable.lyric,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioOption.bitrate_item_audio,
                mContext.getString(R.string.get_bitrate),
                R.drawable.high_quality,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioOption.search_by_artist,
                mContext.getString(R.string.search_by_artist),
                R.drawable.magnify,
                true
            )
        )
        menus.add(
            OptionRequest(
                AudioOption.copy_url,
                mContext.getString(R.string.copy_url),
                R.drawable.content_copy,
                false
            )
        )
        menus.header(
            Utils.firstNonEmptyString(audio.artist, " ") + " - " + audio.title,
            R.drawable.song,
            audio.thumb_image_little
        )
        menus.columns(2)
        menus.show(
            (mContext as FragmentActivity).supportFragmentManager,
            "audio_options",
            object : ModalBottomSheetDialogFragment.Listener {
                override fun onModalOptionSelected(option: Option) {
                    when (option.id) {
                        AudioOption.play_item_audio -> {
                            mClickListener?.onClick(position, audio)
                            if (Settings.get().other().isShow_mini_player) getPlayerPlace(
                                Settings.get().accounts().current
                            ).tryOpenWith(mContext)
                        }

                        AudioOption.play_item_after_current_audio -> playAfterCurrent(
                            audio
                        )

                        AudioOption.edit_track -> {
                            mClickListener?.onEdit(position, audio)
                        }

                        AudioOption.share_button -> startForSendAttachments(
                            mContext,
                            Settings.get().accounts().current,
                            audio
                        )

                        AudioOption.search_by_artist -> getSingleTabSearchPlace(
                            Settings.get().accounts().current,
                            SearchContentType.AUDIOS,
                            AudioSearchCriteria(
                                audio.artist, by_artist = true, in_main_page = false
                            )
                        ).tryOpenWith(mContext)

                        AudioOption.get_lyrics_menu -> get_lyrics(audio)
                        AudioOption.get_recommendation_by_audio -> SearchByAudioPlace(
                            Settings.get().accounts().current, audio.ownerId, audio.id
                        ).tryOpenWith(mContext)

                        AudioOption.open_album -> getAudiosInAlbumPlace(
                            Settings.get().accounts().current,
                            audio.album_owner_id,
                            audio.albumId,
                            audio.album_access_key
                        ).tryOpenWith(mContext)

                        AudioOption.copy_url -> {
                            val clipboard =
                                mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                            val clip = ClipData.newPlainText("response", audio.url)
                            clipboard?.setPrimaryClip(clip)
                            createCustomToast(mContext).showToast(R.string.copied)
                        }

                        AudioOption.add_item_audio -> {
                            val myAudio = audio.ownerId == Settings.get().accounts().current
                            if (myAudio) {
                                deleteTrack(Settings.get().accounts().current, audio, position)
                            } else {
                                addTrack(Settings.get().accounts().current, audio)
                            }
                        }

                        AudioOption.add_and_download_button -> {
                            if (audio.ownerId != Settings.get().accounts().current) {
                                addTrack(Settings.get().accounts().current, audio)
                            }
                            if (!hasReadWriteStoragePermission(mContext)) {
                                mClickListener?.onRequestWritePermissions()
                                return
                            }
                            audio.downloadIndicator = 1
                            updateDownloadState(holder, audio)
                            val ret = doDownloadAudio(
                                mContext,
                                audio,
                                Settings.get().accounts().current,
                                Force = false,
                                isLocal = false
                            )
                            when (ret) {
                                0 -> createCustomToast(mContext).showToastBottom(R.string.saved_audio)
                                1, 2 -> {
                                    CustomSnackbars.createCustomSnackbars(view)
                                        ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                                        ?.themedSnack(if (ret == 1) R.string.audio_force_download else R.string.audio_force_download_pc)
                                        ?.setAction(
                                            R.string.button_yes
                                        ) {
                                            doDownloadAudio(
                                                mContext,
                                                audio,
                                                Settings.get().accounts().current,
                                                Force = true,
                                                isLocal = false
                                            )
                                        }
                                        ?.show()
                                }

                                else -> {
                                    audio.downloadIndicator = 0
                                    updateDownloadState(holder, audio)
                                    createCustomToast(mContext).showToastBottom(R.string.error_audio)
                                }
                            }
                        }

                        AudioOption.save_item_audio -> {
                            if (!hasReadWriteStoragePermission(mContext)) {
                                mClickListener?.onRequestWritePermissions()
                                return
                            }
                            audio.downloadIndicator = 1
                            updateDownloadState(holder, audio)
                            val ret = doDownloadAudio(
                                mContext,
                                audio,
                                Settings.get().accounts().current,
                                Force = false,
                                isLocal = false
                            )
                            when (ret) {
                                0 -> createCustomToast(mContext).showToastBottom(R.string.saved_audio)
                                1, 2 -> {
                                    CustomSnackbars.createCustomSnackbars(view)
                                        ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                                        ?.themedSnack(if (ret == 1) R.string.audio_force_download else R.string.audio_force_download_pc)
                                        ?.setAction(
                                            R.string.button_yes
                                        ) {
                                            doDownloadAudio(
                                                mContext,
                                                audio,
                                                Settings.get().accounts().current,
                                                Force = true,
                                                isLocal = false
                                            )
                                        }
                                        ?.show()
                                }

                                else -> {
                                    audio.downloadIndicator = 0
                                    updateDownloadState(holder, audio)
                                    createCustomToast(mContext).showToastBottom(R.string.error_audio)
                                }
                            }
                        }

                        AudioOption.bitrate_item_audio -> getMp3AndBitrate(
                            Settings.get().accounts().current, audio
                        )

                        AudioOption.goto_artist -> {
                            val artists = Utils.getArrayFromHash(
                                audio.main_artists
                            )
                            if (audio.main_artists?.keys?.size.orZero() > 1) {
                                MaterialAlertDialogBuilder(mContext)
                                    .setItems(artists[1]) { _: DialogInterface?, which: Int ->
                                        getArtistPlace(
                                            Settings.get().accounts().current,
                                            artists[0][which]
                                        ).tryOpenWith(mContext)
                                    }
                                    .show()
                            } else {
                                getArtistPlace(
                                    Settings.get().accounts().current,
                                    artists[0][0]
                                ).tryOpenWith(mContext)
                            }
                        }
                    }
                }
            })
    }

    private fun doPlay(position: Int, audio: Audio) {
        if (isNowPlayingOrPreparingOrPaused(audio)) {
            if (!Settings.get().other().isUse_stop_audio) {
                playOrPause()
            } else {
                stop()
            }
        } else {
            mClickListener?.onClick(position, audio)
        }
    }

    override fun onBindItemViewHolder(viewHolder: AudioHolder, position: Int, type: Int) {
        val audio = getItem(position)
        viewHolder.cancelSelectionAnimation()
        if (audio.isAnimationNow) {
            viewHolder.startSelectionAnimation()
            audio.isAnimationNow = false
        }
        viewHolder.artist.text = audio.artist
        if (!audio.isLocal && !audio.isLocalServer && Constants.DEFAULT_ACCOUNT_TYPE == AccountType.VK_ANDROID && !audio.isHLS) {
            viewHolder.quality.visibility = View.VISIBLE
            if (audio.isHq) {
                viewHolder.quality.setImageResource(R.drawable.high_quality)
            } else {
                viewHolder.quality.setImageResource(R.drawable.low_quality)
            }
        } else {
            viewHolder.quality.visibility = View.GONE
        }
        viewHolder.title.text = audio.title
        if (audio.duration <= 0) viewHolder.time.visibility = View.INVISIBLE else {
            viewHolder.time.visibility = View.VISIBLE
            if (audio.isLocalServer) {
                viewHolder.time.text = Utils.BytesToSize(audio.duration.toLong())
            } else {
                viewHolder.time.text = AppTextUtils.getDurationString(audio.duration)
            }
        }
        viewHolder.lyric.visibility = if (audio.lyricsId != 0) View.VISIBLE else View.GONE
        viewHolder.isSelectedView.visibility = if (audio.isSelected) View.VISIBLE else View.GONE
        if (audio.isSelected) {
            when {
                audio.url.isNullOrEmpty() -> {
                    viewHolder.isSelectedView.setCardBackgroundColor(Color.parseColor("#ff0000"))
                }

                TrackIsDownloaded(audio) != 0 -> {
                    viewHolder.isSelectedView.setCardBackgroundColor(Color.parseColor("#00aa00"))
                }

                else -> {
                    viewHolder.isSelectedView.setCardBackgroundColor(
                        CurrentTheme.getColorPrimary(
                            mContext
                        )
                    )
                }
            }
        }
        if (not_show_my) viewHolder.my.visibility = View.GONE else viewHolder.my.visibility =
            if (audio.ownerId == Settings.get().accounts().current) View.VISIBLE else View.GONE
        viewHolder.saved.visibility = View.GONE
        updateDownloadState(viewHolder, audio)
        updateAudioStatus(viewHolder, audio)
        if (audio.thumb_image_little.nonNullNoEmpty()) {
            with()
                .load(audio.thumb_image_little)
                .placeholder(
                    ResourcesCompat.getDrawable(
                        mContext.resources,
                        audioCoverSimple,
                        mContext.theme
                    ) ?: return
                )
                .transform(transformCover)
                .tag(Constants.PICASSO_TAG)
                .into(viewHolder.play_cover)
        } else {
            with().cancelRequest(viewHolder.play_cover)
            viewHolder.play_cover.setImageResource(audioCoverSimple)
        }
        viewHolder.play.setOnLongClickListener {
            if ((audio.thumb_image_very_big.nonNullNoEmpty()
                        || audio.thumb_image_big.nonNullNoEmpty() || audio.thumb_image_little.nonNullNoEmpty()) && audio.artist.nonNullNoEmpty() && audio.title.nonNullNoEmpty()
            ) {
                Utils.firstNonEmptyString(
                    audio.thumb_image_very_big,
                    audio.thumb_image_big, audio.thumb_image_little
                )?.let {
                    audio.artist?.let { it1 ->
                        audio.title?.let { it2 ->
                            mClickListener?.onUrlPhotoOpen(
                                it, it1, it2
                            )
                        }
                    }
                }
            }
            true
        }
        viewHolder.play.setOnClickListener { v: View ->
            if (Settings.get().main().isRevert_play_audio) {
                doMenu(viewHolder, getItemRawPosition(viewHolder.bindingAdapterPosition), v, audio)
            } else {
                doPlay(getItemRawPosition(viewHolder.bindingAdapterPosition), audio)
            }
        }
        if (!iSSelectMode) {
            if (isLongPressDownload) {
                viewHolder.Track.setOnLongClickListener { v: View? ->
                    if (!hasReadWriteStoragePermission(mContext)) {
                        mClickListener?.onRequestWritePermissions()
                        return@setOnLongClickListener false
                    }
                    audio.downloadIndicator = 1
                    updateDownloadState(viewHolder, audio)
                    val ret = doDownloadAudio(
                        mContext,
                        audio,
                        Settings.get().accounts().current,
                        Force = false,
                        isLocal = false
                    )
                    when (ret) {
                        0 -> createCustomToast(mContext).showToastBottom(R.string.saved_audio)
                        1, 2 -> {
                            v?.let {
                                CustomSnackbars.createCustomSnackbars(it)
                                    ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                                    ?.themedSnack(if (ret == 1) R.string.audio_force_download else R.string.audio_force_download_pc)
                                    ?.setAction(
                                        R.string.button_yes
                                    ) {
                                        doDownloadAudio(
                                            mContext,
                                            audio,
                                            Settings.get().accounts().current,
                                            Force = true,
                                            isLocal = false
                                        )
                                    }
                                    ?.show()
                            }
                        }

                        else -> {
                            audio.downloadIndicator = 0
                            updateDownloadState(viewHolder, audio)
                            createCustomToast(mContext).showToastBottom(R.string.error_audio)
                        }
                    }
                    true
                }
            }
            viewHolder.Track.setOnClickListener { view: View ->
                viewHolder.cancelSelectionAnimation()
                viewHolder.startSomeAnimation()
                if (Settings.get().main().isRevert_play_audio) {
                    doPlay(getItemRawPosition(viewHolder.bindingAdapterPosition), audio)
                } else {
                    doMenu(
                        viewHolder,
                        getItemRawPosition(viewHolder.bindingAdapterPosition),
                        view,
                        audio
                    )
                }
            }
        } else {
            viewHolder.Track.setOnClickListener {
                audio.isSelected = !audio.isSelected
                viewHolder.isSelectedView.visibility =
                    if (audio.isSelected) View.VISIBLE else View.GONE
                when {
                    audio.url.isNullOrEmpty() -> {
                        viewHolder.isSelectedView.setCardBackgroundColor(Color.parseColor("#ff0000"))
                    }

                    TrackIsDownloaded(audio) != 0 -> {
                        viewHolder.isSelectedView.setCardBackgroundColor(Color.parseColor("#00aa00"))
                    }

                    else -> {
                        viewHolder.isSelectedView.setCardBackgroundColor(
                            CurrentTheme.getColorPrimary(
                                mContext
                            )
                        )
                    }
                }
            }
        }
    }

    override fun viewHolder(view: View, type: Int): AudioHolder {
        return AudioHolder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_audio
    }

    fun setData(data: MutableList<Audio>) {
        setItems(data)
    }

    fun setClickListener(clickListener: ClickListener?) {
        mClickListener = clickListener
    }

    interface ClickListener {
        fun onClick(position: Int, audio: Audio)
        fun onEdit(position: Int, audio: Audio)
        fun onDelete(position: Int)
        fun onUrlPhotoOpen(url: String, prefix: String, photo_prefix: String)
        fun onRequestWritePermissions()
    }

    inner class AudioHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val artist: TextView = itemView.findViewById(R.id.dialog_title)
        val title: TextView = itemView.findViewById(R.id.dialog_message)
        val play: View = itemView.findViewById(R.id.item_audio_play)
        val play_cover: ImageView = itemView.findViewById(R.id.item_audio_play_cover)
        val visual: RLottieImageView = itemView.findViewById(R.id.item_audio_visual)
        val time: TextView = itemView.findViewById(R.id.item_audio_time)
        val saved: ImageView = itemView.findViewById(R.id.saved)
        val lyric: ImageView = itemView.findViewById(R.id.lyric)
        val my: ImageView = itemView.findViewById(R.id.my)
        val quality: ImageView = itemView.findViewById(R.id.quality)
        val Track: View = itemView.findViewById(R.id.track_option)
        val selectionView: MaterialCardView = itemView.findViewById(R.id.item_audio_selection)
        val isSelectedView: MaterialCardView = itemView.findViewById(R.id.item_audio_select_add)
        private val animationAdapter: Animator.AnimatorListener
        var animator: ObjectAnimator? = null
        fun startSelectionAnimation() {
            selectionView.setCardBackgroundColor(CurrentTheme.getColorPrimary(mContext))
            selectionView.alpha = 0.5f
            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f)
            animator?.duration = 1500
            animator?.addListener(animationAdapter)
            animator?.start()
        }

        fun startSomeAnimation() {
            selectionView.setCardBackgroundColor(CurrentTheme.getColorSecondary(mContext))
            selectionView.alpha = 0.5f
            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f)
            animator?.duration = 500
            animator?.addListener(animationAdapter)
            animator?.start()
        }

        fun cancelSelectionAnimation() {
            animator?.cancel()
            animator = null
            selectionView.visibility = View.INVISIBLE
        }

        init {
            animationAdapter = object : WeakViewAnimatorAdapter<View>(selectionView) {
                override fun onAnimationEnd(view: View) {
                    view.visibility = View.GONE
                }

                override fun onAnimationStart(view: View) {
                    view.visibility = View.VISIBLE
                }

                override fun onAnimationCancel(view: View) {
                    view.visibility = View.GONE
                }
            }
        }
    }

    init {
        this.iSSelectMode = iSSelectMode
        this.playlist_id = playlist_id
        currAudio = currentAudio
    }
}