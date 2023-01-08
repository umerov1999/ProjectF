package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.readModel
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.writeModel
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.readOwners
import dev.ragnarok.fenrir.model.ParcelableOwnerWrapper.Companion.writeOwners
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class LikeCommentFeedback : Feedback {
    var liked: Comment? = null
        private set
    var commented: AbsModel? = null
        private set
    var owners: List<Owner>? = null
        private set

    // one of FeedbackType.LIKE_COMMENT, FeedbackType.LIKE_COMMENT_PHOTO, FeedbackType.LIKE_COMMENT_TOPIC, FeedbackType.LIKE_COMMENT_VIDEO
    constructor(@FeedbackType type: Int) : super(type)
    internal constructor(parcel: Parcel) : super(parcel) {
        liked = parcel.readTypedObjectCompat(Comment.CREATOR)
        commented = readModel(parcel)
        owners = readOwners(parcel)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeTypedObjectCompat(liked, flags)
        writeModel(dest, flags, commented)
        writeOwners(dest, flags, owners)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setCommented(commented: AbsModel?): LikeCommentFeedback {
        this.commented = commented
        return this
    }

    fun setLiked(liked: Comment?): LikeCommentFeedback {
        this.liked = liked
        return this
    }

    fun setOwners(owners: List<Owner>?): LikeCommentFeedback {
        this.owners = owners
        return this
    }

    companion object CREATOR : Parcelable.Creator<LikeCommentFeedback> {
        override fun createFromParcel(parcel: Parcel): LikeCommentFeedback {
            return LikeCommentFeedback(parcel)
        }

        override fun newArray(size: Int): Array<LikeCommentFeedback?> {
            return arrayOfNulls(size)
        }
    }
}