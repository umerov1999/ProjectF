package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class ShortLink : AbsModel {
    var short_url: String? = null
        private set
    var url: String? = null
        private set
    var timestamp: Long = 0
        private set
    var key: String? = null
        private set
    var access_key: String? = null
        private set
    var views = 0
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        short_url = parcel.readString()
        url = parcel.readString()
        timestamp = parcel.readLong()
        key = parcel.readString()
        views = parcel.readInt()
        access_key = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(short_url)
        parcel.writeString(url)
        parcel.writeLong(timestamp)
        parcel.writeString(key)
        parcel.writeInt(views)
        parcel.writeString(access_key)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_SHORT_LINK
    }

    fun setShort_url(short_url: String?): ShortLink {
        this.short_url = short_url
        return this
    }

    fun setUrl(url: String?): ShortLink {
        this.url = url
        return this
    }

    fun setTimestamp(timestamp: Long): ShortLink {
        this.timestamp = timestamp
        return this
    }

    fun setKey(key: String?): ShortLink {
        this.key = key
        return this
    }

    fun setAccess_key(access_key: String?): ShortLink {
        this.access_key = access_key
        return this
    }

    fun setViews(views: Int): ShortLink {
        this.views = views
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ShortLink> {
        override fun createFromParcel(parcel: Parcel): ShortLink {
            return ShortLink(parcel)
        }

        override fun newArray(size: Int): Array<ShortLink?> {
            return arrayOfNulls(size)
        }
    }
}