package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import android.util.ArrayMap
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class ParcelableFeedbackWrapper : Parcelable {
    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ParcelableFeedbackWrapper> =
            object : Parcelable.Creator<ParcelableFeedbackWrapper> {
                override fun createFromParcel(parcel: Parcel): ParcelableFeedbackWrapper {
                    return ParcelableFeedbackWrapper(parcel)
                }

                override fun newArray(size: Int): Array<ParcelableFeedbackWrapper?> {
                    return arrayOfNulls(size)
                }
            }
        private val TYPES: ArrayMap<Int, (parcel: Parcel) -> Feedback?> = ArrayMap(9)

        init {
            TYPES[FeedbackModelType.MODEL_COMMENT_FEEDBACK] =
                { it.readTypedObjectCompat(CommentFeedback.CREATOR) }
            TYPES[FeedbackModelType.MODEL_COPY_FEEDBACK] =
                { it.readTypedObjectCompat(CopyFeedback.CREATOR) }
            TYPES[FeedbackModelType.MODEL_LIKECOMMENT_FEEDBACK] =
                { it.readTypedObjectCompat(LikeCommentFeedback.CREATOR) }
            TYPES[FeedbackModelType.MODEL_LIKE_FEEDBACK] =
                { it.readTypedObjectCompat(LikeFeedback.CREATOR) }
            TYPES[FeedbackModelType.MODEL_MENTIONCOMMENT_FEEDBACK] =
                { it.readTypedObjectCompat(MentionCommentFeedback.CREATOR) }
            TYPES[FeedbackModelType.MODEL_MENTION_FEEDBACK] =
                { it.readTypedObjectCompat(MentionFeedback.CREATOR) }
            TYPES[FeedbackModelType.MODEL_POSTPUBLISH_FEEDBACK] =
                { it.readTypedObjectCompat(PostPublishFeedback.CREATOR) }
            TYPES[FeedbackModelType.MODEL_REPLYCOMMENT_FEEDBACK] =
                { it.readTypedObjectCompat(ReplyCommentFeedback.CREATOR) }
            TYPES[FeedbackModelType.MODEL_USERS_FEEDBACK] =
                { it.readTypedObjectCompat(UsersFeedback.CREATOR) }
        }
    }

    private val feedback: Feedback?

    constructor(feedback: Feedback?) {
        this.feedback = feedback
    }

    internal constructor(parcel: Parcel) {
        val index = parcel.readInt()
        feedback = if (index == FeedbackModelType.MODEL_NULL_FEEDBACK) {
            null
        } else {
            TYPES[index]!!.invoke(parcel)!!
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        if (feedback == null || !TYPES.contains(feedback.getModelType())) {
            dest.writeInt(FeedbackModelType.MODEL_NULL_FEEDBACK)
            return
        }
        dest.writeInt(feedback.getModelType())
        dest.writeTypedObjectCompat(feedback, flags)
    }

    fun get(): Feedback? {
        return feedback
    }
}