package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class IdPair : Parcelable {
    val id: Int
    val ownerId: Int

    constructor(id: Int, ownerId: Int) {
        this.id = id
        this.ownerId = ownerId
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        ownerId = parcel.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeInt(ownerId)
    }

    val isValid: Boolean
        get() = id != 0 && ownerId != 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val idPair = other as IdPair
        return if (id != idPair.id) false else ownerId == idPair.ownerId
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + ownerId
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