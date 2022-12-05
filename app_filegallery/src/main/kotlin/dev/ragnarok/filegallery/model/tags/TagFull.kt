package dev.ragnarok.filegallery.model.tags

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import dev.ragnarok.filegallery.model.FileType
import kotlinx.serialization.Serializable

@Serializable
@Keep
class TagFull : Parcelable {
    var name: String? = null
        private set
    var dirs: ArrayList<TagDirEntry>? = null


    constructor()
    constructor(`in`: Parcel) {
        name = `in`.readString()
        dirs = `in`.createTypedArrayList(TagDirEntry.CREATOR)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeTypedList(dirs)
    }

    fun setDirs(dirs: ArrayList<TagDirEntry>?): TagFull {
        this.dirs = dirs
        return this
    }

    fun reverseList() {
        dirs?.reverse()
    }

    fun setName(name: String?): TagFull {
        this.name = name
        return this
    }

    @Serializable
    class TagDirEntry : Parcelable {
        var name: String? = null
            private set
        var path: String? = null
            private set

        @FileType
        var type = FileType.folder
            private set

        constructor()
        constructor(`in`: Parcel) {
            name = `in`.readString()
            path = `in`.readString()
            type = `in`.readInt()
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeString(path)
            parcel.writeInt(type)
        }

        fun setType(@FileType type: Int): TagDirEntry {
            this.type = type
            return this
        }

        fun setName(name: String?): TagDirEntry {
            this.name = name
            return this
        }

        fun setPath(path: String?): TagDirEntry {
            this.path = path
            return this
        }

        companion object CREATOR : Parcelable.Creator<TagDirEntry> {
            override fun createFromParcel(parcel: Parcel): TagDirEntry {
                return TagDirEntry(parcel)
            }

            override fun newArray(size: Int): Array<TagDirEntry?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TagFull> = object : Parcelable.Creator<TagFull> {
            override fun createFromParcel(`in`: Parcel): TagFull {
                return TagFull(`in`)
            }

            override fun newArray(size: Int): Array<TagFull?> {
                return arrayOfNulls(size)
            }
        }
    }
}