package dev.ragnarok.fenrir.fragment.wall.wallattachments.wallmultiattachments

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils

class PhotoAlbumsAdapter(private var data: List<PhotoAlbum>, private val context: Context) :
    RecyclerView.Adapter<PhotoAlbumsAdapter.Holder>() {
    @PhotoSize
    private val mPhotoPreviewSize: Int = Settings.get().main().prefPreviewImageSize
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(context).inflate(R.layout.item_photo_album, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val album = data[position]
        val url = album.getSizes()?.getUrlForSize(mPhotoPreviewSize, false)
        if (url.nonNullNoEmpty()) {
            with()
                .load(url)
                .placeholder(R.drawable.background_gray)
                .tag(Constants.PICASSO_TAG)
                .into(holder.thumb)
            holder.thumb.visibility = View.VISIBLE
        } else {
            with().cancelRequest(holder.thumb)
            holder.thumb.visibility = View.INVISIBLE
        }
        holder.count.text = context.getString(R.string.photos_count, album.getSize())
        holder.name.text = album.getDisplayTitle(holder.name.context)
        if (album.getDescription().isNullOrEmpty()) holder.description.visibility = View.GONE else {
            holder.description.visibility = View.VISIBLE
            holder.description.text = album.getDescription()
        }
        holder.update.text = AppTextUtils.getDateFromUnixTime(context, album.getUpdatedTime())
        holder.album_container.setOnClickListener {
            clickListener?.onAlbumClick(holder.bindingAdapterPosition, album)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<PhotoAlbum>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onAlbumClick(index: Int, album: PhotoAlbum)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumb: ImageView = itemView.findViewById(R.id.item_thumb)
        val name: TextView = itemView.findViewById(R.id.item_title)
        val description: TextView = itemView.findViewById(R.id.item_description)
        val count: TextView = itemView.findViewById(R.id.item_count)
        val update: TextView = itemView.findViewById(R.id.item_time)
        val album_container: View = itemView.findViewById(R.id.album_container)
    }
}