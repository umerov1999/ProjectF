package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.putBoolean

class SimplePrivacy : Parcelable, ParcelNative.ParcelableNative {
    private val type: String?
    private val entries: List<Entry>?

    constructor(type: String?, entries: List<Entry>?) {
        this.type = type
        this.entries = entries
    }

    internal constructor(`in`: Parcel) {
        type = `in`.readString()
        entries = `in`.createTypedArrayList(Entry.CREATOR)
    }

    internal constructor(`in`: ParcelNative) {
        type = `in`.readString()
        entries = `in`.readParcelableList(Entry.NativeCreator)
    }

    fun getType(): String? {
        return type
    }

    fun getEntries(): List<Entry>? {
        return entries
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
        private val type: Int
        private val id: Int
        private val allowed: Boolean

        constructor(type: Int, id: Int, allowed: Boolean) {
            this.type = type
            this.id = id
            this.allowed = allowed
        }

        internal constructor(`in`: Parcel) {
            type = `in`.readInt()
            id = `in`.readInt()
            allowed = `in`.getBoolean()
        }

        internal constructor(`in`: ParcelNative) {
            type = `in`.readInt()
            id = `in`.readInt()
            allowed = `in`.readBoolean()
        }

        fun getType(): Int {
            return type
        }

        fun getId(): Int {
            return id
        }

        fun isAllowed(): Boolean {
            return allowed
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(type)
            dest.writeInt(id)
            dest.putBoolean(allowed)
        }

        override fun writeToParcelNative(dest: ParcelNative) {
            dest.writeInt(type)
            dest.writeInt(id)
            dest.writeBoolean(allowed)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val entry = other as Entry
            return type == entry.getType() && id == entry.getId() && allowed == entry.isAllowed()
        }

        override fun hashCode(): Int {
            var result = type
            result = 31 * result + id
            result = 31 * result + if (allowed) 1 else 0
            return result
        }

        companion object {
            const val TYPE_USER = 1
            const val TYPE_FRIENDS_LIST = 2

            @JvmField
            val CREATOR: Parcelable.Creator<Entry> = object : Parcelable.Creator<Entry> {
                override fun createFromParcel(`in`: Parcel): Entry {
                    return Entry(`in`)
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
                override fun createFromParcel(`in`: Parcel): SimplePrivacy {
                    return SimplePrivacy(`in`)
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