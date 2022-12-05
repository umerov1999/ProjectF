package dev.ragnarok.filegallery.fragment.tagdir

import android.content.Context
import android.graphics.Color
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Callback
import com.squareup.picasso3.Picasso
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.media.music.PlayerStatus
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.FileType
import dev.ragnarok.filegallery.model.tags.TagDir
import dev.ragnarok.filegallery.picasso.PicassoInstance
import dev.ragnarok.filegallery.settings.CurrentTheme
import dev.ragnarok.filegallery.settings.Settings
import dev.ragnarok.filegallery.toMainThread
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.view.natives.rlottie.RLottieImageView
import io.reactivex.rxjava3.disposables.Disposable


class TagDirAdapter(context: Context, private var data: List<TagDir>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val colorPrimary = CurrentTheme.getColorPrimary(context)
    private var clickListener: ClickListener? = null
    private var recyclerView: RecyclerView? = null
    private val colorOnSurface = CurrentTheme.getColorOnSurface(context)
    private var currAudio: Audio? = MusicPlaybackController.currentAudio
    private var mPlayerDisposable = Disposable.disposed()

    fun setItems(data: List<TagDir>) {
        this.data = data
        notifyDataSetChanged()
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
            .load("thumb_file://${item.path}").tag(Constants.PICASSO_TAG)
            .priority(Picasso.Priority.LOW)
            .into(holder.icon, object : Callback {
                override fun onSuccess() {
                    holder.visual.clearColorFilter()
                }

                override fun onError(t: Throwable) {
                    holder.visual.setColorFilter(colorOnSurface)
                }
            })
        holder.fileName.text = item.name
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
        updateAudioStatus(holder, item)
    }

    private fun updateAudioStatus(
        holder: AudioHolder,
        audio: TagDir
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
            .load("thumb_file://${item.path}").tag(Constants.PICASSO_TAG)
            .priority(Picasso.Priority.LOW)
            .into(holder.icon)
        holder.fileName.text = item.name
        holder.fileDetails.text =
            if (item.type != FileType.folder) Utils.BytesToSize(item.size) else fixNumerical(
                holder.fileDetails.context,
                item.size.toInt()
            )
        holder.itemView.setOnClickListener {
            clickListener?.onClick(holder.bindingAdapterPosition, item)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        mPlayerDisposable = MusicPlaybackController.observeServiceBinding()
            .toMainThread()
            .subscribe { status: Int ->
                onServiceBindEvent(
                    status
                )
            }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
        mPlayerDisposable.dispose()
    }

    interface ClickListener {
        fun onClick(position: Int, item: TagDir)
        fun onRemove(position: Int, item: TagDir)
    }

    inner class FileHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        OnCreateContextMenuListener {
        val fileName: TextView = itemView.findViewById(R.id.item_file_name)
        val fileDetails: TextView = itemView.findViewById(R.id.item_file_details)
        val icon: ImageView = itemView.findViewById(R.id.item_file_icon)
        val current: RLottieImageView = itemView.findViewById(R.id.current)

        init {
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(
            menu: ContextMenu,
            v: View,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            val position: Int = recyclerView?.getChildAdapterPosition(v) ?: 0
            val dir: TagDir = data[position]
            menu.setHeaderTitle(dir.name)

            menu.add(0, v.id, 0, R.string.delete)
                .setOnMenuItemClickListener {
                    clickListener?.onRemove(position, dir)
                    true
                }
        }
    }

    inner class AudioHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        OnCreateContextMenuListener {
        val fileName: TextView = itemView.findViewById(R.id.item_file_name)
        val fileDetails: TextView = itemView.findViewById(R.id.item_file_details)
        val icon: ImageView = itemView.findViewById(R.id.item_file_icon)
        val current: RLottieImageView = itemView.findViewById(R.id.current)
        val visual: RLottieImageView = itemView.findViewById(R.id.item_audio_visual)

        init {
            itemView.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(
            menu: ContextMenu,
            v: View,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            val position: Int = recyclerView?.getChildAdapterPosition(v) ?: 0
            val dir: TagDir = data[position]
            menu.setHeaderTitle(dir.name)

            menu.add(0, v.id, 0, R.string.delete)
                .setOnMenuItemClickListener {
                    clickListener?.onRemove(position, dir)
                    true
                }
        }
    }
}
