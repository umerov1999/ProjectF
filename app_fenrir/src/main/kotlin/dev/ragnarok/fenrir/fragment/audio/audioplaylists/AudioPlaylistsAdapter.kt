package dev.ragnarok.fenrir.fragment.audio.audioplaylists

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class AudioPlaylistsAdapter(
    private var data: List<AudioPlaylist>,
    private val context: Context,
    private val isSelect: Boolean
) : RecyclerView.Adapter<AudioPlaylistsAdapter.Holder>() {
    private val isDark: Boolean = Settings.get().ui().isDarkModeEnabled(context)
    private var recyclerView: RecyclerView? = null
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(context).inflate(R.layout.item_audio_playlist, parent, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val playlist = data[position]
        if (playlist.getThumb_image().nonNullNoEmpty()) displayAvatar(
            holder.thumb,
            null,
            playlist.getThumb_image(),
            Constants.PICASSO_TAG
        ) else holder.thumb.setImageResource(if (isDark) R.drawable.generic_audio_nowplaying_dark else R.drawable.generic_audio_nowplaying_light)
        holder.count.text =
            playlist.getCount().toString() + " " + context.getString(R.string.audios_pattern_count)
        holder.name.text = playlist.getTitle()
        if (playlist.getDescription().isNullOrEmpty()) holder.description.visibility =
            View.GONE else {
            holder.description.visibility = View.VISIBLE
            holder.description.text = playlist.getDescription()
        }
        if (playlist.getArtist_name().isNullOrEmpty()) holder.artist.visibility = View.GONE else {
            holder.artist.visibility = View.VISIBLE
            holder.artist.text = playlist.getArtist_name()
        }
        if (playlist.getYear() == 0) holder.year.visibility = View.GONE else {
            holder.year.visibility = View.VISIBLE
            holder.year.text = playlist.getYear().toString()
        }
        if (playlist.getGenre().isNullOrEmpty()) holder.genre.visibility = View.GONE else {
            holder.genre.visibility = View.VISIBLE
            holder.genre.text = playlist.getGenre()
        }
        holder.update.text = AppTextUtils.getDateFromUnixTime(context, playlist.getUpdate_time())
        holder.playlist_container.setOnClickListener {
            clickListener?.onAlbumClick(holder.bindingAdapterPosition, playlist)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<AudioPlaylist>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onAlbumClick(index: Int, album: AudioPlaylist)
        fun onOpenClick(index: Int, album: AudioPlaylist)
        fun onDelete(index: Int, album: AudioPlaylist)
        fun onShare(index: Int, album: AudioPlaylist)
        fun onEdit(index: Int, album: AudioPlaylist)
        fun onAddAudios(index: Int, album: AudioPlaylist)
        fun onAdd(index: Int, album: AudioPlaylist, clone: Boolean)
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView),
        OnCreateContextMenuListener {
        val thumb: ImageView
        val name: TextView
        val description: TextView
        val count: TextView
        val year: TextView
        val artist: TextView
        val genre: TextView
        val update: TextView
        val playlist_container: View
        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
            val position = recyclerView?.getChildAdapterPosition(v) ?: 0
            val playlist = data[position]
            if (!Utils.isValueAssigned(playlist.getId(), Settings.get().other().servicePlaylist)) {
                if (Settings.get().accounts().current == playlist.getOwnerId()) {
                    menu.add(0, v.id, 0, R.string.delete)
                        .setOnMenuItemClickListener {
                            clickListener?.onDelete(position, playlist)
                            true
                        }
                    if (playlist.getOriginal_access_key()
                            .isNullOrEmpty() || playlist.getOriginal_id() == 0 || playlist.getOriginal_owner_id() == 0L
                    ) {
                        menu.add(0, v.id, 0, R.string.edit)
                            .setOnMenuItemClickListener {
                                clickListener?.onEdit(position, playlist)
                                true
                            }
                        menu.add(0, v.id, 0, R.string.action_add_audios)
                            .setOnMenuItemClickListener {
                                clickListener?.onAddAudios(position, playlist)
                                true
                            }
                    }
                } else {
                    menu.add(0, v.id, 0, R.string.save)
                        .setOnMenuItemClickListener {
                            clickListener?.onAdd(position, playlist, false)
                            true
                        }
                }
                if (!isSelect) {
                    menu.add(0, v.id, 0, R.string.share)
                        .setOnMenuItemClickListener {
                            clickListener?.onShare(position, playlist)
                            true
                        }
                }
            } else {
                menu.add(0, v.id, 0, R.string.save).setOnMenuItemClickListener {
                    clickListener?.onAdd(position, playlist, true)
                    true
                }
            }
            menu.add(0, v.id, 0, R.string.open).setOnMenuItemClickListener {
                clickListener?.onOpenClick(position, playlist)
                true
            }
        }

        init {
            itemView.setOnCreateContextMenuListener(this)
            thumb = itemView.findViewById(R.id.item_thumb)
            name = itemView.findViewById(R.id.item_name)
            count = itemView.findViewById(R.id.item_count)
            playlist_container = itemView.findViewById(R.id.playlist_container)
            description = itemView.findViewById(R.id.item_description)
            update = itemView.findViewById(R.id.item_time)
            year = itemView.findViewById(R.id.item_year)
            artist = itemView.findViewById(R.id.item_artist)
            genre = itemView.findViewById(R.id.item_genre)
        }
    }

}