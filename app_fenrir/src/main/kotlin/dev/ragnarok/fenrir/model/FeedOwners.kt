package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class FeedOwners : Parcelable {
    var id: Long
        private set

    var title: String? = null
        private set
    var owners = ArrayList<Owner>()
        private set

    constructor(id: Long) {
        this.id = id
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readLong()
        title = parcel.readString()
        ParcelableOwnerWrapper.readOwners(parcel)?.let {
            owners.addAll(it)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(title)
        ParcelableOwnerWrapper.writeOwners(dest, flags, owners)
    }

    fun setId(id: Long): FeedOwners {
        this.id = id
        return this
    }

    fun setTitle(title: String?): FeedOwners {
        this.title = title
        return this
    }

    fun setOwners(owners: List<Owner>): FeedOwners {
        this.owners.clear()
        this.owners.addAll(owners)
        return this
    }

    companion object CREATOR : Parcelable.Creator<FeedOwners> {
        override fun createFromParcel(parcel: Parcel): FeedOwners {
            return FeedOwners(parcel)
        }

        override fun newArray(size: Int): Array<FeedOwners?> {
            return arrayOfNulls(size)
        }
    }
}
