package dev.ragnarok.fenrir.model.feedback

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.readModel
import dev.ragnarok.fenrir.model.ParcelableModelWrapper.Companion.writeModel

/**
 * Base class for types [mention, mention_comment_photo, mention_comment_video]
 * where - в каком обьекте было упоминание
 */
class MentionFeedback : Feedback {
    var where: AbsModel? = null
        private set

    // one of FeedbackType.MENTION
    constructor(@FeedbackType type: Int) : super(type)
    internal constructor(parcel: Parcel) : super(parcel) {
        where = readModel(parcel)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        writeModel(dest, flags, where)
    }

    override fun getModelType(): Int {
        return FeedbackModelType.MODEL_MENTION_FEEDBACK
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setWhere(where: AbsModel?): MentionFeedback {
        this.where = where
        return this
    }

    companion object CREATOR : Parcelable.Creator<MentionFeedback> {
        override fun createFromParcel(parcel: Parcel): MentionFeedback {
            return MentionFeedback(parcel)
        }

        override fun newArray(size: Int): Array<MentionFeedback?> {
            return arrayOfNulls(size)
        }
    }
}