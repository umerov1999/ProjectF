package dev.ragnarok.fenrir.fragment.search.options

import android.os.Parcel
import android.os.Parcelable

class SimpleTextOption : BaseOption {
    var value: String? = null

    constructor(key: Int, title: Int, active: Boolean) : super(SIMPLE_TEXT, key, title, active)
    internal constructor(parcel: Parcel) : super(parcel) {
        value = parcel.readString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeString(value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as SimpleTextOption
        return value == that.value
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + if (value != null) value.hashCode() else 0
        return result
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): SimpleTextOption {
        val clone = super.clone() as SimpleTextOption
        clone.value = value
        return clone
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SimpleTextOption> {
        override fun createFromParcel(parcel: Parcel): SimpleTextOption {
            return SimpleTextOption(parcel)
        }

        override fun newArray(size: Int): Array<SimpleTextOption?> {
            return arrayOfNulls(size)
        }
    }
}