package dev.ragnarok.fenrir.fragment.attachments.absattachmentsedit

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.fragment.base.holder.IdentificableHolder
import dev.ragnarok.fenrir.fragment.base.holder.SharedHolders
import dev.ragnarok.fenrir.model.AbsModelType
import dev.ragnarok.fenrir.model.Article
import dev.ragnarok.fenrir.model.AttachmentEntry
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioArtist
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Event
import dev.ragnarok.fenrir.model.Graffiti
import dev.ragnarok.fenrir.model.Link
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.MarketAlbum
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.view.CircleRoadProgress

class AttchmentsEditorAdapter(
    private val context: Context,
    items: MutableList<AttachmentEntry>,
    private val callback: Callback
) : RecyclerBindableAdapter<AttachmentEntry, AttchmentsEditorAdapter.ViewHolder>(items) {
    private val sharedHolders: SharedHolders<ViewHolder> = SharedHolders(false)
    override fun onBindItemViewHolder(viewHolder: ViewHolder, position: Int, type: Int) {
        val attachment = getItem(position)
        sharedHolders.put(attachment.id, viewHolder)
        configView(attachment, viewHolder)
        viewHolder.vRemove.setOnClickListener {
            val dataposition = getItemRawPosition(viewHolder.bindingAdapterPosition)
            callback.onRemoveClick(dataposition, attachment)
        }
    }

    fun cleanup() {
        sharedHolders.release()
    }

    override fun viewHolder(view: View, type: Int): ViewHolder {
        return ViewHolder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_post_attachments
    }

    fun updateEntityProgress(attachmentId: Int, progress: Int) {
        val holder = sharedHolders.findOneByEntityId(attachmentId)
        if (holder != null) {
            bindProgress(holder, progress, true)
        }
    }

    private fun bindProgress(holder: ViewHolder?, progress: Int, smoothly: Boolean) {
        val progressLine = "$progress%"
        holder?.tvTitle?.text = progressLine
        if (smoothly) {
            holder?.pbProgress?.changePercentageSmoothly(progress)
        } else {
            holder?.pbProgress?.changePercentage(progress)
        }
    }

    private fun configUploadObject(upload: Upload, holder: ViewHolder) {
        holder.pbProgress.visibility =
            if (upload.status == Upload.STATUS_UPLOADING) View.VISIBLE else View.GONE
        holder.vTint.visibility = View.VISIBLE
        val nonErrorTextColor = CurrentTheme.getPrimaryTextColorCode(holder.tvTitle.context)
        when (upload.status) {
            Upload.STATUS_ERROR -> {
                holder.tvTitle.setText(R.string.error)
                holder.tvTitle.setTextColor(ERROR_COLOR)
            }

            Upload.STATUS_QUEUE -> {
                holder.tvTitle.setText(R.string.in_order)
                holder.tvTitle.setTextColor(nonErrorTextColor)
            }

            Upload.STATUS_CANCELLING -> {
                holder.tvTitle.setText(R.string.cancelling)
                holder.tvTitle.setTextColor(nonErrorTextColor)
            }

            else -> {
                holder.tvTitle.setTextColor(nonErrorTextColor)
                val progressLine = upload.progress.toString() + "%"
                holder.tvTitle.text = progressLine
            }
        }
        holder.pbProgress.changePercentage(upload.progress)
        if (upload.hasThumbnail()) {
            with()
                .load(upload.buildThumnailUri())
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        } else {
            with().cancelRequest(holder.photoImageView)
            holder.photoImageView.setImageResource(R.drawable.background_gray)
        }
    }

    private fun bindLink(holder: ViewHolder, link: Link) {
        holder.tvTitle.setText(R.string.link)
        val photoLink =
            link.photo?.getUrlForSize(PhotoSize.X, false)
        if (photoLink.nonNullNoEmpty()) {
            with()
                .load(photoLink)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        } else {
            with().cancelRequest(holder.photoImageView)
            holder.photoImageView.setImageResource(R.drawable.background_gray)
        }
    }

    private fun bindArticle(holder: ViewHolder, link: Article) {
        holder.tvTitle.setText(R.string.article)
        val photoLink =
            link.photo?.getUrlForSize(PhotoSize.X, false)
        if (photoLink.nonNullNoEmpty()) {
            with()
                .load(photoLink)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        } else {
            with().cancelRequest(holder.photoImageView)
            holder.photoImageView.setImageResource(R.drawable.background_gray)
        }
    }

    private fun bindStory(holder: ViewHolder, story: Story) {
        holder.tvTitle.setText(R.string.story)
        val photoLink = story.owner?.maxSquareAvatar
        if (photoLink.nonNullNoEmpty()) {
            with()
                .load(photoLink)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        } else {
            with().cancelRequest(holder.photoImageView)
            holder.photoImageView.setImageResource(R.drawable.background_gray)
        }
    }

    private fun bindPhotoAlbum(holder: ViewHolder, album: PhotoAlbum) {
        holder.tvTitle.setText(R.string.photo_album)
        val photoLink = album.getSizes()?.getUrlForSize(
            PhotoSize.X,
            false
        )
        if (photoLink.nonNullNoEmpty()) {
            with()
                .load(photoLink)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        } else {
            with().cancelRequest(holder.photoImageView)
            holder.photoImageView.setImageResource(R.drawable.background_gray)
        }
    }

    private fun bindAudioPlaylist(holder: ViewHolder, playlist: AudioPlaylist) {
        holder.tvTitle.text = playlist.getTitle()
        val photoLink = playlist.getThumb_image()
        if (photoLink.nonNullNoEmpty()) {
            with()
                .load(photoLink)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        } else {
            with().cancelRequest(holder.photoImageView)
            holder.photoImageView.setImageResource(R.drawable.background_gray)
        }
    }

    private fun bindGraffiti(holder: ViewHolder, graffiti: Graffiti) {
        holder.tvTitle.setText(R.string.graffiti)
        val photoLink = graffiti.url
        if (photoLink.nonNullNoEmpty()) {
            with()
                .load(photoLink)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        } else {
            with().cancelRequest(holder.photoImageView)
            holder.photoImageView.setImageResource(R.drawable.background_gray)
        }
    }

    private fun bindCall(holder: ViewHolder) {
        holder.tvTitle.setText(R.string.call)
        with().cancelRequest(holder.photoImageView)
        holder.photoImageView.setImageResource(R.drawable.phone_call_color)
    }

    private fun bindGeo(holder: ViewHolder) {
        holder.tvTitle.setText(R.string.geo)
        with().cancelRequest(holder.photoImageView)
        holder.photoImageView.setImageResource(R.drawable.geo_color)
    }

    private fun bindEvent(holder: ViewHolder, event: Event) {
        holder.tvTitle.text = event.button_text
        with().cancelRequest(holder.photoImageView)
    }

    private fun bindMarket(holder: ViewHolder, market: Market) {
        holder.tvTitle.text = market.title
        if (market.thumb_photo.isNullOrEmpty()) {
            with().cancelRequest(holder.photoImageView)
        } else {
            with()
                .load(market.thumb_photo)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        }
    }

    private fun bindMarketAlbum(holder: ViewHolder, market_album: MarketAlbum) {
        holder.tvTitle.text = market_album.getTitle()
        if (market_album.getPhoto() == null) {
            with().cancelRequest(holder.photoImageView)
        } else {
            with()
                .load(market_album.getPhoto()?.getUrlForSize(PhotoSize.X, false))
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        }
    }

    private fun bindAudioArtist(holder: ViewHolder, artist: AudioArtist) {
        holder.tvTitle.text = artist.getName()
        if (artist.getMaxPhoto() == null) {
            with().cancelRequest(holder.photoImageView)
        } else {
            with()
                .load(artist.getMaxPhoto())
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        }
    }

    private fun bindNotSupported(holder: ViewHolder) {
        holder.tvTitle.setText(R.string.not_supported)
        with().cancelRequest(holder.photoImageView)
        holder.photoImageView.setImageResource(R.drawable.not_supported)
    }

    private fun bindPhoto(holder: ViewHolder, photo: Photo) {
        holder.tvTitle.setText(R.string.photo)
        with()
            .load(photo.getUrlForSize(PhotoSize.X, false))
            .placeholder(R.drawable.background_gray)
            .into(holder.photoImageView)
        holder.photoImageView.setOnClickListener(null)
    }

    private fun bindVideo(holder: ViewHolder, video: Video) {
        holder.tvTitle.text = video.title
        with()
            .load(video.image)
            .placeholder(R.drawable.background_gray)
            .into(holder.photoImageView)
        holder.photoImageView.setOnClickListener(null)
    }

    private fun bindAudio(holder: ViewHolder, audio: Audio) {
        if (audio.thumb_image_big.isNullOrEmpty()) {
            with().cancelRequest(holder.photoImageView)
            holder.photoImageView.setImageResource(
                if (Settings.get().ui().isDarkModeEnabled(
                        context
                    )
                ) R.drawable.generic_audio_nowplaying_dark else R.drawable.generic_audio_nowplaying_light
            )
        } else {
            with()
                .load(audio.thumb_image_big)
                .placeholder(R.drawable.background_gray)
                .into(holder.photoImageView)
        }
        val audiostr = audio.artist + " - " + audio.title
        holder.tvTitle.text = audiostr
        holder.photoImageView.setOnClickListener(null)
    }

    private fun bindPoll(holder: ViewHolder, poll: Poll) {
        with()
            .load(R.drawable.background_gray)
            .into(holder.photoImageView)
        holder.tvTitle.text = poll.question
        holder.photoImageView.setOnClickListener(null)
    }

    private fun bindPost(holder: ViewHolder, post: Post) {
        val postImgUrl = post.findFirstImageCopiesInclude()
        if (postImgUrl.isNullOrEmpty()) {
            with()
                .load(R.drawable.background_gray)
                .into(holder.photoImageView)
        } else {
            with()
                .load(postImgUrl)
                .into(holder.photoImageView)
        }
        holder.tvTitle.setText(R.string.attachment_wall_post)
        holder.photoImageView.setOnClickListener(null)
    }

    private fun bindDoc(holder: ViewHolder, document: Document) {
        val previewUrl = document.getPreviewWithSize(PhotoSize.X, false)
        if (previewUrl.nonNullNoEmpty()) {
            with()
                .load(previewUrl)
                .into(holder.photoImageView)
        } else {
            with()
                .load(R.drawable.background_gray)
                .into(holder.photoImageView)
        }
        holder.photoImageView.setOnClickListener(null)
        holder.tvTitle.text = document.title
    }

    private fun bindFwdMessages(holder: ViewHolder) {
        with()
            .load(R.drawable.background_gray)
            .into(holder.photoImageView)
        holder.tvTitle.text = context.getString(R.string.title_messages)
    }

    private fun bindWallReplies(holder: ViewHolder) {
        with()
            .load(R.drawable.background_gray)
            .into(holder.photoImageView)
        holder.tvTitle.text = context.getString(R.string.comments)
    }

    private fun configView(item: AttachmentEntry, holder: ViewHolder) {
        holder.vRemove.visibility = if (item.isCanDelete) View.VISIBLE else View.GONE
        val model = item.attachment
        holder.pbProgress.visibility = View.GONE
        holder.vTint.visibility = View.GONE
        when (model.getModelType()) {
            AbsModelType.MODEL_PHOTO -> {
                bindPhoto(holder, model as Photo)
            }

            AbsModelType.MODEL_VIDEO -> {
                bindVideo(holder, model as Video)
            }

            AbsModelType.MODEL_AUDIO -> {
                bindAudio(holder, model as Audio)
            }

            AbsModelType.MODEL_POLL -> {
                bindPoll(holder, model as Poll)
            }

            AbsModelType.MODEL_POST -> {
                bindPost(holder, model as Post)
            }

            AbsModelType.MODEL_DOCUMENT -> {
                bindDoc(holder, model as Document)
            }

            AbsModelType.MODEL_FWDMESSAGES -> {
                bindFwdMessages(holder)
            }

            AbsModelType.MODEL_UPLOAD -> {
                configUploadObject(model as Upload, holder)
            }

            AbsModelType.MODEL_LINK -> {
                bindLink(holder, model as Link)
            }

            AbsModelType.MODEL_ARTICLE -> {
                bindArticle(holder, model as Article)
            }

            AbsModelType.MODEL_STORY -> {
                bindStory(holder, model as Story)
            }

            AbsModelType.MODEL_CALL -> {
                bindCall(holder)
            }

            AbsModelType.MODEL_GEO -> {
                bindGeo(holder)
            }

            AbsModelType.MODEL_NOT_SUPPORTED -> {
                bindNotSupported(holder)
            }

            AbsModelType.MODEL_EVENT -> {
                bindEvent(holder, model as Event)
            }

            AbsModelType.MODEL_MARKET -> {
                bindMarket(holder, model as Market)
            }

            AbsModelType.MODEL_MARKET_ALBUM -> {
                bindMarketAlbum(holder, model as MarketAlbum)
            }

            AbsModelType.MODEL_AUDIO_ARTIST -> {
                bindAudioArtist(holder, model as AudioArtist)
            }

            AbsModelType.MODEL_AUDIO_PLAYLIST -> {
                bindAudioPlaylist(holder, model as AudioPlaylist)
            }

            AbsModelType.MODEL_GRAFFITI -> {
                bindGraffiti(holder, model as Graffiti)
            }

            AbsModelType.MODEL_PHOTO_ALBUM -> {
                bindPhotoAlbum(holder, model as PhotoAlbum)
            }

            AbsModelType.MODEL_WALL_REPLY -> {
                bindWallReplies(holder)
            }

            else -> {
                throw UnsupportedOperationException("Type " + model.javaClass + " in not supported")
            }
        }
    }

    interface Callback {
        fun onRemoveClick(dataposition: Int, entry: AttachmentEntry)
    }

    class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        IdentificableHolder {
        val photoImageView: ImageView = itemView.findViewById(R.id.item_attachment_image)
        val tvTitle: TextView = itemView.findViewById(R.id.item_attachment_title)
        val vRemove: View = itemView.findViewById(R.id.item_attachment_progress_root)
        val pbProgress: CircleRoadProgress = itemView.findViewById(R.id.item_attachment_progress)
        val vTint: View = itemView.findViewById(R.id.item_attachment_tint)
        val vTitleRoot: View = itemView.findViewById(R.id.item_attachment_title_root)
        override val holderId: Int
            get() = itemView.tag as Int

        init {
            itemView.tag = generateNextHolderId()
        }
    }

    companion object {
        private val ERROR_COLOR = Color.parseColor("#ff0000")
        private var idGenerator = 0
        internal fun generateNextHolderId(): Int {
            idGenerator++
            return idGenerator
        }
    }

}