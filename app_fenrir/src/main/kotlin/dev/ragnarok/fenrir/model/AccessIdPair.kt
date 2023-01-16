package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class AccessIdPair : Parcelable {
    private val id: Int
    private val ownerId: Long
    private val accessKey: String?

    constructor(id: Int, ownerId: Long, accessKey: String?) {
        this.id = id
        this.ownerId = ownerId
        this.accessKey = accessKey
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        ownerId = parcel.readLong()
        accessKey = parcel.readString()
    }

    fun getId(): Int {
        return id
    }

    fun getOwnerId(): Long {
        return ownerId
    }

    fun getAccessKey(): String? {
        return accessKey
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
        parcel.writeLong(ownerId)
        parcel.writeString(accessKey)
    }

    companion object CREATOR : Parcelable.Creator<AccessIdPair> {
        override fun createFromParcel(parcel: Parcel): AccessIdPair {
            return AccessIdPair(parcel)
        }

        override fun newArray(size: Int): Array<AccessIdPair?> {
            return arrayOfNulls(size)
        }
    }
}