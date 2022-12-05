package dev.ragnarok.fenrir.fragment.localvideos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.MemoryPolicy
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.LocalVideo
import dev.ragnarok.fenrir.picasso.Content_Local
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.buildUriForPicasso
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.view.AspectRatioImageView

class LocalVideosAdapter(private val context: Context, private val data: List<LocalVideo>) :
    RecyclerView.Adapter<LocalVideosAdapter.ViewHolder>() {
    private val holders: MutableSet<ViewHolder>
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(
            LayoutInflater.from(
                context
            )
                .inflate(R.layout.item_local_video, parent, false)
        )
        holders.add(holder)
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = data[position]
        holder.attachPhoto(video)
        with()
            .load(buildUriForPicasso(Content_Local.VIDEO, video.getId()))
            .tag(TAG)
            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
            .placeholder(R.drawable.background_gray)
            .into(holder.photoImageView)
        resolveSelectionVisibility(video, holder)
        resolveIndexText(video, holder)
        val listener = View.OnClickListener {
            clickListener?.onVideoClick(holder, video)
        }
        val preview_listener = View.OnLongClickListener {
            if (clickListener != null) {
                clickListener?.onVideoLongClick(holder, video)
                return@OnLongClickListener true
            }
            false
        }
        holder.photoImageView.setOnClickListener(listener)
        holder.photoImageView.setOnLongClickListener(preview_listener)
        holder.selectedRoot.setOnClickListener(listener)
    }

    fun updateHoldersSelectionAndIndexes() {
        for (holder in holders) {
            val video = holder.itemView.tag as LocalVideo
            resolveSelectionVisibility(video, holder)
            resolveIndexText(video, holder)
        }
    }

    private fun resolveSelectionVisibility(video: LocalVideo, holder: ViewHolder) {
        holder.selectedRoot.visibility = if (video.isSelected) View.VISIBLE else View.GONE
    }

    private fun resolveIndexText(video: LocalVideo, holder: ViewHolder) {
        holder.tvTitle.text = video.getTitle()
        holder.tvDuration.text =
            if (video.getDuration() == 0) "" else AppTextUtils.getDurationStringMS(video.getDuration())
        holder.tvIndex.text = if (video.getIndex() == 0) "" else video.getIndex().toString()
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    override fun getItemCount(): Int {
        return data.size
    }

    interface ClickListener {
        fun onVideoClick(holder: ViewHolder, video: LocalVideo)
        fun onVideoLongClick(holder: ViewHolder, video: LocalVideo)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: AspectRatioImageView =
            itemView.findViewById(R.id.item_video_album_image)
        val selectedRoot: View = itemView.findViewById(R.id.selected)
        val tvIndex: TextView = itemView.findViewById(R.id.item_video_index)
        val tvTitle: TextView = itemView.findViewById(R.id.item_video_album_title)
        val tvDuration: TextView = itemView.findViewById(R.id.item_video_album_count)
        fun attachPhoto(video: LocalVideo) {
            itemView.tag = video
        }
    }

    companion object {
        val TAG: String = LocalVideosAdapter::class.java.simpleName
    }

    init {
        holders = HashSet()
    }
}