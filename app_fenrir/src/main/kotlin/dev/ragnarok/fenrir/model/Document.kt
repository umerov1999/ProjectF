package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class Document : AbsModel {
    val id: Int
    val ownerId: Int
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

    @DocType
    var type = 0
        private set
    var accessKey: String? = null
        private set
    var photoPreview: PhotoSizes? = null
        private set
    var videoPreview: VideoPreview? = null
        private set
    var graffiti: Graffiti? = null
        private set
    var msgId = 0
        private set
    var msgPeerId = 0
        private set

    constructor(id: Int, ownerId: Int) {
        this.id = id
        this.ownerId = ownerId
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        ownerId = parcel.readInt()
        title = parcel.readString()
        size = parcel.readLong()
        ext = parcel.readString()
        url = parcel.readString()
        date = parcel.readLong()
        type = parcel.readInt()
        accessKey = parcel.readString()
        photoPreview = parcel.readTypedObjectCompat(PhotoSizes.CREATOR)
        videoPreview = parcel.readTypedObjectCompat(VideoPreview.CREATOR)
        graffiti = parcel.readTypedObjectCompat(Graffiti.CREATOR)
        msgId = parcel.readInt()
        msgPeerId = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeString(title)
        parcel.writeLong(size)
        parcel.writeString(ext)
        parcel.writeString(url)
        parcel.writeLong(date)
        parcel.writeInt(type)
        parcel.writeString(accessKey)
        parcel.writeTypedObjectCompat(photoPreview, flags)
        parcel.writeTypedObjectCompat(videoPreview, flags)
        parcel.writeTypedObjectCompat(graffiti, flags)
        parcel.writeInt(msgId)
        parcel.writeInt(msgPeerId)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_DOCUMENT
    }

    fun getPreviewWithSize(@PhotoSize size: Int, excludeNonAspectRatio: Boolean): String? {
        return photoPreview?.getUrlForSize(
            size,
            excludeNonAspectRatio
        )
    }

    fun setPhotoPreview(photoPreview: PhotoSizes?): Document {
        this.photoPreview = photoPreview
        return this
    }

    fun setVideoPreview(videoPreview: VideoPreview?): Document {
        this.videoPreview = videoPreview
        return this
    }

    fun getMaxPreviewSize(excludeNonAspectRatio: Boolean): PhotoSizes.Size? {
        return photoPreview?.getMaxSize(excludeNonAspectRatio)
    }

    fun setGraffiti(graffiti: Graffiti?): Document {
        this.graffiti = graffiti
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setTitle(title: String?): Document {
        this.title = title
        return this
    }

    fun setSize(size: Long): Document {
        this.size = size
        return this
    }

    fun setExt(ext: String?): Document {
        this.ext = ext
        return this
    }

    fun setUrl(url: String?): Document {
        this.url = url
        return this
    }

    fun setDate(date: Long): Document {
        this.date = date
        return this
    }

    fun setType(type: Int): Document {
        this.type = type
        return this
    }

    fun setAccessKey(accessKey: String?): Document {
        this.accessKey = accessKey
        return this
    }

    fun generateWebLink(): String {
        return String.format("vk.com/doc%s_%s", ownerId, id)
    }

    val isGif: Boolean
        get() = "gif" == ext

    fun hasValidGifVideoLink(): Boolean {
        return videoPreview?.src.nonNullNoEmpty()
    }

    fun setMsgId(msgId: Int): Document {
        this.msgId = msgId
        return this
    }

    fun setMsgPeerId(msgPeerId: Int): Document {
        this.msgPeerId = msgPeerId
        return this
    }

    class Graffiti : AbsModel {
        var src: String? = null
            private set
        var width = 0
            private set
        var height = 0
            private set

        constructor()
        internal constructor(parcel: Parcel) {
            src = parcel.readString()
            width = parcel.readInt()
            height = parcel.readInt()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(src)
            parcel.writeInt(width)
            parcel.writeInt(height)
        }

        @AbsModelType
        override fun getModelType(): Int {
            return AbsModelType.MODEL_DOCUMENT_GRAFFITI
        }

        fun setSrc(src: String?): Graffiti {
            this.src = src
            return this
        }

        fun setWidth(width: Int): Graffiti {
            this.width = width
            return this
        }

        fun setHeight(height: Int): Graffiti {
            this.height = height
            return this
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Graffiti> {
            override fun createFromParcel(parcel: Parcel): Graffiti {
                return Graffiti(parcel)
            }

            override fun newArray(size: Int): Array<Graffiti?> {
                return arrayOfNulls(size)
            }
        }
    }

    class VideoPreview : AbsModel {
        var src: String? = null
            private set
        var width = 0
            private set
        var height = 0
            private set
        var fileSize: Long = 0
            private set

        constructor()
        internal constructor(parcel: Parcel) {
            src = parcel.readString()
            width = parcel.readInt()
            height = parcel.readInt()
            fileSize = parcel.readLong()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(src)
            parcel.writeInt(width)
            parcel.writeInt(height)
            parcel.writeLong(fileSize)
        }

        @AbsModelType
        override fun getModelType(): Int {
            return AbsModelType.MODEL_DOCUMENT_VIDEO_PREVIEW
        }

        fun setSrc(src: String?): VideoPreview {
            this.src = src
            return this
        }

        fun setFileSize(fileSize: Long): VideoPreview {
            this.fileSize = fileSize
            return this
        }

        fun setWidth(width: Int): VideoPreview {
            this.width = width
            return this
        }

        fun setHeight(height: Int): VideoPreview {
            this.height = height
            return this
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<VideoPreview> {
            override fun createFromParcel(parcel: Parcel): VideoPreview {
                return VideoPreview(parcel)
            }

            override fun newArray(size: Int): Array<VideoPreview?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<Document> {
        override fun createFromParcel(parcel: Parcel): Document {
            return Document(parcel)
        }

        override fun newArray(size: Int): Array<Document?> {
            return arrayOfNulls(size)
        }
    }
}