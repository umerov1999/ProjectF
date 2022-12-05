package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.writeTypedObjectCompat

class Event : AbsModel {
    val id: Int
    var button_text: String? = null
        private set
    var text: String? = null
        private set
    var subject: Owner? = null
        private set

    constructor(id: Int) {
        this.id = id
    }

    internal constructor(`in`: Parcel) {
        Owner
        id = `in`.readInt()
        button_text = `in`.readString()
        text = `in`.readString()
        subject =
            Owner.readOwnerFromParcel(id, `in`)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_EVENT
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(button_text)
        parcel.writeString(text)
        parcel.writeTypedObjectCompat(subject, flags)
    }

    fun setText(text: String?): Event {
        this.text = text
        return this
    }

    fun setButton_text(button_text: String?): Event {
        this.button_text = button_text
        return this
    }

    fun setSubject(subject: Owner?): Event {
        this.subject = subject
        return this
    }

    val subjectPhoto: String?
        get() = subject?.maxSquareAvatar
    val subjectName: String?
        get() = subject?.fullName

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Event> {
        override fun createFromParcel(parcel: Parcel): Event {
            return Event(parcel)
        }

        override fun newArray(size: Int): Array<Event?> {
            return arrayOfNulls(size)
        }
    }
}