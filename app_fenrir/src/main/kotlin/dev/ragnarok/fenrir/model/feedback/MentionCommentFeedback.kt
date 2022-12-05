package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.readModel
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.writeModel
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class MentionCommentFeedback : Feedback {
    var where: Comment? = null
        private set
    var commentOf: AbsModel? = null
        private set

    // one of FeedbackType.MENTION_COMMENT_POST, FeedbackType.MENTION_COMMENT_PHOTO, FeedbackType.MENTION_COMMENT_VIDEO
    constructor(@FeedbackType type: Int) : super(type)
    internal constructor(`in`: Parcel) : super(`in`) {
        where = `in`.readTypedObjectCompat(Comment.CREATOR)
        commentOf = readModel(`in`)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeTypedObjectCompat(where, flags)
        writeModel(dest, flags, commentOf)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setCommentOf(commentOf: AbsModel?): MentionCommentFeedback {
        this.commentOf = commentOf
        return this
    }

    fun setWhere(where: Comment?): MentionCommentFeedback {
        this.where = where
        return this
    }

    companion object CREATOR : Parcelable.Creator<MentionCommentFeedback> {
        override fun createFromParcel(parcel: Parcel): MentionCommentFeedback {
            return MentionCommentFeedback(parcel)
        }

        override fun newArray(size: Int): Array<MentionCommentFeedback?> {
            return arrayOfNulls(size)
        }
    }
}