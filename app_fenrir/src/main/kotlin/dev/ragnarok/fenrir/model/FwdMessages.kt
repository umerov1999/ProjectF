package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class FwdMessages : AbsModel {
    val fwds: ArrayList<Message>

    constructor(fwds: ArrayList<Message>) {
        this.fwds = fwds
    }

    internal constructor(`in`: Parcel) {
        fwds = `in`.createTypedArrayList(Message.CREATOR)!!
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_FWDMESSAGES
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(fwds)
    }

    companion object CREATOR : Parcelable.Creator<FwdMessages> {
        override fun createFromParcel(parcel: Parcel): FwdMessages {
            return FwdMessages(parcel)
        }

        override fun newArray(size: Int): Array<FwdMessages?> {
            return arrayOfNulls(size)
        }
    }
}