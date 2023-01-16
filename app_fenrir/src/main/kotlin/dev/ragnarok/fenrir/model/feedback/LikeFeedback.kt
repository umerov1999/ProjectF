package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.readModel
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.writeModel
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.readOwners
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.writeOwners

class LikeFeedback : Feedback {
    var liked: AbsModel? = null
        private set
    var owners: List<Owner>? = null
        private set

    // one of FeedbackType.LIKE_PHOTO, FeedbackType.LIKE_POST, FeedbackType.LIKE_VIDEO
    constructor(@FeedbackType type: Int) : super(type)
    internal constructor(parcel: Parcel) : super(parcel) {
        liked = readModel(parcel)
        owners = readOwners(parcel)
    }

    override fun getModelType(): Int {
        return FeedbackModelType.MODEL_LIKE_FEEDBACK
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        writeModel(dest, flags, liked)
        writeOwners(dest, flags, owners)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setOwners(owners: List<Owner>?): LikeFeedback {
        this.owners = owners
        return this
    }

    fun setLiked(liked: AbsModel?): LikeFeedback {
        this.liked = liked
        return this
    }

    companion object CREATOR : Parcelable.Creator<LikeFeedback> {
        override fun createFromParcel(parcel: Parcel): LikeFeedback {
            return LikeFeedback(parcel)
        }

        override fun newArray(size: Int): Array<LikeFeedback?> {
            return arrayOfNulls(size)
        }
    }
}