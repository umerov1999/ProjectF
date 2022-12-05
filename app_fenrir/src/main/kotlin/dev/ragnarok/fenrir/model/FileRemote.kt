package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.putBoolean
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class FileRemote : Parcelable, ParcelNative.ParcelableNative {
    @SerialName("id")
    var id = 0
        private set

    @SerialName("owner_Id")
    var owner_Id = 0
        private set

    @SerialName("file_name")
    var file_name: String? = null
        private set

    @SerialName("type")
    @FileType
    var type: Int = FileType.error
        private set

    @SerialName("modification_time")
    var modification_time: Long = 0
        private set

    @SerialName("size")
    var size: Long = 0
        private set

    @SerialName("url")
    var url: String? = null
        private set

    @SerialName("preview_url")
    var preview_url: String? = null
        private set

    @SerialName("is_selected")
    var isSelected: Boolean = false
        private set

    @Suppress("UNUSED")
    constructor()
    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        owner_Id = `in`.readInt()
        file_name = `in`.readString()
        type = `in`.readInt()
        modification_time = `in`.readLong()
        size = `in`.readLong()
        url = `in`.readString()
        preview_url = `in`.readString()
        isSelected = `in`.getBoolean()
    }

    internal constructor(`in`: ParcelNative) {
        id = `in`.readInt()
        owner_Id = `in`.readInt()
        file_name = `in`.readString()
        type = `in`.readInt()
        modification_time = `in`.readLong()
        size = `in`.readLong()
        url = `in`.readString()
        preview_url = `in`.readString()
        isSelected = `in`.readBoolean()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeInt(owner_Id)
        dest.writeString(file_name)
        dest.writeInt(type)
        dest.writeLong(modification_time)
        dest.writeLong(size)
        dest.writeString(url)
        dest.writeString(preview_url)
        dest.putBoolean(isSelected)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(id)
        dest.writeInt(owner_Id)
        dest.writeString(file_name)
        dest.writeInt(type)
        dest.writeLong(modification_time)
        dest.writeLong(size)
        dest.writeString(url)
        dest.writeString(preview_url)
        dest.writeBoolean(isSelected)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setId(id: Int): FileRemote {
        this.id = id
        return this
    }

    fun setOwnerId(ownerId: Int): FileRemote {
        this.owner_Id = ownerId
        return this
    }

    fun setFileName(title: String?): FileRemote {
        this.file_name = title
        return this
    }

    fun setType(@FileType type: Int): FileRemote {
        this.type = type
        return this
    }

    fun setModTime(modification_time: Long): FileRemote {
        this.modification_time = modification_time
        return this
    }

    fun setSize(size: Long): FileRemote {
        this.size = size
        return this
    }

    fun setUrl(url: String): FileRemote {
        this.url = url
        return this
    }

    fun setPreview(preview_url: String): FileRemote {
        this.preview_url = preview_url
        return this
    }

    fun setSelected(sel: Boolean): FileRemote {
        this.isSelected = sel
        return this
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<FileRemote> = object : Parcelable.Creator<FileRemote> {
            override fun createFromParcel(`in`: Parcel): FileRemote {
                return FileRemote(`in`)
            }

            override fun newArray(size: Int): Array<FileRemote?> {
                return arrayOfNulls(size)
            }
        }
        val NativeCreator: ParcelNative.Creator<FileRemote> =
            object : ParcelNative.Creator<FileRemote> {
                override fun readFromParcelNative(dest: ParcelNative): FileRemote {
                    return FileRemote(dest)
                }
            }
    }
}
