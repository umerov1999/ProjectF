package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.Identificable
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class Account : Parcelable, Identificable {
    private val id: Int
    val owner: Owner?

    constructor(id: Int, owner: Owner?) {
        this.id = id
        this.owner = owner
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        val wrapper = `in`.readTypedObjectCompat(
            ParcelableOwnerWrapper.CREATOR
        )
        owner = wrapper!!.get()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeTypedObjectCompat(ParcelableOwnerWrapper(owner), flags)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Account
        return id == that.id
    }

    override fun hashCode(): Int {
        return id
    }

    val displayName: String
        get() = owner?.fullName ?: id.toString()

    override fun getObjectId(): Int {
        return id
    }

    companion object CREATOR : Parcelable.Creator<Account> {
        override fun createFromParcel(parcel: Parcel): Account {
            return Account(parcel)
        }

        override fun newArray(size: Int): Array<Account?> {
            return arrayOfNulls(size)
        }
    }
}