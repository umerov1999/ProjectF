package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class TmpSource : Parcelable {
    val ownerId: Int
    val sourceId: Int

    constructor(ownerId: Int, sourceId: Int) {
        this.ownerId = ownerId
        this.sourceId = sourceId
    }

    internal constructor(parcel: Parcel) {
        ownerId = parcel.readInt()
        sourceId = parcel.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(ownerId)
        dest.writeInt(sourceId)
    }

    companion object CREATOR : Parcelable.Creator<TmpSource> {
        override fun createFromParcel(parcel: Parcel): TmpSource {
            return TmpSource(parcel)
        }

        override fun newArray(size: Int): Array<TmpSource?> {
            return arrayOfNulls(size)
        }
    }
}