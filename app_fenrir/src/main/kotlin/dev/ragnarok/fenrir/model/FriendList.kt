package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class FriendList : Parcelable {
    private val id: Long
    private val name: String?

    constructor(id: Long, name: String?) {
        this.id = id
        this.name = name
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readLong()
        name = parcel.readString()
    }

    fun getId(): Long {
        return id
    }

    fun getName(): String? {
        return name
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
    }

    companion object CREATOR : Parcelable.Creator<FriendList> {
        override fun createFromParcel(parcel: Parcel): FriendList {
            return FriendList(parcel)
        }

        override fun newArray(size: Int): Array<FriendList?> {
            return arrayOfNulls(size)
        }
    }
}