package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class Token : Parcelable {
    val ownerId: Long
    val accessToken: String?

    constructor(ownerId: Long, accessToken: String?) {
        this.ownerId = ownerId
        this.accessToken = accessToken
    }

    internal constructor(parcel: Parcel) {
        ownerId = parcel.readLong()
        accessToken = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(ownerId)
        dest.writeString(accessToken)
    }

    override fun toString(): String {
        return "Token{" +
                "ownerId=" + ownerId +
                ", accessToken='" + accessToken + '\'' +
                '}'
    }

    companion object CREATOR : Parcelable.Creator<Token> {
        override fun createFromParcel(parcel: Parcel): Token {
            return Token(parcel)
        }

        override fun newArray(size: Int): Array<Token?> {
            return arrayOfNulls(size)
        }
    }
}