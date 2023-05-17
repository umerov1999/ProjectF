package dev.ragnarok.fenrir.fragment.fave.favephotos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView

class FavePhotosAdapter(context: Context, private var data: List<Photo>) :
    RecyclerView.Adapter<FavePhotosAdapter.ViewHolder>() {
    private val colorPrimary: Int = CurrentTheme.getColorPrimary(context)
    private var photoSelectionListener: PhotoSelectionListener? = null
    private var photoConversationListener: PhotoConversationListener? = null
    private var currentPosition = -1
    var attachedRecyclerView: RecyclerView? = null
        private set

    fun updateCurrentPosition(currentPosition: Int) {
        this.currentPosition = currentPosition
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val root =
            LayoutInflater.from(parent.context).inflate(R.layout.item_fave_photo, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val photo = data[position]
        viewHolder.tvLike.text = AppTextUtils.getCounterWithK(photo.likesCount)
        viewHolder.tvLike.visibility = if (photo.likesCount > 0) View.VISIBLE else View.GONE
        viewHolder.ivLike.visibility = if (photo.likesCount > 0) View.VISIBLE else View.GONE
        viewHolder.tvComment.text = AppTextUtils.getCounterWithK(photo.commentsCount)
        viewHolder.tvComment.visibility = if (photo.commentsCount > 0) View.VISIBLE else View.GONE
        viewHolder.ivComment.visibility = if (photo.commentsCount > 0) View.VISIBLE else View.GONE
        viewHolder.vgBottom.setBackgroundColor(Utils.adjustAlpha(colorPrimary, 0.75f))
        viewHolder.vgBottom.visibility =
            if (photo.likesCount + photo.commentsCount > 0) View.VISIBLE else View.GONE
        if (FenrirNative.isNativeLoaded) {
            if (currentPosition == position) {
                viewHolder.current.visibility = View.VISIBLE
                viewHolder.current.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.donater_fire,
                    Utils.dp(100f),
                    Utils.dp(100f),
                    intArrayOf(0xFF812E, colorPrimary),
                    true
                )
                viewHolder.current.playAnimation()
            } else {
                viewHolder.current.visibility = View.GONE
                viewHolder.current.clearAnimationDrawable()
            }
        }
        with()
            .load(photo.getUrlForSize(PhotoSize.X, false))
            .tag(Constants.PICASSO_TAG)
            .placeholder(R.drawable.background_gray)
            .into(viewHolder.photoImageView)
        viewHolder.cardView.setOnClickListener {
            photoSelectionListener?.onPhotoClicked(viewHolder.bindingAdapterPosition, photo)
        }
        viewHolder.cardView.setOnLongClickListener {
            if (photoConversationListener != null) {
                photoConversationListener?.onGoPhotoConversation(photo)
                return@setOnLongClickListener true
            }
            false
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<Photo>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setPhotoSelectionListener(photoSelectionListener: PhotoSelectionListener?) {
        this.photoSelectionListener = photoSelectionListener
    }

    fun setPhotoConversationListener(photoConversationListener: PhotoConversationListener?) {
        this.photoConversationListener = photoConversationListener
    }

    interface PhotoSelectionListener {
        fun onPhotoClicked(position: Int, photo: Photo)
    }

    interface PhotoConversationListener {
        fun onGoPhotoConversation(photo: Photo)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        attachedRecyclerView = null
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: View = itemView.findViewById(R.id.card_view)
        val photoImageView: ImageView = itemView.findViewById(R.id.imageView)
        val vgBottom: ViewGroup = itemView.findViewById(R.id.vk_photo_item_bottom)
        val tvLike: TextView = itemView.findViewById(R.id.vk_photo_item_like_counter)
        val tvComment: TextView = itemView.findViewById(R.id.vk_photo_item_comment_counter)
        val ivLike: ImageView = itemView.findViewById(R.id.vk_photo_item_like)
        val ivComment: ImageView = itemView.findViewById(R.id.vk_photo_item_comment)
        val current: RLottieImageView = itemView.findViewById(R.id.current)
    }
}