package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.readModel
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.writeModel
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class ReplyCommentFeedback : Feedback {
    var commentsOf: AbsModel? = null
        private set
    var ownComment: Comment? = null
        private set
    var feedbackComment: Comment? = null
        private set

    constructor(@FeedbackType type: Int) : super(type)
    internal constructor(parcel: Parcel) : super(parcel) {
        commentsOf = readModel(parcel)
        ownComment = parcel.readTypedObjectCompat(Comment.CREATOR)
        feedbackComment = parcel.readTypedObjectCompat(Comment.CREATOR)
    }

    override fun getModelType(): Int {
        return FeedbackModelType.MODEL_REPLYCOMMENT_FEEDBACK
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        writeModel(dest, flags, commentsOf)
        dest.writeTypedObjectCompat(ownComment, flags)
        dest.writeTypedObjectCompat(feedbackComment, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setCommentsOf(commentsOf: AbsModel?): ReplyCommentFeedback {
        this.commentsOf = commentsOf
        return this
    }

    fun setOwnComment(ownComment: Comment?): ReplyCommentFeedback {
        this.ownComment = ownComment
        return this
    }

    fun setFeedbackComment(feedbackComment: Comment?): ReplyCommentFeedback {
        this.feedbackComment = feedbackComment
        return this
    }

    companion object CREATOR : Parcelable.Creator<ReplyCommentFeedback> {
        override fun createFromParcel(parcel: Parcel): ReplyCommentFeedback {
            return ReplyCommentFeedback(parcel)
        }

        override fun newArray(size: Int): Array<ReplyCommentFeedback?> {
            return arrayOfNulls(size)
        }
    }
}