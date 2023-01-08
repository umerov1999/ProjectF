package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean

class FileItem : Parcelable {
    val isDir: Boolean
    val file_name: String
    val file_path: String
    val parent_name: String?
    val parent_path: String?
    val modification: Long
    val size: Long

    constructor(
        isDirectory: Boolean,
        file_name: String,
        file_path: String,
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

    internal constructor(parcel: Parcel) {
        isDir = parcel.getBoolean()
        file_name = parcel.readString()!!
        file_path = parcel.readString()!!
        parent_name = parcel.readString()
        parent_path = parcel.readString()
        modification = parcel.readLong()
        size = parcel.readLong()
    }

    override fun toString(): String {
        return file_path
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

    companion object CREATOR : Parcelable.Creator<FileItem> {
        override fun createFromParcel(parcel: Parcel): FileItem {
            return FileItem(parcel)
        }

        override fun newArray(size: Int): Array<FileItem?> {
            return arrayOfNulls(size)
        }
    }
}