package dev.ragnarok.fenrir.fragment.vkphotoalbums

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
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with

class VKPhotoAlbumsAdapter(private val context: Context, private var data: List<PhotoAlbum>) :
    RecyclerView.Adapter<VKPhotoAlbumsAdapter.Holder>() {
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.local_album_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val photoAlbum = data[position]
        if (photoAlbum.getSizes()?.notEmpty() == true) {
            val thumb = photoAlbum.getSizes()?.getUrlForSize(PhotoSize.Y, false)
            with()
                .load(thumb)
                .tag(Constants.PICASSO_TAG)
                .placeholder(R.drawable.background_gray)
                .into(holder.imageView)
        } else {
            with().cancelRequest(holder.imageView)
            holder.imageView.setImageResource(R.drawable.album)
        }
        holder.title.text = photoAlbum.getDisplayTitle(holder.title.context)
        holder.imageView.setOnClickListener {
            clickListener?.onVkPhotoAlbumClick(photoAlbum)
        }
        if (photoAlbum.getSize() >= 0) holder.counterText.text = context.getString(
            R.string.photos_count,
            photoAlbum.getSize()
        ) else holder.counterText.setText(R.string.unknown_photos_count)
        holder.imageView.setOnLongClickListener {
            clickListener?.onVkPhotoAlbumLongClick(
                photoAlbum
            ) == true
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    fun setData(data: List<PhotoAlbum>) {
        this.data = data
        notifyDataSetChanged()
    }

    interface ClickListener {
        fun onVkPhotoAlbumClick(album: PhotoAlbum)
        fun onVkPhotoAlbumLongClick(album: PhotoAlbum): Boolean
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.item_local_album_cover)
        val title: TextView = itemView.findViewById(R.id.item_local_album_name)
        val counterText: TextView = itemView.findViewById(R.id.counter)
    }
}