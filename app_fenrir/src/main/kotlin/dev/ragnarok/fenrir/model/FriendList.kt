package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class FriendList : Parcelable {
    private val id: Int
    private val name: String?

    constructor(id: Int, name: String?) {
        this.id = id
        this.name = name
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        name = parcel.readString()
    }

    fun getId(): Int {
        return id
    }

    fun getName(): String? {
        return name
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
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