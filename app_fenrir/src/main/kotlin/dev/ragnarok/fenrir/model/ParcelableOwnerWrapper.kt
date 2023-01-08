package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat


class ParcelableOwnerWrapper : Parcelable, ParcelNative.ParcelableNative {
    private val type: Int
    private val isNull: Boolean
    private val owner: Owner?

    constructor(owner: Owner?) {
        this.owner = owner
        type = owner?.ownerType ?: 0
        isNull = owner == null
    }

    internal constructor(parcel: Parcel) {
        type = parcel.readInt()
        isNull = parcel.getBoolean()
        owner = if (!isNull) {
            if (type == OwnerType.USER) {
                parcel.readTypedObjectCompat(User.CREATOR)
            } else {
                parcel.readTypedObjectCompat(Community.CREATOR)
            }
        } else {
            null
        }
    }

    internal constructor(parcel: ParcelNative) {
        type = parcel.readInt()
        isNull = parcel.readBoolean()
        owner = if (!isNull) {
            if (type == OwnerType.USER) {
                parcel.readParcelable(User.NativeCreator)
            } else {
                parcel.readParcelable(Community.NativeCreator)
            }
        } else {
            null
        }
    }

    fun get(): Owner? {
        return owner
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(type)
        dest.putBoolean(isNull)
        if (!isNull) {
            dest.writeTypedObjectCompat(owner, flags)
        }
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(type)
        dest.writeBoolean(isNull)
        if (!isNull) {
            dest.writeParcelable(owner)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ParcelableOwnerWrapper> =
            object : Parcelable.Creator<ParcelableOwnerWrapper> {
                override fun createFromParcel(parcel: Parcel): ParcelableOwnerWrapper {
                    return ParcelableOwnerWrapper(parcel)
                }

                override fun newArray(size: Int): Array<ParcelableOwnerWrapper?> {
                    return arrayOfNulls(size)
                }
            }
        val NativeCreator: ParcelNative.Creator<ParcelableOwnerWrapper> =
            object : ParcelNative.Creator<ParcelableOwnerWrapper> {
                override fun readFromParcelNative(dest: ParcelNative): ParcelableOwnerWrapper {
                    return ParcelableOwnerWrapper(
                        dest
                    )
                }

            }

        fun wrap(owner: Owner?): ParcelableOwnerWrapper {
            return ParcelableOwnerWrapper(owner)
        }

        fun readOwner(parcel: ParcelNative): Owner? {
            return parcel.readParcelable(NativeCreator)?.get()
        }

        fun writeOwner(dest: ParcelNative, owner: Owner?) {
            dest.writeParcelable(ParcelableOwnerWrapper(owner))
        }

        fun readOwner(parcel: Parcel): Owner? {
            return parcel.readTypedObjectCompat(CREATOR)
                ?.get()
        }

        fun writeOwner(dest: Parcel, flags: Int, owner: Owner?) {
            dest.writeTypedObjectCompat(ParcelableOwnerWrapper(owner), flags)
        }

        fun readOwners(parcel: Parcel): List<Owner>? {
            val isNull = parcel.getBoolean()
            if (isNull) {
                return null
            }
            val ownersCount = parcel.readInt()
            val owners: MutableList<Owner> = ArrayList(ownersCount)
            for (i in 0 until ownersCount) {
                readOwner(parcel)?.let { owners.add(it) }
            }
            return owners
        }

        fun writeOwners(dest: Parcel, flags: Int, owners: List<Owner>?) {
            if (owners == null) {
                dest.writeInt(1)
                return
            }
            dest.writeInt(0)
            dest.writeInt(owners.size)
            for (owner in owners) {
                writeOwner(dest, flags, owner)
            }
        }

        fun readOwners(parcel: ParcelNative): List<Owner>? {
            val isNull = parcel.readBoolean()
            if (isNull) {
                return null
            }
            val ownersCount = parcel.readInt()
            val owners: MutableList<Owner> = ArrayList(ownersCount)
            for (i in 0 until ownersCount) {
                readOwner(parcel)?.let { owners.add(it) }
            }
            return owners
        }

        fun writeOwners(dest: ParcelNative, owners: List<Owner>?) {
            if (owners == null) {
                dest.writeBoolean(true)
                return
            }
            dest.writeBoolean(false)
            dest.writeInt(owners.size)
            for (owner in owners) {
                writeOwner(dest, owner)
            }
        }
    }
}