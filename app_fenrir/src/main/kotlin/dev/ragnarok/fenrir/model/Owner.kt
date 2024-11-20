package dev.ragnarok.fenrir.model

import android.os.Parcel
import dev.ragnarok.fenrir.api.model.interfaces.IdentificableOwner
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import kotlinx.serialization.Serializable

@Serializable
sealed class Owner : AbsModel, IdentificableOwner, ParcelNative.ParcelableNative {
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

    open val ownerId: Long
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
}