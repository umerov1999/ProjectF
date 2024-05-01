package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.putBoolean

class SimplePrivacy : Parcelable, ParcelNative.ParcelableNative {
    val type: String?
    val entries: List<Entry>?

    constructor(type: String?, entries: List<Entry>?) {
        this.type = type
        this.entries = entries
    }

    internal constructor(parcel: Parcel) {
        type = parcel.readString()
        entries = parcel.createTypedArrayList(Entry.CREATOR)
    }

    internal constructor(parcel: ParcelNative) {
        type = parcel.readString()
        entries = parcel.readParcelableList(Entry.NativeCreator)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(type)
        parcel.writeTypedList(entries)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeString(type)
        dest.writeParcelableList(entries)
    }

    class Entry : Parcelable, ParcelNative.ParcelableNative {
        val type: Int
        val id: Long
        val allowed: Boolean

        constructor(type: Int, id: Long, allowed: Boolean) {
            this.type = type
            this.id = id
            this.allowed = allowed
        }

        internal constructor(parcel: Parcel) {
            type = parcel.readInt()
            id = parcel.readLong()
            allowed = parcel.getBoolean()
        }

        internal constructor(parcel: ParcelNative) {
            type = parcel.readInt()
            id = parcel.readLong()
            allowed = parcel.readBoolean()
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(type)
            dest.writeLong(id)
            dest.putBoolean(allowed)
        }

        override fun writeToParcelNative(dest: ParcelNative) {
            dest.writeInt(type)
            dest.writeLong(id)
            dest.writeBoolean(allowed)
        }

        override fun equals(other: Any?): Boolean {
            return other is Entry && type == other.type && id == other.id && allowed == other.allowed
        }

        override fun hashCode(): Int {
            var result = type
            result = 31 * result + id.hashCode()
            result = 31 * result + if (allowed) 1 else 0
            return result
        }

        companion object {
            const val TYPE_USER = 1
            const val TYPE_FRIENDS_LIST = 2

            @JvmField
            val CREATOR: Parcelable.Creator<Entry> = object : Parcelable.Creator<Entry> {
                override fun createFromParcel(parcel: Parcel): Entry {
                    return Entry(parcel)
                }

                override fun newArray(size: Int): Array<Entry?> {
                    return arrayOfNulls(size)
                }
            }
            val NativeCreator: ParcelNative.Creator<Entry> =
                object : ParcelNative.Creator<Entry> {
                    override fun readFromParcelNative(dest: ParcelNative): Entry {
                        return Entry(dest)
                    }

                }
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SimplePrivacy> =
            object : Parcelable.Creator<SimplePrivacy> {
                override fun createFromParcel(parcel: Parcel): SimplePrivacy {
                    return SimplePrivacy(parcel)
                }

                override fun newArray(size: Int): Array<SimplePrivacy?> {
                    return arrayOfNulls(size)
                }
            }
        val NativeCreator: ParcelNative.Creator<SimplePrivacy> =
            object : ParcelNative.Creator<SimplePrivacy> {
                override fun readFromParcelNative(dest: ParcelNative): SimplePrivacy {
                    return SimplePrivacy(dest)
                }
            }
    }
}