package dev.ragnarok.fenrir.fragment.search.photosearch

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView

class SearchPhotosAdapter(
    private val mContext: Context,
    photos: List<Photo>,
    private val mPicassoTag: String
) : RecyclerView.Adapter<SearchPhotosAdapter.PhotoViewHolder>() {
    private val mColorPrimaryWithAlpha: Int =
        Utils.adjustAlpha(CurrentTheme.getColorPrimary(mContext), 0.75f)
    private val mColorSecondaryWithAlpha: Int =
        Utils.adjustAlpha(CurrentTheme.getColorSecondary(mContext), 0.60f)
    private var data: List<Photo>
    private var mPhotosActionListener: PhotosActionListener? = null
    private val colorPrimary: Int = CurrentTheme.getColorPrimary(mContext)

    private var currentPosition = -1
    fun updateCurrentPosition(currentPosition: Int) {
        this.currentPosition = currentPosition
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(
            LayoutInflater.from(mContext).inflate(R.layout.vk_photo_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, adapterPosition: Int) {
        bindPhotoViewHolder(holder, adapterPosition, data[adapterPosition])
    }

    fun setData(data: List<Photo>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    private fun bindPhotoViewHolder(holder: PhotoViewHolder, position: Int, photo: Photo) {
        if (FenrirNative.isNativeLoaded) {
            if (currentPosition == position) {
                holder.current.visibility = View.VISIBLE
                holder.current.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.donater_fire,
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
        holder.tvLike.text = AppTextUtils.getCounterWithK(photo.likesCount)
        holder.tvLike.visibility = if (photo.likesCount > 0) View.VISIBLE else View.GONE
        holder.ivLike.visibility = if (photo.likesCount > 0) View.VISIBLE else View.GONE
        holder.ivDownload.visibility = View.GONE
        holder.tvComment.text = AppTextUtils.getCounterWithK(photo.commentsCount)
        holder.tvComment.visibility = if (photo.commentsCount > 0) View.VISIBLE else View.GONE
        holder.ivComment.visibility = if (photo.commentsCount > 0) View.VISIBLE else View.GONE
        holder.bottomRoot.setBackgroundColor(mColorPrimaryWithAlpha)
        holder.bottomRoot.visibility =
            if (photo.likesCount + photo.commentsCount > 0) View.VISIBLE else View.GONE
        holder.bottomTop.visibility = View.VISIBLE
        holder.bottomTop.setBackgroundColor(mColorSecondaryWithAlpha)
        holder.tvDate.text = AppTextUtils.getDateFromUnixTimeShorted(mContext, photo.date)
        holder.setSelected(false)
        val targetUrl = photo.getUrlForSize(PhotoSize.Q, false)
        if (targetUrl.nonNullNoEmpty()) {
            with()
                .load(targetUrl)
                .tag(mPicassoTag)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        } else {
            with().cancelRequest(holder.photoImageView)
        }
        val clickListener = View.OnClickListener {
            mPhotosActionListener?.onPhotoClick(holder, photo)
        }
        holder.photoImageView.setOnClickListener(clickListener)
        holder.index.setOnClickListener(clickListener)
        holder.darkView.setOnClickListener(clickListener)
    }

    fun setPhotosActionListener(photosActionListener: PhotosActionListener?) {
        mPhotosActionListener = photosActionListener
    }

    interface PhotosActionListener {
        fun onPhotoClick(holder: PhotoViewHolder, photo: Photo)
    }

    class PhotoViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: ImageView = itemView.findViewById(R.id.imageView)
        val index: TextView = itemView.findViewById(R.id.item_photo_index)
        val darkView: View = itemView.findViewById(R.id.selected)
        val bottomRoot: ViewGroup = itemView.findViewById(R.id.vk_photo_item_bottom)
        val bottomTop: ViewGroup = itemView.findViewById(R.id.vk_photo_item_top)
        val tvLike: TextView = itemView.findViewById(R.id.vk_photo_item_like_counter)
        val tvDate: TextView = itemView.findViewById(R.id.vk_photo_item_date)
        val tvComment: TextView = itemView.findViewById(R.id.vk_photo_item_comment_counter)
        val ivLike: ImageView = itemView.findViewById(R.id.vk_photo_item_like)
        val ivComment: ImageView = itemView.findViewById(R.id.vk_photo_item_comment)
        val ivDownload: ImageView = itemView.findViewById(R.id.is_downloaded)
        val current: RLottieImageView = itemView.findViewById(R.id.current)
        fun setSelected(selected: Boolean) {
            index.visibility = if (selected) View.VISIBLE else View.GONE
            darkView.visibility = if (selected) View.VISIBLE else View.GONE
        }

    }

    init {
        data = photos
    }
}