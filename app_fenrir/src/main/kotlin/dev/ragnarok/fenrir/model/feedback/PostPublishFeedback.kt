package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class PostPublishFeedback : Feedback {
    var post: Post? = null
        private set

    constructor(@FeedbackType type: Int) : super(type)
    internal constructor(`in`: Parcel) : super(`in`) {
        post = `in`.readTypedObjectCompat(Post.CREATOR)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeTypedObjectCompat(post, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setPost(post: Post?): PostPublishFeedback {
        this.post = post
        return this
    }

    companion object CREATOR : Parcelable.Creator<PostPublishFeedback> {
        override fun createFromParcel(parcel: Parcel): PostPublishFeedback {
            return PostPublishFeedback(parcel)
        }

        override fun newArray(size: Int): Array<PostPublishFeedback?> {
            return arrayOfNulls(size)
        }
    }
}