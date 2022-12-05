package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class NotSupported : AbsModel {
    private var type: String? = null
    private var body: String? = null

    constructor()
    internal constructor(`in`: Parcel) {
        type = `in`.readString()
        body = `in`.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        parcel.writeString(body)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_NOT_SUPPORTED
    }

    fun getType(): String? {
        return type
    }

    fun setType(type: String?): NotSupported {
        this.type = type
        return this
    }

    fun getBody(): String? {
        return body
    }

    fun setBody(body: String?): NotSupported {
        this.body = body
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NotSupported> {
        override fun createFromParcel(parcel: Parcel): NotSupported {
            return NotSupported(parcel)
        }

        override fun newArray(size: Int): Array<NotSupported?> {
            return arrayOfNulls(size)
        }
    }
}