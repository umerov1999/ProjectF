package dev.ragnarok.fenrir.fragment.base

import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.view.mozaik.PostImagePosition

class PostImage(val attachment: AbsModel, val type: Int) {
    var position: PostImagePosition? = null

    fun setPosition(position: PostImagePosition?): PostImage {
        this.position = position
        return this
    }

    val width: Int
        get() = when (type) {
            TYPE_IMAGE -> {
                val photo = attachment as Photo
                if (photo.width == 0) 100 else photo.width
            }

            TYPE_VIDEO -> 640
            TYPE_GIF -> {
                val document = attachment as Document
                val max = document.getMaxPreviewSize(false)
                max?.getW() ?: 640
            }

            else -> throw UnsupportedOperationException()
        }

    fun getPreviewUrl(@PhotoSize photoPreviewSize: Int): String? {
        when (type) {
            TYPE_IMAGE -> {
                val photo = attachment as Photo
                val size = photo.sizes?.getSize(photoPreviewSize, true)
                return size?.url
            }

            TYPE_VIDEO -> {
                val video = attachment as Video
                return video.image
            }

            TYPE_GIF -> {
                val document = attachment as Document
                return document.getPreviewWithSize(photoPreviewSize, false)
            }
        }
        throw UnsupportedOperationException()
    }

    val height: Int
        get() = when (type) {
            TYPE_IMAGE -> {
                val photo = attachment as Photo
                if (photo.height == 0) 100 else photo.height
            }

            TYPE_VIDEO -> 360
            TYPE_GIF -> {
                val document = attachment as Document
                val max = document.getMaxPreviewSize(false)
                max?.getH() ?: 480
            }

            else -> throw UnsupportedOperationException()
        }
    val aspectRatio: Float
        get() = width.toFloat() / height.toFloat()

    companion object {
        const val TYPE_IMAGE = 1
        const val TYPE_VIDEO = 2
        const val TYPE_GIF = 3
    }
}