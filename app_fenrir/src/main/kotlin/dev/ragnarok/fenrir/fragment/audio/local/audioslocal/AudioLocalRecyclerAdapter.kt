package dev.ragnarok.fenrir.fragment.audio.local.audioslocal

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getString
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
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.menu.options.AudioLocalOption
import dev.ragnarok.fenrir.module.FileUtils
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.PolyTransformation
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.toMainThread
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.WeakViewAnimatorAdapter
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.disposables.Disposable
import java.io.File

class AudioLocalRecyclerAdapter(private val mContext: Context, private var data: List<Audio>) :
    RecyclerView.Adapter<AudioLocalRecyclerAdapter.AudioHolder>() {
    private var mClickListener: ClickListener? = null
    private var mPlayerDisposable = Disposable.disposed()
    private var audioListDisposable = Disposable.disposed()
    private var currAudio: Audio?
    private val isAudio_round_icon: Boolean = Settings.get().main().isAudio_round_icon
    fun setItems(data: List<Audio>) {
        this.data = data
        notifyDataSetChanged()
    }

    @Suppress("DEPRECATION")
    private fun doLocalBitrate(url: String): Single<Pair<Int, Long>> {
        return Single.create { v: SingleEmitter<Pair<Int, Long>> ->
            try {
                val cursor = mContext.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.MediaColumns.DATA),
                    BaseColumns._ID + "=? ",
                    arrayOf(Uri.parse(url).lastPathSegment),
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val retriever = MediaMetadataRetriever()
                    val fl =
                        cursor.getString(MediaStore.MediaColumns.DATA)
                    retriever.setDataSource(fl)
                    cursor.close()
                    val bitrate =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    if (bitrate != null && fl != null) {
                        v.onSuccess(Pair((bitrate.toLong() / 1000).toInt(), File(fl).length()))
                    } else {
                        v.tryOnError(Throwable("Can't receipt bitrate "))
                    }
                } else {
                    v.tryOnError(Throwable("Can't receipt bitrate "))
                }
            } catch (e: RuntimeException) {
                v.tryOnError(e)
            }
        }
    }

    @Suppress("DEPRECATION")
    internal fun stripMetadata(url: String): Completable {
        return Completable.create {
            try {
                val cursor = mContext.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.MediaColumns.DATA),
                    BaseColumns._ID + "=? ",
                    arrayOf(Uri.parse(url).lastPathSegment),
                    null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val fl =
                        cursor.getString(MediaStore.MediaColumns.DATA)
                    cursor.close()
                    if (fl.nonNullNoEmpty()) {
                        if (FileUtils.audioTagStrip(fl)) {
                            it.onComplete()
                            return@create
                        } else {
                            it.tryOnError(Throwable("Can't strip metadata"))
                        }
                    } else {
                        it.tryOnError(Throwable("Can't find file"))
                    }
                } else {
                    it.tryOnError(Throwable("Can't find file"))
                }
            } catch (e: RuntimeException) {
                it.tryOnError(e)
            }
        }
    }

    internal fun getLocalBitrate(url: String?) {
        if (url.isNullOrEmpty()) {
            return
        }
        audioListDisposable = doLocalBitrate(url).fromIOToMain()
            .subscribe(
                { r: Pair<Int, Long> ->
                    createCustomToast(mContext).showToast(
                        mContext.resources.getString(
                            R.string.bitrate,
                            r.first,
                            Utils.BytesToSize(r.second)
                        )
                    )
                }
            ) { e -> createCustomToast(mContext).showToastThrowable(e) }
    }

    @get:DrawableRes
    private val audioCoverSimple: Int
        get() = if (Settings.get()
                .main().isAudio_round_icon
        ) R.drawable.audio_button else R.drawable.audio_button_material

    private val transformCover: Transformation by lazy {
        if (isAudio_round_icon) RoundTransformation() else PolyTransformation()
    }

    private fun updateAudioStatus(holder: AudioHolder, audio: Audio) {
        if (audio != currAudio) {
            holder.visual.setImageResource(if (audio.url.isNullOrEmpty()) R.drawable.audio_died else R.drawable.song)
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

    private fun doMenu(position: Int, view: View, audio: Audio) {
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(
            OptionRequest(
                AudioLocalOption.upload_item_audio,
                mContext.getString(R.string.upload),
                R.drawable.web,
                true
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.play_item_audio,
                mContext.getString(R.string.play),
                R.drawable.play,
                true
            )
        )
        if (Settings.get().other().localServer.enabled) {
            menus.add(
                OptionRequest(
                    AudioLocalOption.play_via_local_server,
                    mContext.getString(R.string.play_remote),
                    R.drawable.remote_cloud,
                    false
                )
            )
        }
        if (canPlayAfterCurrent(audio)) {
            menus.add(
                OptionRequest(
                    AudioLocalOption.play_item_after_current_audio,
                    mContext.getString(R.string.play_audio_after_current),
                    R.drawable.play_next,
                    false
                )
            )
        }
        menus.add(
            OptionRequest(
                AudioLocalOption.bitrate_item_audio,
                mContext.getString(R.string.get_bitrate),
                R.drawable.high_quality,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.delete_item_audio,
                mContext.getString(R.string.delete),
                R.drawable.ic_outline_delete,
                true
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.strip_metadata_item_audio,
                mContext.getString(R.string.strip_metadata),
                R.drawable.ic_outline_delete,
                true
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
            "audio_options"
        ) { _, option ->
            when (option.id) {
                AudioLocalOption.upload_item_audio -> {
                    mClickListener?.onUpload(position, audio)
                }

                AudioLocalOption.play_via_local_server -> {
                    mClickListener?.onRemotePlay(position, audio)
                }

                AudioLocalOption.play_item_audio -> {
                    mClickListener?.onClick(position, audio)
                    if (Settings.get().other().isShow_mini_player) getPlayerPlace(
                        Settings.get().accounts().current
                    ).tryOpenWith(mContext)
                }

                AudioLocalOption.play_item_after_current_audio -> playAfterCurrent(
                    audio
                )

                AudioLocalOption.bitrate_item_audio -> getLocalBitrate(audio.url)
                AudioLocalOption.strip_metadata_item_audio -> {
                    audio.url?.let { it ->
                        audioListDisposable = stripMetadata(it).fromIOToMain().subscribe(
                            {
                                CustomSnackbars.createCustomSnackbars(view)
                                    ?.setDurationSnack(Snackbar.LENGTH_LONG)
                                    ?.coloredSnack(
                                        R.string.success,
                                        Color.parseColor("#AA48BE2D")
                                    )
                                    ?.show()
                            },
                            { createCustomToast(mContext).showToastError(it.localizedMessage) }
                        )
                    }
                }

                AudioLocalOption.delete_item_audio -> try {
                    if (mContext.getContentResolver()
                            .delete(Uri.parse(audio.url), null, null) == 1
                    ) {
                        CustomSnackbars.createCustomSnackbars(view)
                            ?.setDurationSnack(Snackbar.LENGTH_LONG)
                            ?.coloredSnack(R.string.success, Color.parseColor("#AA48BE2D"))
                            ?.show()
                        mClickListener?.onDelete(position)
                    }
                } catch (e: Exception) {
                    createCustomToast(mContext).showToastError(e.localizedMessage)
                }

                else -> {}
            }
        }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioHolder {
        return AudioHolder(
            LayoutInflater.from(mContext).inflate(R.layout.item_local_audio, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AudioHolder, position: Int) {
        val audio = data[position]
        holder.cancelSelectionAnimation()
        if (audio.isAnimationNow) {
            holder.startSelectionAnimation()
            audio.isAnimationNow = false
        }
        if (audio.artist.nonNullNoEmpty()) {
            holder.artist.text = audio.artist
        } else {
            holder.artist.text = mContext.getString(R.string.not_set_artist)
        }
        holder.title.text = audio.title
        if (audio.duration <= 0) holder.time.visibility = View.INVISIBLE else {
            holder.time.visibility = View.VISIBLE
            holder.time.text = AppTextUtils.getDurationString(audio.duration)
        }
        updateAudioStatus(holder, audio)
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
                .into(holder.play_cover)
        } else {
            with().cancelRequest(holder.play_cover)
            holder.play_cover.setImageResource(audioCoverSimple)
        }
        holder.play.setOnClickListener { v: View ->
            if (Settings.get().main().isRevert_play_audio) {
                doMenu(holder.bindingAdapterPosition, v, audio)
            } else {
                doPlay(holder.bindingAdapterPosition, audio)
            }
        }
        holder.Track.setOnClickListener { view: View ->
            holder.cancelSelectionAnimation()
            holder.startSomeAnimation()
            if (Settings.get().main().isRevert_play_audio) {
                doPlay(holder.bindingAdapterPosition, audio)
            } else {
                doMenu(holder.bindingAdapterPosition, view, audio)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
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
        val pos = data.indexOf(audio)
        if (pos != -1) {
            notifyItemChanged(pos)
        }
    }

    fun setClickListener(clickListener: ClickListener?) {
        mClickListener = clickListener
    }

    interface ClickListener {
        fun onClick(position: Int, audio: Audio)
        fun onDelete(position: Int)
        fun onUpload(position: Int, audio: Audio)
        fun onRemotePlay(position: Int, audio: Audio)
    }

    inner class AudioHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val artist: TextView = itemView.findViewById(R.id.dialog_title)
        val title: TextView = itemView.findViewById(R.id.dialog_message)
        val play: View = itemView.findViewById(R.id.item_audio_play)
        val play_cover: ImageView = itemView.findViewById(R.id.item_audio_play_cover)
        val Track: View = itemView.findViewById(R.id.track_option)
        val selectionView: MaterialCardView = itemView.findViewById(R.id.item_audio_selection)
        val animationAdapter: Animator.AnimatorListener
        val visual: RLottieImageView = itemView.findViewById(R.id.item_audio_visual)
        val time: TextView = itemView.findViewById(R.id.item_audio_time)
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
        currAudio = currentAudio
    }
}