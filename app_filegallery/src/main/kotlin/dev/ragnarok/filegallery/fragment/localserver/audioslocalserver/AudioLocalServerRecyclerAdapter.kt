package dev.ragnarok.filegallery.fragment.localserver.audioslocalserver

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso3.Transformation
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.Includes.networkInterfaces
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.api.interfaces.ILocalServerApi
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.media.music.PlayerStatus
import dev.ragnarok.filegallery.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.filegallery.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.menu.options.AudioLocalServerOption
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.picasso.PicassoInstance.Companion.with
import dev.ragnarok.filegallery.picasso.transforms.PolyTransformation
import dev.ragnarok.filegallery.picasso.transforms.RoundTransformation
import dev.ragnarok.filegallery.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorPrimary
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorSecondary
import dev.ragnarok.filegallery.settings.Settings.get
import dev.ragnarok.filegallery.toColor
import dev.ragnarok.filegallery.util.DownloadWorkUtils.doDownloadAudio
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.coroutines.CancelableJob
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.filegallery.util.toast.CustomSnackbars
import dev.ragnarok.filegallery.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.filegallery.view.WeakViewAnimatorAdapter
import dev.ragnarok.filegallery.view.natives.animation.ThorVGLottieView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.regex.Pattern

class AudioLocalServerRecyclerAdapter(
    private val mContext: Context,
    private var data: List<Audio>
) : RecyclerView.Adapter<AudioLocalServerRecyclerAdapter.AudioHolder>() {
    private val mAudioInteractor: ILocalServerApi
    private var mClickListener: ClickListener? = null
    private var mPlayerDisposable = CancelableJob()
    private var audioListDisposable = CancelableJob()
    private var currAudio: Audio?
    private val isAudio_round_icon: Boolean = get().main().isAudio_round_icon
    fun setItems(data: List<Audio>) {
        this.data = data
        notifyDataSetChanged()
    }

    private fun doBitrate(url: String): Flow<Int> {
        return flow {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(url, HashMap())
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (bitrate != null) {
                emit((bitrate.toLong() / 1000).toInt())
            } else {
                throw Throwable("Can't receipt bitrate ")
            }
        }
    }

    private fun getBitrate(url: String?, size: Int) {
        if (url.isNullOrEmpty()) {
            return
        }
        audioListDisposable.set(
            doBitrate(url).fromIOToMain(
                {
                    createCustomToast(
                        mContext, null
                    )?.showToast(
                        mContext.resources.getString(
                            R.string.bitrate,
                            it,
                            Utils.BytesToSize(size.toLong())
                        )
                    )
                }
            ) {
                createCustomToast(mContext, null)?.setDuration(Toast.LENGTH_LONG)
                    ?.showToastThrowable(it)
            })
    }

    @get:DrawableRes
    private val audioCoverSimple: Int
        get() = if (get().main()
                .isAudio_round_icon
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
        when (MusicPlaybackController.playerStatus()) {
            1 -> {
                Utils.doWavesLottie(holder.visual, true)
                holder.play_cover.setColorFilter("#44000000".toColor())
            }

            2 -> {
                Utils.doWavesLottie(holder.visual, false)
                holder.play_cover.setColorFilter("#44000000".toColor())
            }
        }
    }

    private fun updateDownloadState(holder: AudioHolder, audio: Audio) {
        holder.saved.visibility =
            if (audio.downloadIndicator) View.VISIBLE else View.GONE
    }

    private fun doMenu(holder: AudioHolder, position: Int, view: View, audio: Audio) {
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(
            OptionRequest(
                AudioLocalServerOption.save_item_audio,
                mContext.getString(R.string.download),
                R.drawable.save,
                true
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalServerOption.play_item_audio,
                mContext.getString(R.string.play),
                R.drawable.play,
                true
            )
        )
        if (MusicPlaybackController.canPlayAfterCurrent(audio)) {
            menus.add(
                OptionRequest(
                    AudioLocalServerOption.play_item_after_current_audio,
                    mContext.getString(R.string.play_audio_after_current),
                    R.drawable.play_next,
                    false
                )
            )
        }
        menus.add(
            OptionRequest(
                AudioLocalServerOption.bitrate_item_audio,
                mContext.getString(R.string.get_bitrate),
                R.drawable.high_quality,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalServerOption.delete_item_audio,
                mContext.getString(R.string.delete),
                R.drawable.ic_outline_delete,
                true
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalServerOption.update_time_item_audio,
                mContext.getString(R.string.update_time),
                R.drawable.ic_recent,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalServerOption.edit_item_audio,
                mContext.getString(R.string.edit),
                R.drawable.about_writed,
                true
            )
        )
        menus.header(
            Utils.firstNonEmptyString(audio.artist, " ") + " - " + audio.title,
            R.drawable.song,
            audio.thumb_image
        )
        menus.columns(2)
        menus.show(
            (mContext as FragmentActivity).supportFragmentManager,
            "audio_options"
        ) { _, option ->
            when (option.id) {
                AudioLocalServerOption.save_item_audio -> {
                    audio.downloadIndicator = true
                    updateDownloadState(holder, audio)
                    when (doDownloadAudio(mContext, audio, false)) {
                        0 -> {
                            createCustomToast(
                                mContext, view
                            )?.showToast(R.string.saved_audio)
                        }

                        1 -> {
                            CustomSnackbars.createCustomSnackbars(view)
                                ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                                ?.themedSnack(
                                    R.string.audio_force_download
                                )?.setAction(
                                    R.string.button_yes
                                ) { doDownloadAudio(mContext, audio, true) }
                                ?.show()
                        }

                        else -> {
                            audio.downloadIndicator = false
                            updateDownloadState(holder, audio)
                            createCustomToast(
                                mContext, view
                            )?.showToastError(R.string.error_audio)
                        }
                    }
                }

                AudioLocalServerOption.play_item_audio -> if (mClickListener != null) {
                    mClickListener?.onClick(position, audio)
                    if (get().main()
                            .isShow_mini_player
                    ) getPlayerPlace().tryOpenWith(mContext)
                }

                AudioLocalServerOption.play_item_after_current_audio -> MusicPlaybackController.playAfterCurrent(
                    audio
                )

                AudioLocalServerOption.bitrate_item_audio -> getBitrate(
                    audio.url,
                    audio.duration
                )

                AudioLocalServerOption.update_time_item_audio -> {
                    val hash = parseLocalServerURL(audio.url)
                    if (hash.isNullOrEmpty()) {
                        return@show
                    }
                    audioListDisposable.set(
                        mAudioInteractor.update_time(hash)
                            .fromIOToMain(
                                {
                                    createCustomToast(
                                        mContext, view
                                    )?.showToast(R.string.success)
                                }) {
                                createCustomToast(
                                    mContext,
                                    view
                                )?.setDuration(Toast.LENGTH_LONG)?.showToastThrowable(it)
                            })
                }

                AudioLocalServerOption.edit_item_audio -> {
                    val hash2 = parseLocalServerURL(audio.url)
                    if (hash2.isNullOrEmpty()) {
                        return@show
                    }
                    audioListDisposable.set(
                        mAudioInteractor.get_file_name(hash2)
                            .fromIOToMain(
                                { t ->
                                    val root =
                                        View.inflate(mContext, R.layout.entry_file_name, null)
                                    root.findViewById<TextInputEditText>(R.id.edit_file_name)
                                        .setText(
                                            t
                                        )
                                    MaterialAlertDialogBuilder(mContext)
                                        .setTitle(R.string.change_name)
                                        .setCancelable(true)
                                        .setView(root)
                                        .setPositiveButton(R.string.button_ok) { _, _ ->
                                            audioListDisposable.set(
                                                mAudioInteractor.update_file_name(
                                                    hash2,
                                                    root.findViewById<TextInputEditText>(R.id.edit_file_name).text.toString()
                                                        .trim()
                                                )
                                                    .fromIOToMain({
                                                        createCustomToast(
                                                            mContext, view
                                                        )?.showToast(
                                                            R.string.success
                                                        )
                                                    }) {
                                                        createCustomToast(
                                                            mContext,
                                                            view
                                                        )?.setDuration(Toast.LENGTH_LONG)
                                                            ?.showToastThrowable(it)
                                                    })
                                        }
                                        .setNegativeButton(R.string.button_cancel, null)
                                        .show()
                                }) {
                                createCustomToast(
                                    mContext,
                                    view
                                )?.setDuration(Toast.LENGTH_LONG)?.showToastThrowable(it)
                            })
                }

                AudioLocalServerOption.delete_item_audio -> MaterialAlertDialogBuilder(
                    mContext
                )
                    .setMessage(R.string.do_delete)
                    .setTitle(R.string.confirmation)
                    .setCancelable(true)
                    .setPositiveButton(R.string.button_yes) { _, _ ->
                        val hash1 = parseLocalServerURL(audio.url)
                        if (hash1.isNullOrEmpty()) {
                            return@setPositiveButton
                        }
                        audioListDisposable.set(
                            mAudioInteractor.delete_media(hash1)
                                .fromIOToMain(
                                    {
                                        createCustomToast(
                                            mContext, view
                                        )?.showToast(R.string.success)
                                    }) {
                                    createCustomToast(
                                        mContext,
                                        view
                                    )?.setDuration(Toast.LENGTH_LONG)?.showToastThrowable(it)
                                })
                    }
                    .setNegativeButton(R.string.button_cancel, null)
                    .show()

                else -> {}
            }
        }
    }

    private fun doPlay(position: Int, audio: Audio) {
        if (MusicPlaybackController.isNowPlayingOrPreparingOrPaused(audio)) {
            if (!get().main().isUse_stop_audio) {
                MusicPlaybackController.playOrPause()
            } else {
                MusicPlaybackController.stop()
            }
        } else {
            mClickListener?.onClick(position, audio)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioHolder {
        return AudioHolder(
            LayoutInflater.from(mContext).inflate(R.layout.item_audio_local_server, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AudioHolder, position: Int) {
        val audio = data[position]
        holder.cancelSelectionAnimation()
        if (audio.isAnimationNow) {
            holder.startSelectionAnimation()
            audio.isAnimationNow = false
        }
        holder.artist.text = audio.artist
        holder.title.text = audio.title
        if (audio.duration <= 0) holder.time.visibility = View.INVISIBLE else {
            holder.time.visibility = View.VISIBLE
            holder.time.text = Utils.BytesToSize(audio.duration.toLong())
        }
        updateDownloadState(holder, audio)
        updateAudioStatus(holder, audio)
        if (audio.thumb_image.nonNullNoEmpty()) {
            ResourcesCompat.getDrawable(
                mContext.resources,
                audioCoverSimple,
                mContext.theme
            )?.let {
                with()
                    .load(audio.thumb_image)
                    .placeholder(
                        it
                    )
                    .transform(transformCover)
                    .tag(Constants.PICASSO_TAG)
                    .into(holder.play_cover)
            }
        } else {
            with().cancelRequest(holder.play_cover)
            holder.play_cover.setImageResource(audioCoverSimple)
        }
        holder.play.setOnClickListener {
            if (get().main().isRevert_play_audio) {
                doMenu(holder, holder.bindingAdapterPosition, it, audio)
            } else {
                doPlay(holder.bindingAdapterPosition, audio)
            }
        }
        holder.Track.setOnLongClickListener { v: View? ->
            audio.downloadIndicator = true
            updateDownloadState(holder, audio)
            when (doDownloadAudio(mContext, audio, false)) {
                0 -> {
                    createCustomToast(
                        mContext, holder.Track
                    )?.showToast(R.string.saved_audio)
                }

                1 -> {
                    CustomSnackbars.createCustomSnackbars(v)
                        ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                        ?.themedSnack(
                            R.string.audio_force_download,
                        )?.setAction(
                            R.string.button_yes
                        ) { doDownloadAudio(mContext, audio, true) }?.show()
                }

                else -> {
                    audio.downloadIndicator = false
                    updateDownloadState(holder, audio)
                    createCustomToast(
                        mContext, holder.Track
                    )?.showToastError(R.string.error_audio)
                }
            }
            true
        }
        holder.Track.setOnClickListener {
            holder.cancelSelectionAnimation()
            holder.startSomeAnimation()
            if (get().main().isRevert_play_audio) {
                doPlay(holder.bindingAdapterPosition, audio)
            } else {
                doMenu(holder, holder.bindingAdapterPosition, it, audio)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mPlayerDisposable.set(
            MusicPlaybackController.observeServiceBinding()
                .sharedFlowToMain { status -> onServiceBindEvent(status) })
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mPlayerDisposable.cancel()
        audioListDisposable.cancel()
    }

    private fun onServiceBindEvent(@PlayerStatus status: Int) {
        when (status) {
            PlayerStatus.UPDATE_TRACK_INFO, PlayerStatus.SERVICE_KILLED, PlayerStatus.UPDATE_PLAY_PAUSE -> {
                updateAudio(currAudio)
                currAudio = MusicPlaybackController.currentAudio
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
    }

    inner class AudioHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val artist: TextView = itemView.findViewById(R.id.dialog_title)
        val title: TextView = itemView.findViewById(R.id.dialog_message)
        val play: View = itemView.findViewById(R.id.item_audio_play)
        val play_cover: ImageView = itemView.findViewById(R.id.item_audio_play_cover)
        val Track: View = itemView.findViewById(R.id.track_option)
        val saved: ImageView = itemView.findViewById(R.id.saved)
        val selectionView: MaterialCardView = itemView.findViewById(R.id.item_audio_selection)
        private val animationAdapter: Animator.AnimatorListener =
            object : WeakViewAnimatorAdapter<View?>(selectionView) {
                override fun onAnimationEnd(view: View?) {
                    view?.visibility = View.GONE
                }

                override fun onAnimationStart(view: View?) {
                    view?.visibility = View.VISIBLE
                }

                override fun onAnimationCancel(view: View?) {
                    view?.visibility = View.GONE
                }
            }
        val visual: ThorVGLottieView = itemView.findViewById(R.id.item_audio_visual)
        val time: TextView = itemView.findViewById(R.id.item_audio_time)
        var animator: ObjectAnimator? = null
        fun startSelectionAnimation() {
            selectionView.setCardBackgroundColor(getColorPrimary(mContext))
            selectionView.alpha = 0.5f
            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f)
            animator?.duration = 1500
            animator?.addListener(animationAdapter)
            animator?.start()
        }

        fun startSomeAnimation() {
            selectionView.setCardBackgroundColor(getColorSecondary(mContext))
            selectionView.alpha = 0.5f
            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f)
            animator?.duration = 500
            animator?.addListener(animationAdapter)
            animator?.start()
        }

        fun cancelSelectionAnimation() {
            if (animator != null) {
                animator?.cancel()
                animator = null
            }
            selectionView.visibility = View.INVISIBLE
        }
    }

    companion object {
        private val PATTERN_FENRIR_SERVER_TRACK_HASH = Pattern.compile("hash=([^&]*)")
        fun parseLocalServerURL(string: String?): String? {
            string ?: return null
            val matcher = PATTERN_FENRIR_SERVER_TRACK_HASH.matcher(string)
            try {
                if (matcher.find()) {
                    return matcher.group(1)
                }
            } catch (_: NumberFormatException) {
            }
            return null
        }
    }

    init {
        currAudio = MusicPlaybackController.currentAudio
        mAudioInteractor = networkInterfaces.localServerApi()
    }
}
