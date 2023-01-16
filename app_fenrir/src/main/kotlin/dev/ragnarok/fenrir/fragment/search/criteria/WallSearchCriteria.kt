package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable

class WallSearchCriteria : BaseSearchCriteria {
    val ownerId: Long

    constructor(query: String?, ownerId: Long) : super(query) {
        this.ownerId = ownerId
    }

    internal constructor(parcel: Parcel) : super(parcel) {
        ownerId = parcel.readLong()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeLong(ownerId)
    }

    companion object CREATOR : Parcelable.Creator<WallSearchCriteria> {
        override fun createFromParcel(parcel: Parcel): WallSearchCriteria {
            return WallSearchCriteria(parcel)
        }

        override fun newArray(size: Int): Array<WallSearchCriteria?> {
            return arrayOfNulls(size)
        }
    }
}