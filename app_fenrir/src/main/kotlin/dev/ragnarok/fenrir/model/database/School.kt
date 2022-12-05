package dev.ragnarok.fenrir.model.database

import android.os.Parcel
import android.os.Parcelable

class School : Parcelable {
    val id: Int
    val title: String?

    constructor(id: Int, title: String?) {
        this.id = id
        this.title = title
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        title = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
    }

    companion object CREATOR : Parcelable.Creator<School> {
        override fun createFromParcel(parcel: Parcel): School {
            return School(parcel)
        }

        override fun newArray(size: Int): Array<School?> {
            return arrayOfNulls(size)
        }
    }
}