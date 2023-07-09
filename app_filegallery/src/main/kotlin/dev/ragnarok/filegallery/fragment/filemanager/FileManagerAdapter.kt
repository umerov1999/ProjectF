package dev.ragnarok.filegallery.fragment.filemanager

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Callback
import com.squareup.picasso3.Picasso
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.fromIOToMain
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.media.music.PlayerStatus
import dev.ragnarok.filegallery.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.filegallery.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.FileItem
import dev.ragnarok.filegallery.model.FileType
import dev.ragnarok.filegallery.model.menu.options.AudioLocalOption
import dev.ragnarok.filegallery.picasso.PicassoInstance
import dev.ragnarok.filegallery.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.filegallery.settings.CurrentTheme
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.toMainThread
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.toast.CustomToast
import dev.ragnarok.filegallery.view.natives.rlottie.RLottieImageView
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.io.File

class FileManagerAdapter(private var context: Context, private var data: List<FileItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val colorPrimary = CurrentTheme.getColorPrimary(context)
    private val colorOnSurface = CurrentTheme.getColorOnSurface(context)
    private var clickListener: ClickListener? = null
    private var mPlayerDisposable = Disposable.disposed()
    private var audioListDisposable = Disposable.disposed()
    private var currAudio: Audio? = MusicPlaybackController.currentAudio
    private var isSelectMode = false

    fun updateSelectedMode(show: Boolean) {
        isSelectMode = show
    }

    fun setItems(data: List<FileItem>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mPlayerDisposable = MusicPlaybackController.observeServiceBinding()
            .toMainThread()
            .subscribe { status: Int ->
                onServiceBindEvent(
                    status
                )
            }
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

    private fun getIndexAudio(audio: Audio?): Int {
        audio ?: return -1
        for (i in data.indices) {
            if (data[i].fileNameHash == audio.id && data[i].filePathHash == audio.ownerId) {
                return i
            }
        }
        return -1
    }

    private fun updateAudio(audio: Audio?) {
        val pos = getIndexAudio(audio)
        if (pos != -1) {
            notifyItemChanged(pos)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mPlayerDisposable.dispose()
        audioListDisposable.dispose()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            FileType.error, FileType.photo, FileType.video -> return FileHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_manager_file, parent, false)
            )

            FileType.folder -> return FileHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_manager_folder, parent, false)
            )

            FileType.audio -> return AudioHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_manager_audio, parent, false)
            )
        }
        return FileHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_manager_file, parent, false)
        )
    }

    override fun getItemViewType(position: Int): Int {
        return data[position].type
    }

    private fun fixNumerical(context: Context, num: Int): String? {
        if (num < 0) {
            return null
        }
        val preLastDigit = num % 100 / 10
        if (preLastDigit == 1) {
            return context.getString(R.string.files_count_c, num)
        }
        return when (num % 10) {
            1 -> context.getString(R.string.files_count_a, num)
            2, 3, 4 -> context.getString(R.string.files_count_b, num)
            else -> context.getString(R.string.files_count_c, num)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            FileType.audio -> onBindAudioHolder(holder as AudioHolder, position)
            else -> onBindFileHolder(holder as FileHolder, position)
        }
    }

    private fun updateAudioStatus(
        holder: AudioHolder,
        audio: FileItem
    ) {
        if (audio.fileNameHash != currAudio?.id && audio.filePathHash != currAudio?.ownerId) {
            holder.visual.setImageResource(R.drawable.song)
            holder.icon.clearColorFilter()
            return
        }
        when (MusicPlaybackController.playerStatus()) {
            1 -> {
                Utils.doWavesLottieBig(holder.visual, true)
                holder.icon.setColorFilter(Color.parseColor("#44000000"))
            }

            2 -> {
                Utils.doWavesLottieBig(holder.visual, false)
                holder.icon.setColorFilter(Color.parseColor("#44000000"))
            }
        }
    }

    private fun doLocalBitrate(url: String): Single<Pair<Int, Long>> {
        return Single.create { v ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(url)
                val bitrate =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                if (bitrate != null) {
                    v.onSuccess(
                        Pair(
                            (bitrate.toLong() / 1000).toInt(),
                            Uri.parse(url).toFile().length()
                        )
                    )
                } else {
                    v.tryOnError(Throwable("Can't receipt bitrate "))
                }
            } catch (e: RuntimeException) {
                v.tryOnError(e)
            }
        }
    }

    internal fun getLocalBitrate(url: String?) {
        if (url.isNullOrEmpty()) {
            return
        }
        audioListDisposable = doLocalBitrate(url).fromIOToMain()
            .subscribe(
                { (first, second) ->
                    CustomToast.createCustomToast(
                        context, null
                    )?.showToast(
                        context.resources.getString(
                            R.string.bitrate,
                            first,
                            Utils.BytesToSize(second)
                        )
                    )
                }
            ) { e: Throwable? ->
                CustomToast.createCustomToast(context, null)?.setDuration(Toast.LENGTH_LONG)
                    ?.showToastThrowable(e)
            }
    }

    private fun doAudioMenu(position: Int, audio: FileItem) {
        val t = Audio()
        t.setId(audio.fileNameHash)
        t.setOwnerId(audio.filePathHash)
        t.setUrl("file://" + audio.file_path)
        t.setThumb_image("thumb_file://" + audio.file_path)
        t.setDuration(audio.size.toInt())

        var TrackName: String = audio.file_name?.replace(".mp3", "") ?: ""
        val Artist: String
        val arr = TrackName.split(Regex(" - ")).toTypedArray()
        if (arr.size > 1) {
            Artist = arr[0]
            TrackName = TrackName.replace("$Artist - ", "")
        } else {
            Artist = audio.parent_name ?: ""
        }
        t.setIsLocal()
        t.setArtist(Artist)
        t.setTitle(TrackName)
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(
            OptionRequest(
                AudioLocalOption.play_item_audio,
                context.getString(R.string.play),
                R.drawable.play,
                true
            )
        )
        if (MusicPlaybackController.canPlayAfterCurrent(t)) {
            menus.add(
                OptionRequest(
                    AudioLocalOption.play_item_after_current_audio,
                    context.getString(R.string.play_audio_after_current),
                    R.drawable.play_next,
                    false
                )
            )
        }
        menus.add(
            OptionRequest(
                AudioLocalOption.bitrate_item_audio,
                context.getString(R.string.get_bitrate),
                R.drawable.high_quality,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.open_with_item,
                context.getString(R.string.open_with),
                R.drawable.ic_external,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.share_item,
                context.getString(R.string.share),
                R.drawable.ic_share,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.update_file_time_item,
                context.getString(R.string.update_time),
                R.drawable.ic_recent,
                false
            )
        )
        if (Settings.get().main().getLocalServer().enabled) {
            menus.add(
                OptionRequest(
                    AudioLocalOption.play_via_local_server,
                    context.getString(R.string.play_remote),
                    R.drawable.remote_cloud,
                    false
                )
            )
        }
        menus.add(
            OptionRequest(
                AudioLocalOption.add_dir_tag_item,
                if (audio.isHasTag) context.getString(R.string.remove_dir_tag) else context.getString(
                    R.string.add_dir_tag
                ),
                R.drawable.star,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.delete_item,
                context.getString(R.string.delete),
                R.drawable.ic_outline_delete,
                false
            )
        )
        menus.header(
            audio.file_name,
            R.drawable.song,
            t.thumb_image
        )
        menus.columns(2)
        menus.show(
            (context as FragmentActivity).supportFragmentManager,
            "audio_options"
        ) { _, option ->
            when (option.id) {
                AudioLocalOption.play_via_local_server -> {
                    clickListener?.onRemotePlay(audio)
                }

                AudioLocalOption.add_dir_tag_item -> {
                    clickListener?.onDirTag(audio)
                }

                AudioLocalOption.delete_item -> {
                    clickListener?.onDelete(audio)
                }

                AudioLocalOption.play_item_audio -> {
                    clickListener?.onClick(position, audio)
                    if (Settings.get().main().isShow_mini_player()) getPlayerPlace(
                    ).tryOpenWith(context)
                }

                AudioLocalOption.play_item_after_current_audio -> MusicPlaybackController.playAfterCurrent(
                    t
                )

                AudioLocalOption.bitrate_item_audio -> getLocalBitrate(t.url)
                AudioLocalOption.open_with_item -> {
                    val intent_open = Intent(Intent.ACTION_VIEW)
                    intent_open.setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            Constants.FILE_PROVIDER_AUTHORITY,
                            File(audio.file_path ?: return@show)
                        ), MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(File(audio.file_path).extension)
                    ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent_open)
                }

                AudioLocalOption.share_item -> {
                    val intent_send = Intent(Intent.ACTION_SEND)
                    intent_send.type = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(File(audio.file_path ?: return@show).extension)
                    intent_send.putExtra(
                        Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            context,
                            Constants.FILE_PROVIDER_AUTHORITY,
                            File(audio.file_path)
                        )
                    ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent_send)
                }

                AudioLocalOption.update_file_time_item -> {
                    clickListener?.onUpdateTimeFile(audio)
                }

                else -> {}
            }
        }
    }

    private fun onBindAudioHolder(holder: AudioHolder, position: Int) {
        val item = data[position]

        if (FenrirNative.isNativeLoaded) {
            if (item.isSelected) {
                holder.current.visibility = View.VISIBLE
                holder.current.fromRes(
                    R.raw.select_fire,
                    Utils.dp(100f),
                    Utils.dp(100f),
                    intArrayOf(0xFF812E, colorPrimary),
                    true
                )
                holder.current.playAnimation()
            } else {
                holder.current.visibility = View.GONE
                holder.current.clearAnimationDrawable()
            }
        }

        PicassoInstance.with()
            .load("thumb_file://${item.file_path}").tag(Constants.PICASSO_TAG)
            .priority(Picasso.Priority.LOW)
            .into(holder.icon, object : Callback {
                override fun onSuccess() {
                    holder.visual.clearColorFilter()
                }

                override fun onError(t: Throwable) {
                    holder.visual.setColorFilter(colorOnSurface)
                }
            })
        holder.fileName.text = item.file_name
        holder.tagged.visibility = if (item.isHasTag) View.VISIBLE else View.GONE
        holder.fileDetails.text =
            if (item.type != FileType.folder) Utils.BytesToSize(item.size) else fixNumerical(
                holder.fileDetails.context,
                item.size.toInt()
            )
        holder.itemView.setOnClickListener {
            val t = Audio()
            t.setId(item.fileNameHash)
            t.setOwnerId(item.filePathHash)
            if (MusicPlaybackController.isNowPlayingOrPreparingOrPaused(t)) {
                if (!Settings.get().main().isUse_stop_audio()) {
                    MusicPlaybackController.playOrPause()
                } else {
                    MusicPlaybackController.stop()
                }
            } else {
                clickListener?.onClick(holder.bindingAdapterPosition, item)
            }
        }
        holder.itemView.setOnLongClickListener {
            doAudioMenu(holder.bindingAdapterPosition, item)
            true
        }
        updateAudioStatus(holder, item)
    }

    private fun doFileMenu(file: FileItem) {
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(
            OptionRequest(
                AudioLocalOption.open_with_item,
                context.getString(R.string.open_with),
                R.drawable.ic_external,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.share_item,
                context.getString(R.string.share),
                R.drawable.ic_share,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.update_file_time_item,
                context.getString(R.string.update_time),
                R.drawable.ic_recent,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.add_dir_tag_item,
                if (file.isHasTag) context.getString(R.string.remove_dir_tag) else context.getString(
                    R.string.add_dir_tag
                ),
                R.drawable.star,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.delete_item,
                context.getString(R.string.delete),
                R.drawable.ic_outline_delete,
                false
            )
        )
        menus.header(
            file.file_name,
            R.drawable.file,
            "thumb_file://" + file.file_path
        )
        menus.columns(2)
        menus.show(
            (context as FragmentActivity).supportFragmentManager,
            "file_options"
        ) { _, option ->
            when (option.id) {
                AudioLocalOption.add_dir_tag_item -> {
                    clickListener?.onDirTag(file)
                }

                AudioLocalOption.delete_item -> {
                    clickListener?.onDelete(file)
                }

                AudioLocalOption.open_with_item -> {
                    val intent_open = Intent(Intent.ACTION_VIEW)
                    intent_open.setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            Constants.FILE_PROVIDER_AUTHORITY,
                            File(file.file_path ?: return@show)
                        ), MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(File(file.file_path).extension)
                    ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent_open)
                }

                AudioLocalOption.share_item -> {
                    val intent_send = Intent(Intent.ACTION_SEND)
                    intent_send.type = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(File(file.file_path ?: return@show).extension)
                    intent_send.putExtra(
                        Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            context,
                            Constants.FILE_PROVIDER_AUTHORITY,
                            File(file.file_path)
                        )
                    ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent_send)
                }

                AudioLocalOption.update_file_time_item -> {
                    clickListener?.onUpdateTimeFile(file)
                }

                else -> {}
            }
        }
    }

    private fun doFolderMenu(file: FileItem) {
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(
            OptionRequest(
                AudioLocalOption.delete_item,
                context.getString(R.string.delete),
                R.drawable.ic_outline_delete,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.fix_dir_time_item,
                context.getString(R.string.fix_dir_time),
                R.drawable.ic_recent,
                false
            )
        )
        menus.add(
            OptionRequest(
                AudioLocalOption.add_dir_tag_item,
                if (file.isHasTag) context.getString(R.string.remove_dir_tag) else context.getString(
                    R.string.add_dir_tag
                ),
                R.drawable.star,
                false
            )
        )
        menus.columns(1)
        menus.show(
            (context as FragmentActivity).supportFragmentManager,
            "folder_options"
        ) { _, option ->
            when (option.id) {
                AudioLocalOption.fix_dir_time_item -> {
                    clickListener?.onFixDir(file)
                }

                AudioLocalOption.add_dir_tag_item -> {
                    clickListener?.onDirTag(file)
                }

                AudioLocalOption.delete_item -> {
                    clickListener?.onDelete(file)
                }

                else -> {}
            }
        }
    }

    private fun onBindFileHolder(holder: FileHolder, position: Int) {
        val item = data[position]

        if (FenrirNative.isNativeLoaded) {
            if (item.isSelected) {
                holder.current.visibility = View.VISIBLE
                holder.current.fromRes(
                    R.raw.select_fire,
                    Utils.dp(100f),
                    Utils.dp(100f),
                    intArrayOf(0xFF812E, colorPrimary),
                    true
                )
                holder.current.playAnimation()
            } else {
                holder.current.visibility = View.GONE
                holder.current.clearAnimationDrawable()
            }
        }

        PicassoInstance.with()
            .load("thumb_file://${item.file_path}").tag(Constants.PICASSO_TAG)
            .priority(Picasso.Priority.LOW)
            .into(holder.icon)
        holder.fileName.text = item.file_name
        holder.tagged.visibility = if (item.isHasTag) View.VISIBLE else View.GONE
        holder.fileDetails.text =
            if (item.type != FileType.folder) Utils.BytesToSize(item.size) else fixNumerical(
                holder.fileDetails.context,
                item.size.toInt()
            )
        holder.itemView.setOnClickListener {
            clickListener?.onClick(holder.bindingAdapterPosition, item)
        }
        holder.itemView.setOnLongClickListener {
            if (item.type != FileType.folder) {
                doFileMenu(item)
            } else {
                if (isSelectMode) {
                    clickListener?.onToggleDirTag(item)
                } else {
                    doFolderMenu(item)
                }
            }
            true
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onClick(position: Int, item: FileItem)
        fun onFixDir(item: FileItem)
        fun onUpdateTimeFile(item: FileItem)
        fun onDirTag(item: FileItem)
        fun onToggleDirTag(item: FileItem)
        fun onDelete(item: FileItem)
        fun onRemotePlay(audio: FileItem)
    }

    class FileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.item_file_name)
        val fileDetails: TextView = itemView.findViewById(R.id.item_file_details)
        val icon: ImageView = itemView.findViewById(R.id.item_file_icon)
        val current: RLottieImageView = itemView.findViewById(R.id.current)
        val tagged: ImageView = itemView.findViewById(R.id.item_tagged)
    }

    class AudioHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.item_file_name)
        val fileDetails: TextView = itemView.findViewById(R.id.item_file_details)
        val icon: ImageView = itemView.findViewById(R.id.item_file_icon)
        val current: RLottieImageView = itemView.findViewById(R.id.current)
        val visual: RLottieImageView = itemView.findViewById(R.id.item_audio_visual)
        val tagged: ImageView = itemView.findViewById(R.id.item_tagged)
    }
}