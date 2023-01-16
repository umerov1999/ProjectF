package dev.ragnarok.fenrir.db.model.entity

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@SerialName("doc")
class DocumentDboEntity : DboEntity() {
    var id = 0
        private set
    var ownerId = 0L
        private set
    var title: String? = null
        private set
    var size: Long = 0
        private set
    var ext: String? = null
        private set
    var url: String? = null
        private set
    var date: Long = 0
        private set
    var type = 0
        private set
    var accessKey: String? = null
        private set
    var photo: PhotoSizeEntity? = null
        private set
    var graffiti: GraffitiDbo? = null
        private set
    var video: VideoPreviewDbo? = null
        private set

    operator fun set(id: Int, ownerId: Long): DocumentDboEntity {
        this.id = id
        this.ownerId = ownerId
        return this
    }

    fun setTitle(title: String?): DocumentDboEntity {
        this.title = title
        return this
    }

    fun setSize(size: Long): DocumentDboEntity {
        this.size = size
        return this
    }

    fun setExt(ext: String?): DocumentDboEntity {
        this.ext = ext
        return this
    }

    fun setUrl(url: String?): DocumentDboEntity {
        this.url = url
        return this
    }

    fun setDate(date: Long): DocumentDboEntity {
        this.date = date
        return this
    }

    fun setType(type: Int): DocumentDboEntity {
        this.type = type
        return this
    }

    fun setAccessKey(accessKey: String?): DocumentDboEntity {
        this.accessKey = accessKey
        return this
    }

    fun setPhoto(photo: PhotoSizeEntity?): DocumentDboEntity {
        this.photo = photo
        return this
    }

    fun setGraffiti(graffiti: GraffitiDbo?): DocumentDboEntity {
        this.graffiti = graffiti
        return this
    }

    fun setVideo(video: VideoPreviewDbo?): DocumentDboEntity {
        this.video = video
        return this
    }

    @Keep
    @Serializable
    class VideoPreviewDbo {
        var src: String? = null
            private set

        var width = 0
            private set

        var height = 0
            private set

        var fileSize: Long = 0
            private set

        operator fun set(src: String?, width: Int, height: Int, fileSize: Long): VideoPreviewDbo {
            this.src = src
            this.width = width
            this.height = height
            this.fileSize = fileSize
            return this
        }
    }

    @Keep
    @Serializable
    class GraffitiDbo {
        var src: String? = null
            private set
        var width = 0
            private set
        var height = 0
            private set

        operator fun set(src: String?, width: Int, height: Int): GraffitiDbo {
            this.src = src
            this.width = width
            this.height = height
            return this
        }
    }
}