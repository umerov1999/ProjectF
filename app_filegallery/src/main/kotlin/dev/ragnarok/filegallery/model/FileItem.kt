package dev.ragnarok.filegallery.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.module.StringHash.calculateCRC32
import dev.ragnarok.filegallery.getBoolean
import dev.ragnarok.filegallery.media.music.MusicPlaybackController
import dev.ragnarok.filegallery.putBoolean

class FileItem : Parcelable {
    @FileType
    val type: Int
    val file_name: String?
    val file_path: String?
    val parent_name: String?
    val parent_path: String?
    val modification: Long
    val size: Long
    val isCanRead: Boolean
    var isSelected = false
    var isHasTag = false
        private set

    constructor(
        @FileType type: Int,
        file_name: String?,
        file_path: String?,
        parent_name: String?,
        parent_path: String?,
        modification: Long,
        size: Long,
        canRead: Boolean
    ) {
        this.type = type
        this.file_name = file_name
        this.file_path = file_path
        this.parent_name = parent_name
        this.parent_path = parent_path
        this.size = size
        isCanRead = canRead
        this.modification = modification
    }

    constructor(parcel: Parcel) {
        type = parcel.readInt()
        file_name = parcel.readString()
        file_path = parcel.readString()
        parent_name = parcel.readString()
        parent_path = parcel.readString()
        modification = parcel.readLong()
        size = parcel.readLong()
        isCanRead = parcel.getBoolean()
        isSelected = parcel.getBoolean()
        isHasTag = parcel.getBoolean()
    }

    fun checkTag(): FileItem {
        isHasTag = file_path?.let { MusicPlaybackController.tracksExist.isExistTag(it) } ?: false
        return this
    }

    val fileNameHash: Int
        get() = file_name?.let { calculateCRC32(it) } ?: -1
    val filePathHash: Long
        get() = file_path?.let { calculateCRC32(it).toLong() } ?: -1

    fun setSelected(selected: Boolean): FileItem {
        isSelected = selected
        return this
    }

    override fun toString(): String {
        return file_path ?: "null"
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(type)
        dest.writeString(file_name)
        dest.writeString(file_path)
        dest.writeString(parent_name)
        dest.writeString(parent_path)
        dest.writeLong(modification)
        dest.writeLong(size)
        dest.putBoolean(isCanRead)
        dest.putBoolean(isSelected)
        dest.putBoolean(isHasTag)
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