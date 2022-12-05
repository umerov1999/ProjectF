package dev.ragnarok.filegallery.model.tags

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.module.StringHash.calculateCRC32
import dev.ragnarok.filegallery.getBoolean
import dev.ragnarok.filegallery.model.FileType
import dev.ragnarok.filegallery.putBoolean

class TagDir : Parcelable {
    var id = 0
        private set
    var owner_id = 0
        private set
    var size: Long = 0
        private set
    var name: String? = null
        private set
    var path: String? = null
        private set

    @get:FileType
    @FileType
    var type = FileType.folder
        private set
    var isSelected = false

    constructor()
    constructor(`in`: Parcel) {
        id = `in`.readInt()
        owner_id = `in`.readInt()
        name = `in`.readString()
        path = `in`.readString()
        type = `in`.readInt()
        size = `in`.readLong()
        isSelected = `in`.getBoolean()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(owner_id)
        parcel.writeString(name)
        parcel.writeString(path)
        parcel.writeInt(type)
        parcel.writeLong(size)
        parcel.putBoolean(isSelected)
    }

    fun setSelected(selected: Boolean): TagDir {
        isSelected = selected
        return this
    }

    val fileNameHash: Int
        get() = calculateCRC32(name!!)
    val filePathHash: Int
        get() = calculateCRC32(path!!)

    fun setId(id: Int): TagDir {
        this.id = id
        return this
    }

    fun setType(@FileType type: Int): TagDir {
        this.type = type
        return this
    }

    fun setOwner_id(owner_id: Int): TagDir {
        this.owner_id = owner_id
        return this
    }

    fun setName(name: String?): TagDir {
        this.name = name
        return this
    }

    fun setPath(path: String?): TagDir {
        this.path = path
        return this
    }

    fun setSize(size: Long): TagDir {
        this.size = size
        return this
    }

    companion object CREATOR : Parcelable.Creator<TagDir> {
        override fun createFromParcel(parcel: Parcel): TagDir {
            return TagDir(parcel)
        }

        override fun newArray(size: Int): Array<TagDir?> {
            return arrayOfNulls(size)
        }
    }
}