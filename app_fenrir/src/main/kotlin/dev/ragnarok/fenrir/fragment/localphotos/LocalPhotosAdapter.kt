package dev.ragnarok.fenrir.fragment.localphotos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.MemoryPolicy
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.picasso.Content_Local
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.buildUriForPicasso
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with

class LocalPhotosAdapter(private val context: Context, private val data: List<LocalPhoto>) :
    RecyclerView.Adapter<LocalPhotosAdapter.ViewHolder>() {
    private val holders: MutableSet<ViewHolder>
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(
            LayoutInflater.from(
                context
            )
                .inflate(R.layout.photo_item, parent, false)
        )
        holders.add(holder)
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photo = data[position]
        holder.attachPhoto(photo)
        with()
            .load(buildUriForPicasso(Content_Local.PHOTO, photo.getImageId()))
            .tag(TAG)
            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
            .placeholder(R.drawable.background_gray)
            .into(holder.photoImageView)
        resolveSelectionVisibility(photo, holder)
        resolveIndexText(photo, holder)
        val listener = View.OnClickListener {
            clickListener?.onPhotoClick(holder, photo)
        }
        val preview_listener = View.OnLongClickListener {
            if (clickListener != null) {
                clickListener?.onLongPhotoClick(holder, photo)
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
            val photo = holder.itemView.tag as LocalPhoto?
            if (photo != null) {
                resolveSelectionVisibility(photo, holder)
                resolveIndexText(photo, holder)
            }
        }
    }

    private fun resolveSelectionVisibility(photo: LocalPhoto, holder: ViewHolder) {
        holder.selectedRoot.visibility = if (photo.isSelected) View.VISIBLE else View.GONE
    }

    private fun resolveIndexText(photo: LocalPhoto, holder: ViewHolder) {
        holder.tvIndex.text = if (photo.getIndex() == 0) "" else photo.getIndex().toString()
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    override fun getItemCount(): Int {
        return data.size
    }

    interface ClickListener {
        fun onPhotoClick(holder: ViewHolder, photo: LocalPhoto)
        fun onLongPhotoClick(holder: ViewHolder, photo: LocalPhoto)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: ImageView = itemView.findViewById(R.id.imageView)
        val selectedRoot: View = itemView.findViewById(R.id.selected)
        val tvIndex: TextView = itemView.findViewById(R.id.item_photo_index)
        fun attachPhoto(photo: LocalPhoto) {
            itemView.tag = photo
        }
    }

    companion object {
        val TAG: String = LocalPhotosAdapter::class.java.simpleName
    }

    init {
        holders = HashSet()
    }
}