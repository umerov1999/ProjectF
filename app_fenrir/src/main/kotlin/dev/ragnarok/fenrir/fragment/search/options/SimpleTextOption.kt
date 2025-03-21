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
        return super.equals(other) && other is SimpleTextOption && value == other.value
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
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