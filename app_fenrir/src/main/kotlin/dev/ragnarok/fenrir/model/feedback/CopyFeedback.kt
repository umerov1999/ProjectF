package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.readModel
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.writeModel
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.readOwners
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.writeOwners

class CopyFeedback : Feedback {
    var what: AbsModel? = null
        private set
    var owners: List<Owner>? = null
        private set

    constructor(@FeedbackType type: Int) : super(type)
    internal constructor(parcel: Parcel) : super(parcel) {
        what = readModel(parcel)
        owners = readOwners(parcel)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        writeModel(dest, flags, what)
        writeOwners(dest, flags, owners)
    }

    override fun getModelType(): Int {
        return FeedbackModelType.MODEL_COPY_FEEDBACK
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setWhat(what: AbsModel?): CopyFeedback {
        this.what = what
        return this
    }

    fun setOwners(owners: List<Owner>?): CopyFeedback {
        this.owners = owners
        return this
    }

    companion object CREATOR : Parcelable.Creator<CopyFeedback> {
        override fun createFromParcel(parcel: Parcel): CopyFeedback {
            return CopyFeedback(parcel)
        }

        override fun newArray(size: Int): Array<CopyFeedback?> {
            return arrayOfNulls(size)
        }
    }
}