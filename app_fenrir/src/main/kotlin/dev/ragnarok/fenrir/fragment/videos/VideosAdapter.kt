package dev.ragnarok.fenrir.fragment.videos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.view.VideoServiceIcons.getIconByType

class VideosAdapter(private val context: Context, private var data: List<Video>) :
    RecyclerView.Adapter<VideosAdapter.Holder>() {
    private var videoOnClickListener: VideoOnClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_fave_video, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val video = data[position]
        holder.viewsCount.text = video.views.toString()
        holder.title.text = video.title
        holder.videoLenght.text = AppTextUtils.getDurationString(video.duration)
        val photoUrl = video.image
        if (photoUrl.nonNullNoEmpty()) {
            with()
                .load(photoUrl)
                .tag(Constants.PICASSO_TAG)
                .into(holder.image)
        } else {
            with().cancelRequest(holder.image)
        }
        val serviceIcon = getIconByType(video.platform)
        if (serviceIcon != null) {
            holder.videoService.visibility = View.VISIBLE
            holder.videoService.setImageResource(serviceIcon)
        } else {
            holder.videoService.visibility = View.GONE
        }
        holder.card.setOnClickListener {
            videoOnClickListener?.onVideoClick(position, video)
        }
        holder.card.setOnLongClickListener {
            videoOnClickListener?.onVideoLongClick(
                position,
                video
            ) == true
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setVideoOnClickListener(videoOnClickListener: VideoOnClickListener?) {
        this.videoOnClickListener = videoOnClickListener
    }

    fun setData(data: List<Video>) {
        this.data = data
        notifyDataSetChanged()
    }

    interface VideoOnClickListener {
        fun onVideoClick(position: Int, video: Video)
        fun onVideoLongClick(position: Int, video: Video): Boolean
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: View = itemView.findViewById(R.id.card_view)
        val image: ImageView = itemView.findViewById(R.id.video_image)
        val videoLenght: TextView = itemView.findViewById(R.id.video_lenght)
        val videoService: ImageView = itemView.findViewById(R.id.video_service)
        val title: TextView = itemView.findViewById(R.id.title)
        val viewsCount: TextView = itemView.findViewById(R.id.view_count)
    }
}