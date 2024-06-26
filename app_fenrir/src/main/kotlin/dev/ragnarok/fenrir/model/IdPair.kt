package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class IdPair : Parcelable {
    val id: Int
    val ownerId: Long

    constructor(id: Int, ownerId: Long) {
        this.id = id
        this.ownerId = ownerId
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        ownerId = parcel.readLong()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeLong(ownerId)
    }

    val isValid: Boolean
        get() = id != 0 && ownerId != 0L

    override fun equals(other: Any?): Boolean {
        return other is IdPair && id == other.id && ownerId == other.ownerId
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + ownerId.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<IdPair> {
        override fun createFromParcel(parcel: Parcel): IdPair {
            return IdPair(parcel)
        }

        override fun newArray(size: Int): Array<IdPair?> {
            return arrayOfNulls(size)
        }
    }
}