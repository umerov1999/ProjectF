package dev.ragnarok.fenrir.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.R

class Call : AbsModel {
    var initiator_id = 0L
        private set
    var receiver_id = 0L
        private set
    var state: String? = null
        private set
    var time: Long = 0
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        initiator_id = parcel.readLong()
        receiver_id = parcel.readLong()
        time = parcel.readLong()
        state = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(initiator_id)
        parcel.writeLong(receiver_id)
        parcel.writeLong(time)
        parcel.writeString(state)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_CALL
    }

    fun setInitiator_id(initiator_id: Long): Call {
        this.initiator_id = initiator_id
        return this
    }

    fun setReceiver_id(receiver_id: Long): Call {
        this.receiver_id = receiver_id
        return this
    }

    fun setTime(time: Long): Call {
        this.time = time
        return this
    }

    fun setState(state: String?): Call {
        this.state = state
        return this
    }

    fun getLocalizedState(context: Context): String? {
        if (state == null) {
            return null
        }
        when (state) {
            "canceled_by_receiver" -> return context.getString(R.string.canceled_by_receiver)
            "canceled_by_initiator" -> return context.getString(R.string.canceled_by_initiator)
            "reached" -> return context.getString(R.string.call_reached)
        }
        return state
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Call> {
        override fun createFromParcel(parcel: Parcel): Call {
            return Call(parcel)
        }

        override fun newArray(size: Int): Array<Call?> {
            return arrayOfNulls(size)
        }
    }
}