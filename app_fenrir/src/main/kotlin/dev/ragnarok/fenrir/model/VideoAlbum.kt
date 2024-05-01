package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class VideoAlbum : AbsModel {
    val id: Int
    val ownerId: Long
    var title: String? = null
        private set
    var count = 0
        private set
    var updatedTime: Long = 0
        private set
    var image: String? = null
        private set
    var privacy: SimplePrivacy? = null
        private set

    constructor(id: Int, ownerId: Long) {
        this.id = id
        this.ownerId = ownerId
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        ownerId = parcel.readLong()
        title = parcel.readString()
        count = parcel.readInt()
        updatedTime = parcel.readLong()
        image = parcel.readString()
        privacy = parcel.readTypedObjectCompat(SimplePrivacy.CREATOR)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeLong(ownerId)
        parcel.writeString(title)
        parcel.writeInt(count)
        parcel.writeLong(updatedTime)
        parcel.writeString(image)
        parcel.writeTypedObjectCompat(privacy, flags)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_VIDEO_ALBUM
    }

    fun setTitle(title: String?): VideoAlbum {
        this.title = title
        return this
    }

    fun setPrivacy(privacy: SimplePrivacy?): VideoAlbum {
        this.privacy = privacy
        return this
    }

    fun setCount(count: Int): VideoAlbum {
        this.count = count
        return this
    }

    fun setUpdatedTime(updatedTime: Long): VideoAlbum {
        this.updatedTime = updatedTime
        return this
    }

    fun setImage(image: String?): VideoAlbum {
        this.image = image
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VideoAlbum> {
        override fun createFromParcel(parcel: Parcel): VideoAlbum {
            return VideoAlbum(parcel)
        }

        override fun newArray(size: Int): Array<VideoAlbum?> {
            return arrayOfNulls(size)
        }
    }
}