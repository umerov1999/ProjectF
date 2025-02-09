package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class Document : AbsModel, ParcelNative.ParcelableNative {
    val id: Int
    val ownerId: Long
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
    var msgPeerId = 0L
        private set

    constructor(id: Int, ownerId: Long) {
        this.id = id
        this.ownerId = ownerId
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        ownerId = parcel.readLong()
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
        msgPeerId = parcel.readLong()
    }

    internal constructor(parcel: ParcelNative) {
        id = parcel.readInt()
        ownerId = parcel.readLong()
        title = parcel.readString()
        size = parcel.readLong()
        ext = parcel.readString()
        url = parcel.readString()
        date = parcel.readLong()
        type = parcel.readInt()
        accessKey = parcel.readString()
        photoPreview = parcel.readParcelable(PhotoSizes.NativeCreator)
        videoPreview = parcel.readParcelable(VideoPreview.NativeCreator)
        graffiti = parcel.readParcelable(Graffiti.NativeCreator)
        msgId = parcel.readInt()
        msgPeerId = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeLong(ownerId)
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
        parcel.writeLong(msgPeerId)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(id)
        dest.writeLong(ownerId)
        dest.writeString(title)
        dest.writeLong(size)
        dest.writeString(ext)
        dest.writeString(url)
        dest.writeLong(date)
        dest.writeInt(type)
        dest.writeString(accessKey)
        dest.writeParcelable(photoPreview)
        dest.writeParcelable(videoPreview)
        dest.writeParcelable(graffiti)
        dest.writeInt(msgId)
        dest.writeLong(msgPeerId)
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

    fun setMsgPeerId(msgPeerId: Long): Document {
        this.msgPeerId = msgPeerId
        return this
    }

    class Graffiti : AbsModel, ParcelNative.ParcelableNative {
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

        internal constructor(parcel: ParcelNative) {
            src = parcel.readString()
            width = parcel.readInt()
            height = parcel.readInt()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(src)
            parcel.writeInt(width)
            parcel.writeInt(height)
        }

        override fun writeToParcelNative(dest: ParcelNative) {
            dest.writeString(src)
            dest.writeInt(width)
            dest.writeInt(height)
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

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<Graffiti> = object : Parcelable.Creator<Graffiti> {
                override fun createFromParcel(parcel: Parcel): Graffiti {
                    return Graffiti(parcel)
                }

                override fun newArray(size: Int): Array<Graffiti?> {
                    return arrayOfNulls(size)
                }
            }
            val NativeCreator: ParcelNative.Creator<Graffiti> =
                object : ParcelNative.Creator<Graffiti> {
                    override fun readFromParcelNative(dest: ParcelNative): Graffiti {
                        return Graffiti(dest)
                    }
                }
        }
    }

    class VideoPreview : AbsModel, ParcelNative.ParcelableNative {
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

        internal constructor(parcel: ParcelNative) {
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

        override fun writeToParcelNative(dest: ParcelNative) {
            dest.writeString(src)
            dest.writeInt(width)
            dest.writeInt(height)
            dest.writeLong(fileSize)
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

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<VideoPreview> =
                object : Parcelable.Creator<VideoPreview> {
                    override fun createFromParcel(parcel: Parcel): VideoPreview {
                        return VideoPreview(parcel)
                    }

                    override fun newArray(size: Int): Array<VideoPreview?> {
                        return arrayOfNulls(size)
                    }
                }
            val NativeCreator: ParcelNative.Creator<VideoPreview> =
                object : ParcelNative.Creator<VideoPreview> {
                    override fun readFromParcelNative(dest: ParcelNative): VideoPreview {
                        return VideoPreview(dest)
                    }
                }
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Document> = object : Parcelable.Creator<Document> {
            override fun createFromParcel(parcel: Parcel): Document {
                return Document(parcel)
            }

            override fun newArray(size: Int): Array<Document?> {
                return arrayOfNulls(size)
            }
        }
        val NativeCreator: ParcelNative.Creator<Document> =
            object : ParcelNative.Creator<Document> {
                override fun readFromParcelNative(dest: ParcelNative): Document {
                    return Document(dest)
                }
            }
    }
}