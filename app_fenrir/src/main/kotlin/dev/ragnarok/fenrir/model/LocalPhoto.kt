package dev.ragnarok.fenrir.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.core.net.toUri
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean

class LocalPhoto : Parcelable, Comparable<LocalPhoto>, ISelectable {
    var imageId: Long = 0
        private set
    var fullImageUri: Uri? = null
        private set
    private var selected = false
    var index = 0
        private set

    constructor()
    internal constructor(parcel: Parcel) {
        imageId = parcel.readLong()
        fullImageUri = parcel.readString()?.toUri()
        selected = parcel.getBoolean()
        index = parcel.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(imageId)
        if (fullImageUri == null) {
            dest.writeString("null")
        } else {
            dest.writeString(fullImageUri.toString())
        }
        dest.putBoolean(selected)
        dest.writeInt(index)
    }

    fun setImageId(imageId: Long): LocalPhoto {
        this.imageId = imageId
        return this
    }

    fun setIndex(index: Int): LocalPhoto {
        this.index = index
        return this
    }

    fun setFullImageUri(fullImageUri: Uri?): LocalPhoto {
        this.fullImageUri = fullImageUri
        return this
    }

    override fun compareTo(other: LocalPhoto): Int {
        return index - other.index
    }

    override val isSelected: Boolean
        get() = selected

    fun setSelected(selected: Boolean): LocalPhoto {
        this.selected = selected
        return this
    }

    companion object CREATOR : Parcelable.Creator<LocalPhoto> {
        override fun createFromParcel(parcel: Parcel): LocalPhoto {
            return LocalPhoto(parcel)
        }

        override fun newArray(size: Int): Array<LocalPhoto?> {
            return arrayOfNulls(size)
        }
    }
}