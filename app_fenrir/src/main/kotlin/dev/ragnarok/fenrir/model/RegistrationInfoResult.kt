package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class RegistrationInfoResult : Parcelable {
    var registered: String? = null
        private set
    var auth: String? = null
        private set
    var changes: String? = null
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        registered = parcel.readString()
        auth = parcel.readString()
        changes = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(registered)
        parcel.writeString(auth)
        parcel.writeString(changes)
    }

    fun setRegistered(registered: String?): RegistrationInfoResult {
        this.registered = registered
        return this
    }

    fun setAuth(auth: String?): RegistrationInfoResult {
        this.auth = auth
        return this
    }

    fun setChanges(changes: String?): RegistrationInfoResult {
        this.changes = changes
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RegistrationInfoResult> {
        override fun createFromParcel(parcel: Parcel): RegistrationInfoResult {
            return RegistrationInfoResult(parcel)
        }

        override fun newArray(size: Int): Array<RegistrationInfoResult?> {
            return arrayOfNulls(size)
        }
    }
}
