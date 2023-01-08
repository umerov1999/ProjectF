package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class PostSource : Parcelable {
    private val type: Int
    private val platform: String?
    private val data: Int
    private val url: String?

    constructor(type: Int, platform: String?, data: Int, url: String?) {
        this.type = type
        this.platform = platform
        this.data = data
        this.url = url
    }

    internal constructor(parcel: Parcel) {
        type = parcel.readInt()
        platform = parcel.readString()
        data = parcel.readInt()
        url = parcel.readString()
    }

    fun getUrl(): String? {
        return url
    }

    fun getPlatform(): String? {
        return platform
    }

    fun getData(): Int {
        return data
    }

    fun getType(): Int {
        return type
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(type)
        parcel.writeString(platform)
        parcel.writeInt(data)
        parcel.writeString(url)
    }

    companion object CREATOR : Parcelable.Creator<PostSource> {
        override fun createFromParcel(parcel: Parcel): PostSource {
            return PostSource(parcel)
        }

        override fun newArray(size: Int): Array<PostSource?> {
            return arrayOfNulls(size)
        }
    }
}