package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
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
        private val TYPES: MutableList<Class<*>> = ArrayList(9)
        private val LOADERS: MutableList<(parcel: Parcel) -> Feedback?> = ArrayList(25)

        init {
            //Types
            TYPES.add(CommentFeedback::class.java)
            TYPES.add(CopyFeedback::class.java)
            TYPES.add(LikeCommentFeedback::class.java)
            TYPES.add(LikeFeedback::class.java)
            TYPES.add(MentionCommentFeedback::class.java)
            TYPES.add(MentionFeedback::class.java)
            TYPES.add(PostPublishFeedback::class.java)
            TYPES.add(ReplyCommentFeedback::class.java)
            TYPES.add(UsersFeedback::class.java)

            //Loaders
            LOADERS.add { it.readTypedObjectCompat(CommentFeedback.CREATOR) }
            LOADERS.add { it.readTypedObjectCompat(CopyFeedback.CREATOR) }
            LOADERS.add { it.readTypedObjectCompat(LikeCommentFeedback.CREATOR) }
            LOADERS.add { it.readTypedObjectCompat(LikeFeedback.CREATOR) }
            LOADERS.add { it.readTypedObjectCompat(MentionCommentFeedback.CREATOR) }
            LOADERS.add { it.readTypedObjectCompat(MentionFeedback.CREATOR) }
            LOADERS.add { it.readTypedObjectCompat(PostPublishFeedback.CREATOR) }
            LOADERS.add { it.readTypedObjectCompat(ReplyCommentFeedback.CREATOR) }
            LOADERS.add { it.readTypedObjectCompat(UsersFeedback.CREATOR) }
        }
    }

    private val feedback: Feedback?

    constructor(feedback: Feedback?) {
        this.feedback = feedback
    }

    internal constructor(parcel: Parcel) {
        val index = parcel.readInt()
        feedback = LOADERS[index].invoke(parcel)!!
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        val index = TYPES.indexOf((feedback ?: return).javaClass)
        if (index == -1) {
            throw UnsupportedOperationException("Unsupported class: " + feedback.javaClass)
        }
        dest.writeInt(index)
        dest.writeTypedObjectCompat(feedback, flags)
    }

    fun get(): Feedback? {
        return feedback
    }
}