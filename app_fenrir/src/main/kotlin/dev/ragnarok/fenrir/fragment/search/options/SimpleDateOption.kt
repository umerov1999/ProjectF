package dev.ragnarok.fenrir.fragment.search.options

import android.os.Parcel
import android.os.Parcelable

class SimpleDateOption : BaseOption {
    var timeUnix: Long = 0

    constructor(key: Int, title: Int, active: Boolean) : super(DATE_TIME, key, title, active)
    internal constructor(`in`: Parcel) : super(`in`) {
        timeUnix = `in`.readLong()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeDouble(timeUnix.toDouble())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as SimpleDateOption
        return timeUnix == that.timeUnix
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + timeUnix.hashCode()
        return result
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): SimpleDateOption {
        return super.clone() as SimpleDateOption
    }

    companion object CREATOR : Parcelable.Creator<SimpleDateOption> {
        override fun createFromParcel(parcel: Parcel): SimpleDateOption {
            return SimpleDateOption(parcel)
        }

        override fun newArray(size: Int): Array<SimpleDateOption?> {
            return arrayOfNulls(size)
        }
    }
}