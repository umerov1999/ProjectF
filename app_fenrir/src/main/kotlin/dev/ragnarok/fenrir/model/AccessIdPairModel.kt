package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class AccessIdPairModel : Parcelable {
    val id: Int
    val ownerId: Long
    val accessKey: String?

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

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
        parcel.writeLong(ownerId)
        parcel.writeString(accessKey)
    }

    companion object CREATOR : Parcelable.Creator<AccessIdPairModel> {
        override fun createFromParcel(parcel: Parcel): AccessIdPairModel {
            return AccessIdPairModel(parcel)
        }

        override fun newArray(size: Int): Array<AccessIdPairModel?> {
            return arrayOfNulls(size)
        }
    }
}