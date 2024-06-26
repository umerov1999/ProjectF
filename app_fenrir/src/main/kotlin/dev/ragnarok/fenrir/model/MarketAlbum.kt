package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class MarketAlbum : AbsModel {
    val id: Int
    val owner_id: Long
    var access_key: String? = null
        private set
    var title: String? = null
        private set
    var photo: Photo? = null
        private set
    var count = 0
        private set
    var updated_time: Long = 0
        private set

    constructor(id: Int, owner_id: Long) {
        this.id = id
        this.owner_id = owner_id
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        owner_id = parcel.readLong()
        access_key = parcel.readString()
        count = parcel.readInt()
        updated_time = parcel.readLong()
        title = parcel.readString()
        photo = parcel.readTypedObjectCompat(Photo.CREATOR)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeLong(owner_id)
        parcel.writeString(access_key)
        parcel.writeInt(count)
        parcel.writeLong(updated_time)
        parcel.writeString(title)
        parcel.writeTypedObjectCompat(photo, flags)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_MARKET_ALBUM
    }

    fun setAccess_key(access_key: String?): MarketAlbum {
        this.access_key = access_key
        return this
    }

    fun setTitle(title: String?): MarketAlbum {
        this.title = title
        return this
    }

    fun setPhoto(photo: Photo?): MarketAlbum {
        this.photo = photo
        return this
    }

    fun setCount(count: Int): MarketAlbum {
        this.count = count
        return this
    }

    fun setUpdated_time(updated_time: Long): MarketAlbum {
        this.updated_time = updated_time
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MarketAlbum> {
        override fun createFromParcel(parcel: Parcel): MarketAlbum {
            return MarketAlbum(parcel)
        }

        override fun newArray(size: Int): Array<MarketAlbum?> {
            return arrayOfNulls(size)
        }
    }
}