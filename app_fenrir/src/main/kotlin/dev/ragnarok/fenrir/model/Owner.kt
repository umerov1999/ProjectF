package dev.ragnarok.fenrir.model

import android.os.Parcel
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat
import kotlinx.serialization.Serializable

@Serializable
sealed class Owner : AbsModel, ParcelNative.ParcelableNative {
    @OwnerType
    val ownerType: Int

    constructor(ownerType: Int) {
        this.ownerType = ownerType
    }

    constructor(parcel: Parcel) {
        ownerType = parcel.readInt()
    }

    constructor(parcel: ParcelNative) {
        ownerType = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ownerType)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(ownerType)
    }

    open val ownerId: Int
        get() {
            throw UnsupportedOperationException()
        }
    open val domain: String?
        get() {
            throw UnsupportedOperationException()
        }
    open val maxSquareAvatar: String?
        get() {
            throw UnsupportedOperationException()
        }
    open val originalAvatar: String?
        get() {
            throw UnsupportedOperationException()
        }

    open fun get100photoOrSmaller(): String? {
        throw UnsupportedOperationException()
    }

    open val fullName: String?
        get() {
            throw UnsupportedOperationException()
        }
    open val isVerified: Boolean
        get() {
            throw UnsupportedOperationException()
        }
    open val isDonated: Boolean
        get() {
            throw UnsupportedOperationException()
        }
    open val isHasUnseenStories: Boolean
        get() {
            throw UnsupportedOperationException()
        }

    companion object {
        fun readOwnerFromParcel(parcel: Parcel): Owner? {
            val ownerType = parcel.readInt()
            return if (ownerType == OwnerType.COMMUNITY) parcel.readTypedObjectCompat(Community.CREATOR) else parcel.readTypedObjectCompat(
                User.CREATOR
            )
        }

        fun readOwnerFromParcel(id: Int, parcel: Parcel): Owner? {
            return if (id <= 0) parcel.readTypedObjectCompat(Community.CREATOR) else parcel.readTypedObjectCompat(
                User.CREATOR
            )
        }

        fun writeOwnerToParcel(owner: Owner, dest: Parcel, flags: Int) {
            dest.writeInt(owner.ownerType)
            dest.writeTypedObjectCompat(owner, flags)
        }
    }
}