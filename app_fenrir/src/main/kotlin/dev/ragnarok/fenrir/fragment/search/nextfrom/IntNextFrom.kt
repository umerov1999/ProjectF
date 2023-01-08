package dev.ragnarok.fenrir.fragment.search.nextfrom

import android.os.Parcel
import android.os.Parcelable

class IntNextFrom : AbsNextFrom {
    var offset: Int

    constructor(initValue: Int) {
        offset = initValue
    }

    internal constructor(parcel: Parcel) {
        offset = parcel.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(offset)
    }

    override fun reset() {
        offset = 0
    }

    companion object CREATOR : Parcelable.Creator<IntNextFrom> {
        override fun createFromParcel(parcel: Parcel): IntNextFrom {
            return IntNextFrom(parcel)
        }

        override fun newArray(size: Int): Array<IntNextFrom?> {
            return arrayOfNulls(size)
        }
    }
}