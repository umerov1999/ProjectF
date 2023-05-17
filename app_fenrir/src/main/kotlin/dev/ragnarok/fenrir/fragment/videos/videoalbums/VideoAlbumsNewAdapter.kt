package dev.ragnarok.fenrir.fragment.videos.videoalbums

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.VideoAlbum
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with

class VideoAlbumsNewAdapter(private val context: Context, private var data: List<VideoAlbum>) :
    RecyclerView.Adapter<VideoAlbumsNewAdapter.ViewHolder>() {
    private var listener: Listener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(
                context
            ).inflate(R.layout.item_video_album, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.tvCount.text =
            context.getString(R.string.videos_albums_videos_counter, item.getCount())
        holder.tvTitle.text = item.getTitle()
        val photoUrl = item.getImage()
        holder.ivPhoto.visibility = if (photoUrl.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        if (photoUrl.nonNullNoEmpty()) {
            with()
                .load(photoUrl)
                .tag(PICASSO_TAG)
                .into(holder.ivPhoto)
        }
        holder.itemView.setOnClickListener {
            listener?.onClick(item)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<VideoAlbum>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    interface Listener {
        fun onClick(album: VideoAlbum)
    }

    class ViewHolder internal constructor(root: View) : RecyclerView.ViewHolder(root) {
        val ivPhoto: ImageView = root.findViewById(R.id.item_video_album_image)
        val tvCount: TextView = root.findViewById(R.id.item_video_album_count)
        val tvTitle: TextView = root.findViewById(R.id.item_video_album_title)
    }

    companion object {
        const val PICASSO_TAG = "VideoAlbumsNewAdapter"
    }
}