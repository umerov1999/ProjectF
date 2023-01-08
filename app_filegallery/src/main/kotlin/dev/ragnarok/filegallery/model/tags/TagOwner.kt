package dev.ragnarok.filegallery.model.tags

import android.os.Parcel
import android.os.Parcelable

class TagOwner : Parcelable {
    var id = 0
        private set
    var name: String? = null
        private set
    var count = 0
        private set

    constructor()
    constructor(parcel: Parcel) {
        id = parcel.readInt()
        name = parcel.readString()
        count = parcel.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeInt(count)
    }

    fun setCount(count: Int): TagOwner {
        this.count = count
        return this
    }

    fun setId(id: Int): TagOwner {
        this.id = id
        return this
    }

    fun setName(name: String?): TagOwner {
        this.name = name
        return this
    }

    companion object CREATOR : Parcelable.Creator<TagOwner> {
        override fun createFromParcel(parcel: Parcel): TagOwner {
            return TagOwner(parcel)
        }

        override fun newArray(size: Int): Array<TagOwner?> {
            return arrayOfNulls(size)
        }
    }
}