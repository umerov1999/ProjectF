package dev.ragnarok.fenrir.fragment.fave.favevideos

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
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.view.VideoServiceIcons.getIconByType

class FaveVideosAdapter(private val context: Context, private var data: List<Video>) :
    RecyclerView.Adapter<FaveVideosAdapter.Holder>() {
    private var recyclerView: RecyclerView? = null
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
            videoOnClickListener?.onVideoClick(holder.bindingAdapterPosition, video)
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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    interface VideoOnClickListener {
        fun onVideoClick(position: Int, video: Video)
        fun onDelete(index: Int, video: Video)
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView),
        OnCreateContextMenuListener {
        val card: View
        val image: ImageView
        val videoLenght: TextView
        val videoService: ImageView
        val title: TextView
        val viewsCount: TextView
        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
            val position = recyclerView?.getChildAdapterPosition(v) ?: 0
            val video = data[position]
            menu.setHeaderTitle(video.title)
            menu.add(0, v.id, 0, R.string.delete).setOnMenuItemClickListener {
                videoOnClickListener?.onDelete(position, video)
                true
            }
        }

        init {
            itemView.setOnCreateContextMenuListener(this)
            card = itemView.findViewById(R.id.card_view)
            image = itemView.findViewById(R.id.video_image)
            videoLenght = itemView.findViewById(R.id.video_lenght)
            videoService = itemView.findViewById(R.id.video_service)
            title = itemView.findViewById(R.id.title)
            viewsCount = itemView.findViewById(R.id.view_count)
        }
    }
}