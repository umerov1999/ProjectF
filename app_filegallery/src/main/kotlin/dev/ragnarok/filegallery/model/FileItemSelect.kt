package dev.ragnarok.filegallery.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.module.StringHash.calculateCRC32
import dev.ragnarok.filegallery.getBoolean
import dev.ragnarok.filegallery.putBoolean

class FileItemSelect : Parcelable {
    val isDir: Boolean
    val file_name: String?
    val file_path: String?
    val parent_name: String?
    val parent_path: String?
    val modification: Long
    val size: Long

    constructor(
        isDirectory: Boolean,
        file_name: String?,
        file_path: String?,
        parent_name: String?,
        parent_path: String?,
        modification: Long,
        size: Long
    ) {
        isDir = isDirectory
        this.file_name = file_name
        this.file_path = file_path
        this.parent_name = parent_name
        this.parent_path = parent_path
        this.size = size
        this.modification = modification
    }

    constructor(`in`: Parcel) {
        isDir = `in`.getBoolean()
        file_name = `in`.readString()
        file_path = `in`.readString()
        parent_name = `in`.readString()
        parent_path = `in`.readString()
        modification = `in`.readLong()
        size = `in`.readLong()
    }

    val fileNameHash: Int
        get() = file_name?.let { calculateCRC32(it) } ?: -1
    val filePathHash: Int
        get() = file_path?.let { calculateCRC32(it) } ?: -1

    override fun toString(): String {
        return file_path ?: "null"
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.putBoolean(isDir)
        dest.writeString(file_name)
        dest.writeString(file_path)
        dest.writeString(parent_name)
        dest.writeString(parent_path)
        dest.writeLong(modification)
        dest.writeLong(size)
    }

    companion object CREATOR : Parcelable.Creator<FileItemSelect> {
        override fun createFromParcel(parcel: Parcel): FileItemSelect {
            return FileItemSelect(parcel)
        }

        override fun newArray(size: Int): Array<FileItemSelect?> {
            return arrayOfNulls(size)
        }
    }
}