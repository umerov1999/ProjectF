package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class Event : AbsModel {
    val id: Long
    var button_text: String? = null
        private set
    var text: String? = null
        private set
    var subject: Owner? = null
        private set

    constructor(id: Long) {
        this.id = id
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readLong()
        button_text = parcel.readString()
        text = parcel.readString()
        subject = ParcelableOwnerWrapper.readOwner(parcel)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_EVENT
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(button_text)
        parcel.writeString(text)
        ParcelableOwnerWrapper.writeOwner(parcel, flags, subject)
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