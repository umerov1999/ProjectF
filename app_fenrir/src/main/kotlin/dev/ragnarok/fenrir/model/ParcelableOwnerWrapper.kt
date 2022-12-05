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

    internal constructor(`in`: Parcel) {
        type = `in`.readInt()
        isNull = `in`.getBoolean()
        owner = if (!isNull) {
            if (type == OwnerType.USER) {
                `in`.readTypedObjectCompat(User.CREATOR)
            } else {
                `in`.readTypedObjectCompat(Community.CREATOR)
            }
        } else {
            null
        }
    }

    internal constructor(`in`: ParcelNative) {
        type = `in`.readInt()
        isNull = `in`.readBoolean()
        owner = if (!isNull) {
            if (type == OwnerType.USER) {
                `in`.readParcelable(User.NativeCreator)
            } else {
                `in`.readParcelable(Community.NativeCreator)
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
                override fun createFromParcel(`in`: Parcel): ParcelableOwnerWrapper {
                    return ParcelableOwnerWrapper(`in`)
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

        fun readOwner(`in`: ParcelNative): Owner? {
            return `in`.readParcelable(NativeCreator)?.get()
        }

        fun writeOwner(dest: ParcelNative, owner: Owner?) {
            dest.writeParcelable(ParcelableOwnerWrapper(owner))
        }

        fun readOwner(`in`: Parcel): Owner? {
            return `in`.readTypedObjectCompat(CREATOR)
                ?.get()
        }

        fun writeOwner(dest: Parcel, flags: Int, owner: Owner?) {
            dest.writeTypedObjectCompat(ParcelableOwnerWrapper(owner), flags)
        }

        fun readOwners(`in`: Parcel): List<Owner>? {
            val isNull = `in`.getBoolean()
            if (isNull) {
                return null
            }
            val ownersCount = `in`.readInt()
            val owners: MutableList<Owner> = ArrayList(ownersCount)
            for (i in 0 until ownersCount) {
                readOwner(`in`)?.let { owners.add(it) }
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

        fun readOwners(`in`: ParcelNative): List<Owner>? {
            val isNull = `in`.readBoolean()
            if (isNull) {
                return null
            }
            val ownersCount = `in`.readInt()
            val owners: MutableList<Owner> = ArrayList(ownersCount)
            for (i in 0 until ownersCount) {
                readOwner(`in`)?.let { owners.add(it) }
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