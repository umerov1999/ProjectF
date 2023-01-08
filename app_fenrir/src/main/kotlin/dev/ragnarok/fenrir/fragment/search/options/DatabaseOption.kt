package dev.ragnarok.fenrir.fragment.search.options

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class DatabaseOption : BaseOption {
    /**
     * Тип данных, который находится в обьекте value
     * страна, город, университет и т.д.
     */
    val type: Int

    /**
     * Текущее значение опции
     */
    var value: Entry? = null

    constructor(key: Int, title: Int, active: Boolean, type: Int) : super(
        DATABASE,
        key,
        title,
        active
    ) {
        this.type = type
    }

    internal constructor(parcel: Parcel) : super(parcel) {
        type = parcel.readInt()
        value = parcel.readTypedObjectCompat(Entry.CREATOR)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(type)
        dest.writeTypedObjectCompat(value, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as DatabaseOption
        return (type == that.type
                && value == that.value)
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): DatabaseOption {
        val clone = super.clone() as DatabaseOption
        clone.value = value?.clone()
        return clone
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + type
        result = 31 * result + if (value != null) value.hashCode() else 0
        return result
    }

    override fun reset() {
        value = null
    }

    class Entry : Parcelable, Cloneable {
        val id: Int
        val title: String?

        constructor(id: Int, title: String?) {
            this.id = id
            this.title = title
        }

        internal constructor(parcel: Parcel) {
            id = parcel.readInt()
            title = parcel.readString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val entry = other as Entry
            return id == entry.id
        }

        override fun hashCode(): Int {
            return id
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(id)
            dest.writeString(title)
        }

        @Throws(CloneNotSupportedException::class)
        public override fun clone(): Entry {
            return super.clone() as Entry
        }

        companion object CREATOR : Parcelable.Creator<Entry> {
            override fun createFromParcel(parcel: Parcel): Entry {
                return Entry(parcel)
            }

            override fun newArray(size: Int): Array<Entry?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        const val TYPE_COUNTRY = 1
        const val TYPE_CITY = 2
        const val TYPE_UNIVERSITY = 3
        const val TYPE_FACULTY = 4
        const val TYPE_CHAIR = 5
        const val TYPE_SCHOOL = 6
        const val TYPE_SCHOOL_CLASS = 7

        @JvmField
        val CREATOR: Parcelable.Creator<DatabaseOption> =
            object : Parcelable.Creator<DatabaseOption> {
                override fun createFromParcel(parcel: Parcel): DatabaseOption {
                    return DatabaseOption(parcel)
                }

                override fun newArray(size: Int): Array<DatabaseOption?> {
                    return arrayOfNulls(size)
                }
            }
    }
}