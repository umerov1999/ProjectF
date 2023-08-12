package dev.ragnarok.fenrir.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.OnAttachmentsActionCallback
import dev.ragnarok.fenrir.fragment.base.PostImage
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.mozaik.MozaikLayout
import dev.ragnarok.fenrir.view.natives.animation.AnimatedShapeableImageView
import dev.ragnarok.fenrir.view.natives.animation.AspectRatioAnimatedShapeableImageView
import dev.ragnarok.fenrir.view.zoomhelper.ZoomHelper.Companion.addZoomableView
import dev.ragnarok.fenrir.view.zoomhelper.ZoomHelper.Companion.removeZoomableView

class PhotosViewHelper internal constructor(
    private val context: Context,
    private val attachmentsActionCallback: OnAttachmentsActionCallback
) {
    @PhotoSize
    private val mPhotoPreviewSize = Settings.get().main().prefPreviewImageSize
    private val isAutoplayGif = Settings.get().other().isAutoplay_gif

    @SuppressLint("SetTextI18n")
    fun displayVideos(videos: List<PostImage>, container: ViewGroup) {
        container.visibility = if (videos.isEmpty()) View.GONE else View.VISIBLE
        if (videos.isEmpty()) {
            return
        }
        val i = videos.size - container.childCount
        for (j in 0 until i) {
            val root = LayoutInflater.from(context)
                .inflate(R.layout.item_video_attachment, container, false)
            container.addView(root)
        }
        for (g in 0 until container.childCount) {
            val tmpV = container.getChildAt(g)
            var holder: VideoHolder? =
                if (tmpV.tag is VideoHolder) tmpV.tag as VideoHolder else null
            if (holder == null) {
                holder = VideoHolder(tmpV)
                tmpV.tag = holder
            }
            if (g < videos.size) {
                val image = videos[g]
                holder.vgVideo.setOnClickListener {
                    if (image.type == PostImage.TYPE_VIDEO) {
                        val video = image.attachment as Video
                        attachmentsActionCallback.onVideoPlay(video)
                    }
                }
                val url = image.getPreviewUrl(mPhotoPreviewSize)
                if (image.type == PostImage.TYPE_VIDEO) {
                    val video = image.attachment as Video
                    holder.tvDelay.text = AppTextUtils.getDurationString(video.duration)
                    holder.tvTitle.text = Utils.firstNonEmptyString(video.title, " ")

                    if (isAutoplayGif && video.trailer.nonNullNoEmpty()) {
                        PicassoInstance.with().cancelRequest(holder.vgVideo)
                        holder.vgVideo.setDecoderCallback(object :
                            AnimatedShapeableImageView.OnDecoderInit {
                            override fun onLoaded(success: Boolean) {
                                if (!success) {
                                    if (url.nonNullNoEmpty()) {
                                        PicassoInstance.with()
                                            .load(url)
                                            .placeholder(R.drawable.background_gray)
                                            .tag(Constants.PICASSO_TAG)
                                            .into(holder.vgVideo)
                                        tmpV.visibility = View.VISIBLE
                                    } else {
                                        PicassoInstance.with().cancelRequest(holder.vgVideo)
                                        tmpV.visibility = View.GONE
                                    }
                                }
                            }
                        })
                        holder.vgVideo.fromNet(
                            (video.ownerId.toString() + "_" + image.attachment.id.toString()),
                            video.trailer,
                            Utils.createOkHttp(Constants.GIF_TIMEOUT, true)
                        )
                    } else if (url.nonNullNoEmpty()) {
                        PicassoInstance.with()
                            .load(url)
                            .placeholder(R.drawable.background_gray)
                            .tag(Constants.PICASSO_TAG)
                            .into(holder.vgVideo)
                        tmpV.visibility = View.VISIBLE
                    } else {
                        PicassoInstance.with().cancelRequest(holder.vgVideo)
                        tmpV.visibility = View.GONE
                    }
                } else {
                    if (url.nonNullNoEmpty()) {
                        PicassoInstance.with()
                            .load(url)
                            .placeholder(R.drawable.background_gray)
                            .tag(Constants.PICASSO_TAG)
                            .into(holder.vgVideo)
                        tmpV.visibility = View.VISIBLE
                    } else {
                        PicassoInstance.with().cancelRequest(holder.vgVideo)
                        tmpV.visibility = View.GONE
                    }
                }
            } else {
                tmpV.visibility = View.GONE
            }
        }
    }

    fun removeZoomable(container: ViewGroup?) {
        if (container == null)
            return
        for (g in 0 until container.childCount) {
            removeZoomableView(container.getChildAt(g))
        }
    }

    fun displayPhotos(photos: List<PostImage>, container: ViewGroup) {
        container.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
        if (photos.isEmpty()) {
            removeZoomable(container)
            return
        }
        var images = photos
        if (container is MozaikLayout) {
            if (photos.size > 10) {
                images = ArrayList(10)
                for (s in 0..9) images.add(photos[s])
            }
        }
        val roundedMode = Settings.get().main().photoRoundMode
        if (roundedMode == 1) {
            if (images.size > 1 && container.childCount == 1 || images.size == 1 && container.childCount > 1) {
                container.removeAllViews()
            }
        }
        val i = images.size - container.childCount
        for (j in 0 until i) {
            val root: View = when (roundedMode) {
                1 -> {
                    if (images.size > 1) LayoutInflater.from(context).inflate(
                        R.layout.item_photo_gif_not_round,
                        container,
                        false
                    ) else LayoutInflater.from(context)
                        .inflate(R.layout.item_photo_gif, container, false)
                }

                2 -> {
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_photo_gif_not_round, container, false)
                }

                else -> {
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_photo_gif, container, false)
                }
            }
            val holder = Holder(root)
            root.tag = holder
            container.addView(root)
        }
        if (container is MozaikLayout) {
            container.setPhotos(images)
        }
        for (g in 0 until container.childCount) {
            val tmpV = container.getChildAt(g)
            var holder: Holder? = if (tmpV.tag is Holder) tmpV.tag as Holder else null
            if (holder == null) {
                holder = Holder(tmpV)
                tmpV.tag = holder
            }
            if (g < images.size) {
                addZoomableView(tmpV, holder)
                val image = images[g]
                holder.ivPlay.visibility =
                    if (image.type == PostImage.TYPE_IMAGE) View.GONE else View.VISIBLE
                holder.tvTitle.visibility =
                    if (image.type == PostImage.TYPE_IMAGE) View.GONE else View.VISIBLE
                holder.vgPhoto.setOnClickListener {
                    when (image.type) {
                        PostImage.TYPE_IMAGE -> openImages(photos, g)
                        PostImage.TYPE_VIDEO -> {
                            val video = image.attachment as Video
                            attachmentsActionCallback.onVideoPlay(video)
                        }

                        PostImage.TYPE_GIF -> {
                            val document = image.attachment as Document
                            attachmentsActionCallback.onDocPreviewOpen(document)
                        }
                    }
                }
                val url = image.getPreviewUrl(mPhotoPreviewSize)
                when (image.type) {
                    PostImage.TYPE_VIDEO -> {
                        val video = image.attachment as Video
                        holder.tvTitle.text = AppTextUtils.getDurationString(video.duration)
                    }

                    PostImage.TYPE_GIF -> {
                        val document = image.attachment as Document
                        holder.tvTitle.text = context.getString(
                            R.string.gif,
                            AppTextUtils.getSizeString(document.size)
                        )
                    }
                }
                if (isAutoplayGif && image.type == PostImage.TYPE_GIF) {
                    PicassoInstance.with().cancelRequest(holder.vgPhoto)
                    holder.vgPhoto.setDecoderCallback(object :
                        AnimatedShapeableImageView.OnDecoderInit {
                        override fun onLoaded(success: Boolean) {
                            if (!success) {
                                holder.ivPlay.visibility = View.VISIBLE
                                if (url.nonNullNoEmpty()) {
                                    PicassoInstance.with()
                                        .load(url)
                                        .placeholder(R.drawable.background_gray)
                                        .tag(Constants.PICASSO_TAG)
                                        .into(holder.vgPhoto)
                                    tmpV.visibility = View.VISIBLE
                                } else {
                                    PicassoInstance.with().cancelRequest(holder.vgPhoto)
                                    tmpV.visibility = View.GONE
                                }
                            } else {
                                holder.ivPlay.visibility = View.GONE
                            }
                        }
                    })
                    holder.vgPhoto.fromNet(
                        ((image.attachment as Document).ownerId.toString() + "_" + image.attachment.id.toString()),
                        image.attachment.videoPreview?.src,
                        Utils.createOkHttp(Constants.GIF_TIMEOUT, true)
                    )
                } else if (url.nonNullNoEmpty()) {
                    PicassoInstance.with()
                        .load(url)
                        .placeholder(R.drawable.background_gray)
                        .tag(Constants.PICASSO_TAG)
                        .into(holder.vgPhoto)
                    tmpV.visibility = View.VISIBLE
                } else {
                    PicassoInstance.with().cancelRequest(holder.vgPhoto)
                    tmpV.visibility = View.GONE
                }
            } else {
                removeZoomableView(tmpV)
                tmpV.visibility = View.GONE
            }
        }
    }

    private fun openImages(photos: List<PostImage>, index: Int) {
        if (photos.isEmpty() || photos.size <= index) {
            return
        }
        val models = ArrayList<Photo>()
        for (postImage in photos) {
            if (postImage.type == PostImage.TYPE_IMAGE) {
                models.add(postImage.attachment as Photo)
            }
        }
        if (models.isEmpty()) {
            return
        }
        attachmentsActionCallback.onPhotosOpen(models, index, true)
    }

    private class Holder(itemView: View) {
        val vgPhoto: AnimatedShapeableImageView = itemView.findViewById(R.id.item_video_image)
        val ivPlay: ImageView = itemView.findViewById(R.id.item_video_play)
        val tvTitle: TextView = itemView.findViewById(R.id.item_video_title)

    }

    private class VideoHolder(itemView: View) {
        val vgVideo: AspectRatioAnimatedShapeableImageView =
            itemView.findViewById(R.id.item_video_album_image)
        val tvTitle: TextView = itemView.findViewById(R.id.item_video_album_title)
        val tvDelay: TextView = itemView.findViewById(R.id.item_video_album_count)

    }
}
