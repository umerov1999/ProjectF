package dev.ragnarok.fenrir.dialog.feedbacklink

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AbsModelType
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Topic
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class FeedbackLinkAdapter(
    private val mContext: Context,
    private val mData: List<AbsModel>,
    private val mActionListener: ActionListener
) : RecyclerView.Adapter<FeedbackLinkAdapter.ViewHolder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feedback_link, parent, false)
        )
    }

    @SuppressLint("SwitchIntDef")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mData[position]
        var title: String? = null
        when (item.getModelType()) {
            AbsModelType.MODEL_USER -> {
                title = (item as User).fullName
                holder.mSubtitle.setText(R.string.open_profile)
                holder.ivImage.visibility = View.VISIBLE
                displayAvatar(holder.ivImage, transformation, item.maxSquareAvatar, null)
            }

            AbsModelType.MODEL_POST -> {
                title = (item as Post).textCopiesInclude
                holder.mSubtitle.setText(R.string.open_post)
                val imageUrl = item.findFirstImageCopiesInclude(PhotoSize.M, false)
                if (imageUrl.isNullOrEmpty()) {
                    holder.ivImage.visibility = View.GONE
                } else {
                    holder.ivImage.visibility = View.VISIBLE
                    with()
                        .load(imageUrl)
                        .into(holder.ivImage)
                }
            }

            AbsModelType.MODEL_COMMENT -> {
                title = (item as Comment).text
                holder.mSubtitle.setText(R.string.jump_to_comment)
                val senderAvatar = item.maxAuthorAvaUrl
                holder.ivImage.visibility =
                    if (senderAvatar.isNullOrEmpty()) View.GONE else View.VISIBLE
                displayAvatar(holder.ivImage, transformation, senderAvatar, null)
            }

            AbsModelType.MODEL_PHOTO -> {
                title = (item as Photo).text
                holder.mSubtitle.setText(R.string.show_photo)
                val imgUrl = item.getUrlForSize(PhotoSize.M, false)
                if (imgUrl.isNullOrEmpty()) {
                    holder.ivImage.visibility = View.GONE
                } else {
                    holder.ivImage.visibility = View.VISIBLE
                    with()
                        .load(imgUrl)
                        .into(holder.ivImage)
                }
            }

            AbsModelType.MODEL_VIDEO -> {
                val imgUrl = (item as Video).image
                title = item.title
                holder.mSubtitle.setText(R.string.show_video)
                if (imgUrl.isNullOrEmpty()) {
                    holder.ivImage.visibility = View.GONE
                } else {
                    holder.ivImage.visibility = View.VISIBLE
                    with()
                        .load(imgUrl)
                        .into(holder.ivImage)
                }
            }

            AbsModelType.MODEL_TOPIC -> {
                title = (item as Topic).title
                holder.mSubtitle.setText(R.string.open_topic)
                holder.ivImage.visibility = View.GONE
            }
        }
        val spannableTitle = OwnerLinkSpanFactory.withSpans(
            title,
            owners = true,
            topics = true,
            listener = null
        )
        holder.mTitle.setText(spannableTitle, TextView.BufferType.SPANNABLE)
        holder.mTitle.visibility =
            if (title.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    interface ActionListener {
        fun onPostClick(post: Post)
        fun onCommentClick(comment: Comment)
        fun onTopicClick(topic: Topic)
        fun onPhotoClick(photo: Photo)
        fun onVideoClick(video: Video)
        fun onUserClick(user: User)
    }

    @SuppressLint("SwitchIntDef")
    inner class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val mTitle: TextView = root.findViewById(R.id.item_feedback_link_text)
        val mSubtitle: TextView = root.findViewById(R.id.item_feedback_link_text2)
        val ivImage: ImageView = root.findViewById(R.id.item_feedback_link_image)

        init {
            val ivForward = root.findViewById<ImageView>(R.id.item_feedback_link_forward)
            Utils.setColorFilter(ivForward, CurrentTheme.getColorPrimary(mContext))
            root.setOnClickListener {
                val item = mData[bindingAdapterPosition]
                when (item.getModelType()) {
                    AbsModelType.MODEL_USER -> {
                        mActionListener.onUserClick(item as User)
                    }

                    AbsModelType.MODEL_POST -> {
                        mActionListener.onPostClick(item as Post)
                    }

                    AbsModelType.MODEL_COMMENT -> {
                        mActionListener.onCommentClick(item as Comment)
                    }

                    AbsModelType.MODEL_PHOTO -> {
                        mActionListener.onPhotoClick(item as Photo)
                    }

                    AbsModelType.MODEL_VIDEO -> {
                        mActionListener.onVideoClick(item as Video)
                    }

                    AbsModelType.MODEL_TOPIC -> {
                        mActionListener.onTopicClick(item as Topic)
                    }
                }
            }
        }
    }

}