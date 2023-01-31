package dev.ragnarok.fenrir.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.annotation.StringRes
import kotlinx.serialization.Serializable

@Keep
@Serializable
class DeltaOwner : Parcelable {
    var ownerId: Long = 0
        private set
    val time: Long
    var content: ArrayList<DeltaOwnerList> = ArrayList()
        private set

    constructor() {
        time = System.currentTimeMillis() / 1000L
    }

    fun setOwner(ownerId: Long): DeltaOwner {
        this.ownerId = ownerId
        return this
    }

    fun appendToList(context: Context, @StringRes name: Int, list: List<Owner>?): DeltaOwner {
        if (list.isNullOrEmpty()) {
            return this
        }
        val ret = DeltaOwnerList()
        ret.name = context.getString(name)
        ret.ownerList = list
        content.add(ret)
        return this
    }

    internal constructor(parcel: Parcel) {
        time = parcel.readLong()
        ownerId = parcel.readLong()
        content = parcel.createTypedArrayList(DeltaOwnerList.CREATOR) ?: ArrayList()
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeLong(time)
        parcel.writeLong(ownerId)
        parcel.writeTypedList(content)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DeltaOwner> {
        override fun createFromParcel(parcel: Parcel): DeltaOwner {
            return DeltaOwner(parcel)
        }

        override fun newArray(size: Int): Array<DeltaOwner?> {
            return arrayOfNulls(size)
        }
    }

    @Keep
    @Serializable
    class DeltaOwnerList : Parcelable {
        var name: String? = null
        var ownerList: List<Owner> = ArrayList()

        constructor()

        internal constructor(parcel: Parcel) {
            name = parcel.readString()
            ownerList = ParcelableOwnerWrapper.readOwners(parcel) ?: ArrayList()
        }

        override fun writeToParcel(parcel: Parcel, i: Int) {
            parcel.writeString(name)
            ParcelableOwnerWrapper.writeOwners(parcel, i, ownerList)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<DeltaOwnerList> {
            override fun createFromParcel(parcel: Parcel): DeltaOwnerList {
                return DeltaOwnerList(parcel)
            }

            override fun newArray(size: Int): Array<DeltaOwnerList?> {
                return arrayOfNulls(size)
            }
        }
    }
}