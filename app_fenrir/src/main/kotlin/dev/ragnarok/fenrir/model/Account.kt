package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.interfaces.IdentificableOwner
import dev.ragnarok.fenrir.readTypedObjectCompat

class Account : Parcelable, IdentificableOwner {
    private val id: Long
    val owner: Owner?

    constructor(id: Long, owner: Owner?) {
        this.id = id
        this.owner = owner
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readLong()
        val wrapper = parcel.readTypedObjectCompat(
            ParcelableOwnerWrapper.CREATOR
        )
        owner = wrapper?.owner
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        ParcelableOwnerWrapper.writeOwner(dest, flags, owner)
    }

    override fun equals(other: Any?): Boolean {
        return other is Account && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    val displayName: String
        get() = owner?.fullName ?: id.toString()

    override fun getOwnerObjectId(): Long {
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