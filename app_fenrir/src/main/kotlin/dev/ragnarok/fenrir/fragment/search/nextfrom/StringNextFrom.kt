package dev.ragnarok.fenrir.fragment.search.nextfrom

import android.os.Parcel
import android.os.Parcelable

class StringNextFrom : AbsNextFrom {
    var nextFrom: String?

    constructor(nextFrom: String?) {
        this.nextFrom = nextFrom
    }

    internal constructor(parcel: Parcel) {
        nextFrom = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(nextFrom)
    }

    override fun reset() {
        nextFrom = null
    }

    companion object CREATOR : Parcelable.Creator<StringNextFrom> {
        override fun createFromParcel(parcel: Parcel): StringNextFrom {
            return StringNextFrom(parcel)
        }

        override fun newArray(size: Int): Array<StringNextFrom?> {
            return arrayOfNulls(size)
        }
    }
}