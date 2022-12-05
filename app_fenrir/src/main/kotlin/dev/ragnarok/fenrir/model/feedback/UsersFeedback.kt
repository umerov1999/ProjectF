package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.readOwners
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.writeOwners

class UsersFeedback : Feedback {
    var owners: List<Owner>? = null
        private set

    constructor(@FeedbackType type: Int) : super(type)
    internal constructor(`in`: Parcel) : super(`in`) {
        owners = readOwners(`in`)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        writeOwners(dest, flags, owners)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setOwners(owners: List<Owner>?): UsersFeedback {
        this.owners = owners
        return this
    }

    companion object CREATOR : Parcelable.Creator<UsersFeedback> {
        override fun createFromParcel(parcel: Parcel): UsersFeedback {
            return UsersFeedback(parcel)
        }

        override fun newArray(size: Int): Array<UsersFeedback?> {
            return arrayOfNulls(size)
        }
    }
}