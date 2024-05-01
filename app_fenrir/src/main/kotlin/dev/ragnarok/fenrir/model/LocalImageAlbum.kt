package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class LocalImageAlbum : Parcelable {
    var id = 0
        private set
    var name: String? = null
        private set
    var coverImageId: Long = 0
        private set
    var photoCount = 0
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        name = parcel.readString()
        coverImageId = parcel.readLong()
        photoCount = parcel.readInt()
    }

    fun setId(id: Int): LocalImageAlbum {
        this.id = id
        return this
    }

    fun setCoverId(coverImageId: Long): LocalImageAlbum {
        this.coverImageId = coverImageId
        return this
    }

    fun setName(name: String?): LocalImageAlbum {
        this.name = name
        return this
    }

    fun setPhotoCount(photoCount: Int): LocalImageAlbum {
        this.photoCount = photoCount
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(name)
        dest.writeLong(coverImageId)
        dest.writeInt(photoCount)
    }

    companion object CREATOR : Parcelable.Creator<LocalImageAlbum> {
        override fun createFromParcel(parcel: Parcel): LocalImageAlbum {
            return LocalImageAlbum(parcel)
        }

        override fun newArray(size: Int): Array<LocalImageAlbum?> {
            return arrayOfNulls(size)
        }
    }
}