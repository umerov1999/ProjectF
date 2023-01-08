package dev.ragnarok.fenrir.fragment.search.options

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.util.ParcelUtils.readObjectInteger
import dev.ragnarok.fenrir.util.ParcelUtils.writeObjectInteger

class SimpleNumberOption : BaseOption {
    var value: Int? = null

    constructor(key: Int, title: Int, active: Boolean) : super(SIMPLE_NUMBER, key, title, active)
    constructor(key: Int, title: Int, active: Boolean, value: Int) : super(
        SIMPLE_NUMBER,
        key,
        title,
        active
    ) {
        this.value = value
    }

    internal constructor(parcel: Parcel) : super(parcel) {
        value = readObjectInteger(parcel)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        writeObjectInteger(dest, value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as SimpleNumberOption
        return value == that.value
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + if (value != null) value.hashCode() else 0
        return result
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): SimpleNumberOption {
        val clone = super.clone() as SimpleNumberOption
        clone.value = value
        return clone
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SimpleNumberOption> {
        override fun createFromParcel(parcel: Parcel): SimpleNumberOption {
            return SimpleNumberOption(parcel)
        }

        override fun newArray(size: Int): Array<SimpleNumberOption?> {
            return arrayOfNulls(size)
        }
    }
}