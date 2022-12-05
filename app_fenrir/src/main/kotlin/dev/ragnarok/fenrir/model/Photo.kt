package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat
import kotlinx.serialization.Serializable

@Keep
@Serializable
class Photo : AbsModel, ISomeones, ParcelNative.ParcelableNative {
    private var id = 0
    override var ownerId = 0
        private set
    var albumId = 0
        private set
    var width = 0
        private set
    var height = 0
        private set
    var sizes: PhotoSizes? = null
        private set
    var text: String? = null
        private set
    var date: Long = 0
        private set
    var isUserLikes = false
        private set
    var isCanComment = false
        private set
    var likesCount = 0
        private set
    var repostsCount = 0
        private set
    var commentsCount = 0
        private set
    var tagsCount = 0
        private set
    var accessKey: String? = null
        private set
    var isDeleted = false
        private set
    var postId = 0
        private set
    var msgId = 0
        private set
    var msgPeerId = 0
        private set

    constructor()
    internal constructor(`in`: ParcelNative) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        albumId = `in`.readInt()
        width = `in`.readInt()
        height = `in`.readInt()
        sizes = `in`.readParcelable(PhotoSizes.NativeCreator)
        text = `in`.readString()
        date = `in`.readLong()
        isUserLikes = `in`.readBoolean()
        isCanComment = `in`.readBoolean()
        likesCount = `in`.readInt()
        commentsCount = `in`.readInt()
        tagsCount = `in`.readInt()
        accessKey = `in`.readString()
        isDeleted = `in`.readBoolean()
        postId = `in`.readInt()
        repostsCount = `in`.readInt()
        msgId = `in`.readInt()
        msgPeerId = `in`.readInt()
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        albumId = `in`.readInt()
        width = `in`.readInt()
        height = `in`.readInt()
        sizes = `in`.readTypedObjectCompat(PhotoSizes.CREATOR)
        text = `in`.readString()
        date = `in`.readLong()
        isUserLikes = `in`.getBoolean()
        isCanComment = `in`.getBoolean()
        likesCount = `in`.readInt()
        commentsCount = `in`.readInt()
        tagsCount = `in`.readInt()
        accessKey = `in`.readString()
        isDeleted = `in`.getBoolean()
        postId = `in`.readInt()
        repostsCount = `in`.readInt()
        msgId = `in`.readInt()
        msgPeerId = `in`.readInt()
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_PHOTO
    }

    override fun getObjectId(): Int {
        return id
    }

    fun setId(id: Int): Photo {
        this.id = id
        return this
    }

    fun setOwnerId(ownerId: Int): Photo {
        this.ownerId = ownerId
        return this
    }

    fun setAlbumId(albumId: Int): Photo {
        this.albumId = albumId
        return this
    }

    fun setWidth(width: Int): Photo {
        this.width = width
        return this
    }

    fun setHeight(height: Int): Photo {
        this.height = height
        return this
    }

    fun setSizes(sizes: PhotoSizes?): Photo {
        this.sizes = sizes
        return this
    }

    fun setText(text: String?): Photo {
        this.text = text
        return this
    }

    fun setDate(date: Long): Photo {
        this.date = date
        return this
    }

    fun setUserLikes(userLikes: Boolean): Photo {
        isUserLikes = userLikes
        return this
    }

    fun setCanComment(canComment: Boolean): Photo {
        isCanComment = canComment
        return this
    }

    fun setLikesCount(likesCount: Int): Photo {
        this.likesCount = likesCount
        return this
    }

    fun setRepostsCount(repostsCount: Int): Photo {
        this.repostsCount = repostsCount
        return this
    }

    fun setCommentsCount(commentsCount: Int): Photo {
        this.commentsCount = commentsCount
        return this
    }

    fun setTagsCount(tagsCount: Int): Photo {
        this.tagsCount = tagsCount
        return this
    }

    fun getUrlForSize(@PhotoSize size: Int, excludeNonAspectRatio: Boolean): String? {
        return sizes?.getUrlForSize(size, excludeNonAspectRatio)
    }

    fun setAccessKey(accessKey: String?): Photo {
        this.accessKey = accessKey
        return this
    }

    fun setDeleted(deleted: Boolean): Photo {
        isDeleted = deleted
        return this
    }

    fun setPostId(postId: Int): Photo {
        this.postId = postId
        return this
    }

    fun setMsgId(msgId: Int): Photo {
        this.msgId = msgId
        return this
    }

    fun setMsgPeerId(msgPeerId: Int): Photo {
        this.msgPeerId = msgPeerId
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(ownerId)
        parcel.writeInt(albumId)
        parcel.writeInt(width)
        parcel.writeInt(height)
        parcel.writeTypedObjectCompat(sizes, flags)
        parcel.writeString(text)
        parcel.writeLong(date)
        parcel.putBoolean(isUserLikes)
        parcel.putBoolean(isCanComment)
        parcel.writeInt(likesCount)
        parcel.writeInt(commentsCount)
        parcel.writeInt(tagsCount)
        parcel.writeString(accessKey)
        parcel.putBoolean(isDeleted)
        parcel.writeInt(postId)
        parcel.writeInt(repostsCount)
        parcel.writeInt(msgId)
        parcel.writeInt(msgPeerId)
    }

    fun generateWebLink(): String {
        return String.format("vk.com/photo%s_%s", ownerId, id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val photo = other as Photo
        return id == photo.id && ownerId == photo.ownerId
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + ownerId
        return result
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(id)
        dest.writeInt(ownerId)
        dest.writeInt(albumId)
        dest.writeInt(width)
        dest.writeInt(height)
        dest.writeParcelable(sizes)
        dest.writeString(text)
        dest.writeLong(date)
        dest.writeBoolean(isUserLikes)
        dest.writeBoolean(isCanComment)
        dest.writeInt(likesCount)
        dest.writeInt(commentsCount)
        dest.writeInt(tagsCount)
        dest.writeString(accessKey)
        dest.writeBoolean(isDeleted)
        dest.writeInt(postId)
        dest.writeInt(repostsCount)
        dest.writeInt(msgId)
        dest.writeInt(msgPeerId)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Photo> = object : Parcelable.Creator<Photo> {
            override fun createFromParcel(`in`: Parcel): Photo {
                return Photo(`in`)
            }

            override fun newArray(size: Int): Array<Photo?> {
                return arrayOfNulls(size)
            }
        }
        val NativeCreator: ParcelNative.Creator<Photo> =
            object : ParcelNative.Creator<Photo> {
                override fun readFromParcelNative(dest: ParcelNative): Photo {
                    return Photo(dest)
                }

            }
    }
}