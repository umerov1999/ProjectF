package dev.ragnarok.fenrir.db

import android.os.Parcel
import android.os.Parcelable

class DatabaseIdRange : Parcelable {
    val first: Int
    val last: Int

    private constructor(first: Int, last: Int) {
        this.first = first
        this.last = last
    }

    internal constructor(`in`: Parcel) {
        first = `in`.readInt()
        last = `in`.readInt()
    }

    override fun toString(): String {
        return "DatabaseIdRange{" +
                "first=" + first +
                ", last=" + last +
                '}'
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(first)
        dest.writeInt(last)
    }

    companion object CREATOR : Parcelable.Creator<DatabaseIdRange> {
        override fun createFromParcel(parcel: Parcel): DatabaseIdRange {
            return DatabaseIdRange(parcel)
        }

        override fun newArray(size: Int): Array<DatabaseIdRange?> {
            return arrayOfNulls(size)
        }
    }
}