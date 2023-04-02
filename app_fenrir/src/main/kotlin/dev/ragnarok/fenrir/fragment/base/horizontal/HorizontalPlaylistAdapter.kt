package dev.ragnarok.fenrir.fragment.base.horizontal

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.transforms.ImageHelper.getEllipseBitmap
import dev.ragnarok.fenrir.picasso.transforms.PolyTransformation
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class HorizontalPlaylistAdapter(data: MutableList<AudioPlaylist>) :
    RecyclerBindableAdapter<AudioPlaylist, HorizontalPlaylistAdapter.Holder>(data) {
    private var listener: Listener? = null

    @SuppressLint("SetTextI18n")
    override fun onBindItemViewHolder(viewHolder: Holder, position: Int, type: Int) {
        val playlist = getItem(position)
        val context = viewHolder.itemView.context
        if (playlist.getThumb_image().nonNullNoEmpty()) displayAvatar(
            viewHolder.thumb,
            PolyTransformation(),
            playlist.getThumb_image(),
            Constants.PICASSO_TAG
        ) else viewHolder.thumb.setImageBitmap(
            getEllipseBitmap(
                BitmapFactory.decodeResource(
                    context.resources,
                    if (Settings.get().ui()
                            .isDarkModeEnabled(context)
                    ) R.drawable.generic_audio_nowplaying_dark else R.drawable.generic_audio_nowplaying_light
                ), 0.1f
            )
        )
        viewHolder.count.text =
            playlist.getCount().toString() + " " + context.getString(R.string.audios_pattern_count)
        viewHolder.name.text = playlist.getTitle()
        if (playlist.getDescription().isNullOrEmpty()) viewHolder.description.visibility =
            View.GONE else {
            viewHolder.description.visibility = View.VISIBLE
            viewHolder.description.text = playlist.getDescription()
            viewHolder.description.setOnClickListener {
                if (viewHolder.description.maxLines == 1) viewHolder.description.maxLines =
                    6 else viewHolder.description.maxLines = 1
            }
        }
        if (playlist.getArtist_name().isNullOrEmpty()) viewHolder.artist.visibility =
            View.GONE else {
            viewHolder.artist.visibility = View.VISIBLE
            viewHolder.artist.text = playlist.getArtist_name()
        }
        if (playlist.getYear() == 0) viewHolder.year.visibility = View.GONE else {
            viewHolder.year.visibility = View.VISIBLE
            viewHolder.year.text = playlist.getYear().toString()
        }
        if (playlist.getGenre().isNullOrEmpty()) viewHolder.genre.visibility = View.GONE else {
            viewHolder.genre.visibility = View.VISIBLE
            viewHolder.genre.text = playlist.getGenre()
        }
        viewHolder.update.text =
            AppTextUtils.getDateFromUnixTime(context, playlist.getUpdate_time())
        viewHolder.add.setOnClickListener {
            listener?.onPlayListClick(
                playlist,
                getItemRawPosition(viewHolder.bindingAdapterPosition)
            )
        }
        viewHolder.share.setOnClickListener {
            listener?.onShareClick(
                playlist,
                getItemRawPosition(viewHolder.bindingAdapterPosition)
            )
        }
        if (playlist.getId() >= 0) {
            viewHolder.add.visibility = View.VISIBLE
            viewHolder.share.visibility = View.VISIBLE
            if (playlist.getOwnerId() == Settings.get()
                    .accounts().current
            ) viewHolder.add.setImageResource(R.drawable.ic_outline_delete) else viewHolder.add.setImageResource(
                R.drawable.plus
            )
        } else {
            viewHolder.add.visibility = View.GONE
            viewHolder.share.visibility = View.GONE
        }
    }

    override fun viewHolder(view: View, type: Int): Holder {
        return Holder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_internal_audio_playlist
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    interface Listener {
        fun onPlayListClick(item: AudioPlaylist, pos: Int)
        fun onShareClick(item: AudioPlaylist, pos: Int)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumb: ImageView = itemView.findViewById(R.id.item_thumb)
        val name: TextView = itemView.findViewById(R.id.item_name)
        val description: TextView = itemView.findViewById(R.id.item_description)
        val count: TextView = itemView.findViewById(R.id.item_count)
        val year: TextView = itemView.findViewById(R.id.item_year)
        val artist: TextView = itemView.findViewById(R.id.item_artist)
        val genre: TextView = itemView.findViewById(R.id.item_genre)
        val update: TextView = itemView.findViewById(R.id.item_time)
        val add: FloatingActionButton = itemView.findViewById(R.id.add_playlist)
        val share: FloatingActionButton = itemView.findViewById(R.id.share_playlist)
    }
}