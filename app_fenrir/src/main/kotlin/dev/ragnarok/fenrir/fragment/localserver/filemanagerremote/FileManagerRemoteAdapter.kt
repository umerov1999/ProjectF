package dev.ragnarok.fenrir.fragment.localserver.filemanagerremote

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso3.Callback
import com.squareup.picasso3.Picasso
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.ILocalServerInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.link.VKLinkParser
import dev.ragnarok.fenrir.media.music.MusicPlaybackController
import dev.ragnarok.fenrir.media.music.PlayerStatus
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.Option
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.FileRemote
import dev.ragnarok.fenrir.model.FileType
import dev.ragnarok.fenrir.model.menu.options.FileLocalServerOption
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.toMainThread
import dev.ragnarok.fenrir.util.DownloadWorkUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.toast.CustomSnackbars
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import io.reactivex.rxjava3.disposables.Disposable

class FileManagerRemoteAdapter(private var context: Context, private var data: List<FileRemote>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val colorPrimary = CurrentTheme.getColorPrimary(context)
    private val colorOnSurface = CurrentTheme.getColorOnSurface(context)
    private var clickListener: ClickListener? = null
    private var mPlayerDisposable = Disposable.disposed()
    private var audioListDisposable = Disposable.disposed()
    private var currAudio: Audio? = MusicPlaybackController.currentAudio
    private val factory: ILocalServerInteractor = InteractorFactory.createLocalServerInteractor()

    fun setItems(data: List<FileRemote>) {
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
            if (data[i].id == audio.id && data[i].owner_Id == audio.ownerId) {
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
        audio: FileRemote
    ) {
        if (audio.id != currAudio?.id || audio.owner_Id != currAudio?.ownerId) {
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

    private fun doAudioMenu(position: Int, audio: FileRemote, view: View) {
        val t = Audio()
        t.setId(audio.id)
        t.setOwnerId(audio.owner_Id)
        t.setUrl(audio.url)
        t.setThumb_image_big(audio.preview_url)
        t.setThumb_image_very_big(audio.preview_url)
        t.setThumb_image_little(audio.preview_url)
        t.setDuration(audio.size.toInt())

        var TrackName: String = audio.file_name?.replace(".mp3", "") ?: ""
        val Artist: String
        val arr = TrackName.split(Regex(" - ")).toTypedArray()
        if (arr.size > 1) {
            Artist = arr[0]
            TrackName = TrackName.replace("$Artist - ", "")
        } else {
            Artist = ""
        }
        t.setIsLocal()
        t.setArtist(Artist)
        t.setTitle(TrackName)
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(
            OptionRequest(
                FileLocalServerOption.play_item_audio,
                context.getString(R.string.play),
                R.drawable.play,
                true
            )
        )
        if (MusicPlaybackController.canPlayAfterCurrent(t)) {
            menus.add(
                OptionRequest(
                    FileLocalServerOption.play_item_after_current_audio,
                    context.getString(R.string.play_audio_after_current),
                    R.drawable.play_next,
                    false
                )
            )
        }
        menus.add(
            OptionRequest(
                FileLocalServerOption.save_item,
                context.getString(R.string.download),
                R.drawable.save,
                true
            )
        )
        menus.add(
            OptionRequest(
                FileLocalServerOption.update_time_item,
                context.getString(R.string.update_time),
                R.drawable.ic_recent,
                false
            )
        )
        menus.add(
            OptionRequest(
                FileLocalServerOption.edit_item,
                context.getString(R.string.edit),
                R.drawable.about_writed,
                true
            )
        )
        menus.add(
            OptionRequest(
                FileLocalServerOption.delete_item,
                context.getString(R.string.delete),
                R.drawable.ic_outline_delete,
                false
            )
        )
        menus.add(
            OptionRequest(
                FileLocalServerOption.upload_item_audio,
                context.getString(R.string.upload),
                R.drawable.web,
                true
            )
        )
        menus.header(
            audio.file_name,
            R.drawable.song,
            t.thumb_image_big
        )
        menus.columns(2)
        menus.show((context as FragmentActivity).supportFragmentManager, "audio_options",
            object : ModalBottomSheetDialogFragment.Listener {
                override fun onModalOptionSelected(option: Option) {
                    when (option.id) {
                        FileLocalServerOption.play_item_after_current_audio -> MusicPlaybackController.playAfterCurrent(
                            t
                        )

                        FileLocalServerOption.save_item -> {
                            when (DownloadWorkUtils.doDownloadAudio(
                                context, t, Settings.get().accounts().current,
                                Force = false,
                                isLocal = true
                            )) {
                                0 -> {
                                    createCustomToast(context).showToastBottom(R.string.saved_audio)
                                }

                                1 -> {
                                    CustomSnackbars.createCustomSnackbars(view)
                                        ?.setDurationSnack(BaseTransientBottomBar.LENGTH_LONG)
                                        ?.themedSnack(R.string.audio_force_download)?.setAction(
                                            R.string.button_yes
                                        ) {
                                            DownloadWorkUtils.doDownloadAudio(
                                                context, t, Settings.get().accounts().current,
                                                Force = true,
                                                isLocal = true
                                            )
                                        }
                                        ?.show()
                                }

                                2 -> {
                                    CustomSnackbars.createCustomSnackbars(view)
                                        ?.setDurationSnack(Snackbar.LENGTH_LONG)
                                        ?.themedSnack(R.string.audio_force_download_pc)?.setAction(
                                            R.string.button_yes
                                        ) {
                                            DownloadWorkUtils.doDownloadAudio(
                                                context,
                                                t,
                                                Settings.get().accounts().current,
                                                true,
                                                isLocal = true
                                            )
                                        }?.show()
                                }

                                else -> {
                                    createCustomToast(context).showToastBottom(R.string.error_audio)
                                }
                            }
                        }

                        FileLocalServerOption.upload_item_audio -> {
                            val hash1 = VKLinkParser.parseLocalServerURL(audio.url)
                            if (hash1.isNullOrEmpty()) {
                                return
                            }
                            audioListDisposable =
                                factory.uploadAudio(hash1).fromIOToMain().subscribe(
                                    { createCustomToast(context).showToast(R.string.success) }) { o ->
                                    createCustomToast(context).showToastThrowable(o)
                                }
                        }

                        FileLocalServerOption.delete_item -> {
                            MaterialAlertDialogBuilder(
                                context
                            )
                                .setMessage(R.string.do_delete)
                                .setTitle(R.string.confirmation)
                                .setCancelable(true)
                                .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                                    val hash1 =
                                        VKLinkParser.parseLocalServerURL(audio.url)
                                    if (hash1.isNullOrEmpty()) {
                                        return@setPositiveButton
                                    }
                                    audioListDisposable =
                                        factory
                                            .delete_media(hash1)
                                            .fromIOToMain().subscribe(
                                                { createCustomToast(context).showToast(R.string.success) }) { o: Throwable? ->
                                                createCustomToast(context).showToastThrowable(o)
                                            }
                                }
                                .setNegativeButton(R.string.button_cancel, null)
                                .show()
                        }

                        FileLocalServerOption.update_time_item -> {
                            val hash =
                                VKLinkParser.parseLocalServerURL(audio.url)
                            if (hash.isNullOrEmpty()) {
                                return
                            }
                            audioListDisposable =
                                factory.update_time(hash)
                                    .fromIOToMain().subscribe(
                                        { createCustomToast(context).showToast(R.string.success) }) { t: Throwable? ->
                                        createCustomToast(context).showToastThrowable(t)
                                    }
                        }

                        FileLocalServerOption.edit_item -> {
                            val hash2 =
                                VKLinkParser.parseLocalServerURL(audio.url)
                            if (hash2.isNullOrEmpty()) {
                                return
                            }
                            audioListDisposable =
                                factory.get_file_name(hash2)
                                    .fromIOToMain().subscribe(
                                        { t: String? ->
                                            val root =
                                                View.inflate(
                                                    context,
                                                    R.layout.entry_file_name,
                                                    null
                                                )
                                            (root.findViewById<View>(R.id.edit_file_name) as TextInputEditText).setText(
                                                t
                                            )
                                            MaterialAlertDialogBuilder(context)
                                                .setTitle(R.string.change_name)
                                                .setCancelable(true)
                                                .setView(root)
                                                .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                                                    audioListDisposable =
                                                        factory
                                                            .update_file_name(
                                                                hash2,
                                                                (root.findViewById<View>(R.id.edit_file_name) as TextInputEditText).text.toString()
                                                                    .trim { it <= ' ' })
                                                            .fromIOToMain()
                                                            .subscribe({
                                                                createCustomToast(context).showToast(
                                                                    R.string.success
                                                                )
                                                            }) { o: Throwable? ->
                                                                createCustomToast(context).showToastThrowable(
                                                                    o
                                                                )
                                                            }
                                                }
                                                .setNegativeButton(R.string.button_cancel, null)
                                                .show()
                                        }) { t: Throwable? ->
                                        createCustomToast(context).showToastThrowable(t)
                                    }
                        }

                        FileLocalServerOption.play_item_audio -> {
                            clickListener?.onClick(position, audio)
                            if (Settings.get().other().isShow_mini_player) getPlayerPlace(
                                Settings.get().accounts().current
                            ).tryOpenWith(context)
                        }

                        else -> {}
                    }
                }
            })
    }

    private fun onBindAudioHolder(holder: AudioHolder, position: Int) {
        val item = data[position]

        if (FenrirNative.isNativeLoaded) {
            if (item.isSelected) {
                holder.current.visibility = View.VISIBLE
                holder.current.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.select_fire,
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
            .load(item.preview_url).tag(Constants.PICASSO_TAG)
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
        holder.fileDetails.text =
            if (item.type != FileType.folder) Utils.BytesToSize(item.size) else fixNumerical(
                holder.fileDetails.context,
                item.size.toInt()
            )
        holder.itemView.setOnClickListener {
            val t = Audio()
            t.setId(item.id)
            t.setOwnerId(item.owner_Id)
            if (MusicPlaybackController.isNowPlayingOrPreparingOrPaused(t)) {
                if (!Settings.get().other().isUse_stop_audio) {
                    MusicPlaybackController.playOrPause()
                } else {
                    MusicPlaybackController.stop()
                }
            } else {
                clickListener?.onClick(holder.bindingAdapterPosition, item)
            }
        }
        holder.itemView.setOnLongClickListener {
            doAudioMenu(position, item, it)
            true
        }
        updateAudioStatus(holder, item)
    }

    private fun doFileMenu(file: FileRemote) {
        val menus = ModalBottomSheetDialogFragment.Builder()
        menus.add(
            OptionRequest(
                FileLocalServerOption.update_time_item,
                context.getString(R.string.update_time),
                R.drawable.ic_recent,
                false
            )
        )
        menus.add(
            OptionRequest(
                FileLocalServerOption.edit_item,
                context.getString(R.string.edit),
                R.drawable.about_writed,
                true
            )
        )
        menus.add(
            OptionRequest(
                FileLocalServerOption.delete_item,
                context.getString(R.string.delete),
                R.drawable.ic_outline_delete,
                false
            )
        )
        menus.header(
            file.file_name,
            R.drawable.file,
            file.preview_url
        )
        menus.columns(2)
        menus.show((context as FragmentActivity).supportFragmentManager, "file_options",
            object : ModalBottomSheetDialogFragment.Listener {
                override fun onModalOptionSelected(option: Option) {
                    when (option.id) {
                        FileLocalServerOption.delete_item -> {
                            MaterialAlertDialogBuilder(
                                context
                            )
                                .setMessage(R.string.do_delete)
                                .setTitle(R.string.confirmation)
                                .setCancelable(true)
                                .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int ->
                                    val hash1 =
                                        VKLinkParser.parseLocalServerURL(file.url)
                                    if (hash1.isNullOrEmpty()) {
                                        return@setPositiveButton
                                    }
                                    audioListDisposable =
                                        factory
                                            .delete_media(hash1)
                                            .fromIOToMain().subscribe(
                                                { createCustomToast(context).showToast(R.string.success) }) { o: Throwable? ->
                                                createCustomToast(context).showToastThrowable(o)
                                            }
                                }
                                .setNegativeButton(R.string.button_cancel, null)
                                .show()
                        }

                        FileLocalServerOption.update_time_item -> {
                            val hash =
                                VKLinkParser.parseLocalServerURL(file.url)
                            if (hash.isNullOrEmpty()) {
                                return
                            }
                            audioListDisposable =
                                factory.update_time(hash)
                                    .fromIOToMain().subscribe(
                                        { createCustomToast(context).showToast(R.string.success) }) { t: Throwable? ->
                                        createCustomToast(context).showToastThrowable(t)
                                    }
                        }

                        FileLocalServerOption.edit_item -> {
                            val hash2 =
                                VKLinkParser.parseLocalServerURL(file.url)
                            if (hash2.isNullOrEmpty()) {
                                return
                            }
                            audioListDisposable =
                                factory.get_file_name(hash2)
                                    .fromIOToMain().subscribe(
                                        { t: String? ->
                                            val root =
                                                View.inflate(
                                                    context,
                                                    R.layout.entry_file_name,
                                                    null
                                                )
                                            (root.findViewById<View>(R.id.edit_file_name) as TextInputEditText).setText(
                                                t
                                            )
                                            MaterialAlertDialogBuilder(context)
                                                .setTitle(R.string.change_name)
                                                .setCancelable(true)
                                                .setView(root)
                                                .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                                                    audioListDisposable =
                                                        factory
                                                            .update_file_name(
                                                                hash2,
                                                                (root.findViewById<View>(R.id.edit_file_name) as TextInputEditText).text.toString()
                                                                    .trim { it <= ' ' })
                                                            .fromIOToMain()
                                                            .subscribe({
                                                                createCustomToast(context).showToast(
                                                                    R.string.success
                                                                )
                                                            }) { o: Throwable? ->
                                                                createCustomToast(context).showToastThrowable(
                                                                    o
                                                                )
                                                            }
                                                }
                                                .setNegativeButton(R.string.button_cancel, null)
                                                .show()
                                        }) { t: Throwable? ->
                                        createCustomToast(context).showToastThrowable(t)
                                    }
                        }

                        else -> {}
                    }
                }

            })
    }

    private fun onBindFileHolder(holder: FileHolder, position: Int) {
        val item = data[position]

        if (FenrirNative.isNativeLoaded) {
            if (item.isSelected) {
                holder.current.visibility = View.VISIBLE
                holder.current.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.select_fire,
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
            .load(item.preview_url).tag(Constants.PICASSO_TAG)
            .priority(Picasso.Priority.LOW)
            .into(holder.icon)
        holder.fileName.text = item.file_name
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
        fun onClick(position: Int, item: FileRemote)
    }

    class FileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.item_file_name)
        val fileDetails: TextView = itemView.findViewById(R.id.item_file_details)
        val icon: ImageView = itemView.findViewById(R.id.item_file_icon)
        val current: RLottieImageView = itemView.findViewById(R.id.current)
    }

    class AudioHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.item_file_name)
        val fileDetails: TextView = itemView.findViewById(R.id.item_file_details)
        val icon: ImageView = itemView.findViewById(R.id.item_file_icon)
        val current: RLottieImageView = itemView.findViewById(R.id.current)
        val visual: RLottieImageView = itemView.findViewById(R.id.item_audio_visual)
    }
}