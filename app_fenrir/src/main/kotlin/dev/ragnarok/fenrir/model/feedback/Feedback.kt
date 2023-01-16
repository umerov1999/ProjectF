package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.CallSuper
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

abstract class Feedback : Parcelable {
    @FeedbackType
    val type: Int
    var date: Long = 0
        private set
    var reply: Comment? = null
        private set

    @FeedbackModelType
    abstract fun getModelType(): Int

    constructor(@FeedbackType type: Int) {
        this.type = type
    }

    protected constructor(parcel: Parcel) {
        type = parcel.readInt()
        date = parcel.readLong()
        reply = parcel.readTypedObjectCompat(Comment.CREATOR)
    }

    fun setDate(date: Long): Feedback {
        this.date = date
        return this
    }

    fun setReply(reply: Comment?): Feedback {
        this.reply = reply
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    @CallSuper
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(type)
        dest.writeLong(date)
        dest.writeTypedObjectCompat(reply, flags)
    }
}