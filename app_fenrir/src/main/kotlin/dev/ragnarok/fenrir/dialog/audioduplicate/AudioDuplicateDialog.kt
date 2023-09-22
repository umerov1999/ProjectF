package dev.ragnarok.fenrir.dialog.audioduplicate

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.fragment.base.BaseMvpDialogFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.currentAudio
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.isNowPlayingOrPreparingOrPaused
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.playOrPause
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.playerStatus
import dev.ragnarok.fenrir.media.music.MusicPlaybackController.stop
import dev.ragnarok.fenrir.media.music.MusicPlaybackService.Companion.startForPlayList
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.picasso.Content_Local
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.buildUriForPicasso
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.buildUriForPicassoNew
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.PolyTransformation
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView

class AudioDuplicateDialog :
    BaseMvpDialogFragment<AudioDuplicatePresenter, IAudioDuplicateView>(), IAudioDuplicateView {
    private var newAudio: AudioHolder? = null
    private var oldAudio: AudioHolder? = null
    private var newBitrate: TextView? = null
    private var oldBitrate: TextView? = null
    private var bBitrate: MaterialButton? = null
    private val isAudio_round_icon: Boolean = Settings.get().main().isAudio_round_icon
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = View.inflate(requireActivity(), R.layout.dialog_audio_duplicate, null)
        val dialog: Dialog = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.save)
            .setIcon(R.drawable.dir_song)
            .setView(view)
            .setPositiveButton(R.string.dual_track) { _: DialogInterface?, _: Int ->
                returnSelection(
                    true
                )
            }
            .setNegativeButton(R.string.new_track) { _: DialogInterface?, _: Int ->
                returnSelection(
                    false
                )
            }
            .setNeutralButton(R.string.button_cancel, null)
            .create()
        newAudio = AudioHolder(view.findViewById(R.id.item_new_audio))
        oldAudio = AudioHolder(view.findViewById(R.id.item_old_audio))
        newBitrate = view.findViewById(R.id.item_new_bitrate)
        oldBitrate = view.findViewById(R.id.item_old_bitrate)
        bBitrate = view.findViewById(R.id.item_get_bitrate)
        bBitrate?.setOnClickListener {
            presenter?.getBitrateAll(
                requireActivity()
            )
            bBitrate?.visibility = View.GONE
        }
        fireViewCreated()
        return dialog
    }

    private fun updateAudioStatus(holder: AudioHolder, audio: Audio) {
        if (audio != currentAudio) {
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

    private val transformCover: Transformation by lazy {
        if (isAudio_round_icon) RoundTransformation() else PolyTransformation()
    }

    @get:DrawableRes
    private val audioCoverSimple: Int
        get() = if (Settings.get()
                .main().isAudio_round_icon
        ) R.drawable.audio_button else R.drawable.audio_button_material

    private fun bind(holder: AudioHolder, audio: Audio) {
        holder.artist.text = audio.artist
        holder.title.text = audio.title
        if (audio.duration <= 0) holder.time.visibility = View.INVISIBLE else {
            holder.time.visibility = View.VISIBLE
            if (audio.isLocalServer) {
                holder.time.text = Utils.BytesToSize(audio.duration.toLong())
            } else {
                holder.time.text = AppTextUtils.getDurationString(audio.duration)
            }
        }
        updateAudioStatus(holder, audio)
        if (audio.thumb_image_little.nonNullNoEmpty()) {
            with()
                .load(audio.thumb_image_little)
                .placeholder(
                    ResourcesCompat.getDrawable(
                        requireActivity().resources,
                        audioCoverSimple,
                        requireActivity().theme
                    ) ?: return
                )
                .transform(transformCover)
                .tag(Constants.PICASSO_TAG)
                .into(holder.play_cover)
        } else {
            with().cancelRequest(holder.play_cover)
            holder.play_cover.setImageResource(audioCoverSimple)
        }
        holder.play.setOnClickListener {
            if (isNowPlayingOrPreparingOrPaused(audio)) {
                if (!Settings.get().main().isUse_stop_audio) {
                    playOrPause()
                } else {
                    stop()
                }
            } else {
                startForPlayList(requireActivity(), ArrayList(listOf(audio)), 0, false)
            }
        }
    }

    override fun displayData(new_audio: Audio, old_audio: Audio) {
        if (newAudio != null && oldAudio != null) {
            bind(newAudio ?: return, new_audio)
            bind(oldAudio ?: return, old_audio)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun setOldBitrate(bitrate: Int?) {
        if (bitrate != null) {
            oldBitrate?.visibility = View.VISIBLE
            oldBitrate?.text = "$bitrate kbps"
        } else {
            oldBitrate?.visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    override fun setNewBitrate(bitrate: Int?) {
        if (bitrate != null) {
            newBitrate?.visibility = View.VISIBLE
            newBitrate?.text = "$bitrate kbps"
        } else {
            newBitrate?.visibility = View.GONE
        }
    }

    override fun updateShowBitrate(needShow: Boolean) {
        bBitrate?.visibility = if (needShow) View.VISIBLE else View.GONE
    }

    private fun returnSelection(type: Boolean) {
        val intent = Bundle()
        intent.putBoolean(Extra.TYPE, type)
        if (arguments != null) {
            intent.putAll(arguments)
        }
        parentFragmentManager.setFragmentResult(REQUEST_CODE_AUDIO_DUPLICATE, intent)
        dismiss()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<AudioDuplicatePresenter> {
        return object : IPresenterFactory<AudioDuplicatePresenter> {
            override fun create(): AudioDuplicatePresenter {
                return AudioDuplicatePresenter(
                    requireArguments().getLong(Extra.ACCOUNT_ID),
                    requireArguments().getParcelableCompat(Extra.NEW)!!,
                    requireArguments().getParcelableCompat(Extra.OLD)!!,
                    saveInstanceState
                )
            }
        }
    }

    internal class AudioHolder(itemView: View) {
        val artist: TextView = itemView.findViewById(R.id.dialog_title)
        val title: TextView = itemView.findViewById(R.id.dialog_message)
        val play: View = itemView.findViewById(R.id.item_audio_play)
        val play_cover: ImageView = itemView.findViewById(R.id.item_audio_play_cover)
        val selectionView: MaterialCardView = itemView.findViewById(R.id.item_audio_selection)
        val visual: RLottieImageView = itemView.findViewById(R.id.item_audio_visual)
        val time: TextView = itemView.findViewById(R.id.item_audio_time)

    }

    companion object {
        const val REQUEST_CODE_AUDIO_DUPLICATE = "request_audio_duplicate"

        @Suppress("DEPRECATION")
        private fun getAudioContent(context: Context, filePath: String, accountId: Long): Audio? {
            val AUDIO_PROJECTION = arrayOf(
                BaseColumns._ID,
                MediaStore.MediaColumns.DURATION,
                MediaStore.MediaColumns.DISPLAY_NAME
            )
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                AUDIO_PROJECTION,
                MediaStore.MediaColumns.DATA + "=? ", arrayOf(filePath), null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getLong(BaseColumns._ID)
                val data = buildUriForPicassoNew(Content_Local.AUDIO, id).toString()
                if (cursor.getString(MediaStore.MediaColumns.DISPLAY_NAME)
                        .isNullOrEmpty()
                ) {
                    cursor.close()
                    return null
                }
                var TrackName =
                    cursor.getString(MediaStore.MediaColumns.DISPLAY_NAME)?.replace(".mp3", "")
                        .orEmpty()
                var Artist = ""
                val arr = TrackName.split(Regex(" - ")).toTypedArray()
                if (arr.size > 1) {
                    Artist = arr[0]
                    TrackName = TrackName.replace("$Artist - ", "")
                }
                var dur =
                    cursor.getInt(MediaStore.MediaColumns.DURATION)
                if (dur != 0) {
                    dur /= 1000
                }
                val ret = Audio().setId(data.hashCode()).setOwnerId(accountId).setDuration(dur)
                    .setUrl(data).setTitle(TrackName).setArtist(Artist)
                cursor.close()
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ret.setThumb_image_big(data).setThumb_image_little(data)
                } else {
                    val uri = buildUriForPicasso(Content_Local.AUDIO, id).toString()
                    ret.setThumb_image_big(uri).setThumb_image_little(uri)
                }
            }
            return null
        }

        fun newInstance(
            context: Context,
            aid: Long,
            new_audio: Audio?,
            old_audio: String
        ): AudioDuplicateDialog? {
            val old = getAudioContent(context, old_audio, aid) ?: return null
            val args = Bundle()
            args.putLong(Extra.ACCOUNT_ID, aid)
            args.putParcelable(Extra.NEW, new_audio)
            args.putParcelable(Extra.OLD, old)
            val dialog = AudioDuplicateDialog()
            dialog.arguments = args
            return dialog
        }
    }
}
