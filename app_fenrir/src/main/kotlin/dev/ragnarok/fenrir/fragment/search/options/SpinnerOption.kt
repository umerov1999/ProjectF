package dev.ragnarok.fenrir.fragment.search.options

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class SpinnerOption : BaseOption {
    var value: Entry? = null
    lateinit var available: ArrayList<Entry>

    constructor(key: Int, title: Int, active: Boolean) : super(SPINNER, key, title, active)
    internal constructor(parcel: Parcel) : super(parcel) {
        value = parcel.readTypedObjectCompat(Entry.CREATOR)
        available = parcel.createTypedArrayList(Entry.CREATOR)!!
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): SpinnerOption {
        val clone = super.clone() as SpinnerOption
        clone.value = value?.clone()
        return clone
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeTypedObjectCompat(value, flags)
        dest.writeTypedList(available)
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is SpinnerOption && value == other.value
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }

    override fun describeContents(): Int {
        return 0
    }

    fun createAvailableNames(context: Context): Array<String?> {
        val names = arrayOfNulls<String>(available.size)
        for (i in available.indices) {
            names[i] = context.getString(available[i].name)
        }
        return names
    }

    class Entry : Parcelable, Cloneable {
        val id: Int
        val name: Int

        constructor(id: Int, name: Int) {
            this.id = id
            this.name = name
        }

        internal constructor(parcel: Parcel) {
            id = parcel.readInt()
            name = parcel.readInt()
        }

        @Throws(CloneNotSupportedException::class)
        public override fun clone(): Entry {
            return super.clone() as Entry
        }

        override fun equals(other: Any?): Boolean {
            return other is Entry && id == other.id
        }

        override fun hashCode(): Int {
            return id
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(id)
            dest.writeInt(name)
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

    companion object CREATOR : Parcelable.Creator<SpinnerOption> {
        override fun createFromParcel(parcel: Parcel): SpinnerOption {
            return SpinnerOption(parcel)
        }

        override fun newArray(size: Int): Array<SpinnerOption?> {
            return arrayOfNulls(size)
        }
    }
}