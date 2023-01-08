package dev.ragnarok.fenrir.model.database

import android.os.Parcel
import android.os.Parcelable

class Faculty : Parcelable {
    val id: Int
    val title: String?

    constructor(id: Int, title: String?) {
        this.id = id
        this.title = title
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        title = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
    }

    companion object CREATOR : Parcelable.Creator<Faculty> {
        override fun createFromParcel(parcel: Parcel): Faculty {
            return Faculty(parcel)
        }

        override fun newArray(size: Int): Array<Faculty?> {
            return arrayOfNulls(size)
        }
    }
}