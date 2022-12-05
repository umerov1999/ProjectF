package dev.ragnarok.fenrir.fragment.search.criteria

import android.os.Parcel
import android.os.Parcelable

class WallSearchCriteria : BaseSearchCriteria {
    val ownerId: Int

    constructor(query: String?, ownerId: Int) : super(query) {
        this.ownerId = ownerId
    }

    internal constructor(`in`: Parcel) : super(`in`) {
        ownerId = `in`.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(ownerId)
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