package dev.ragnarok.fenrir.model.wrappers

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.model.ISelectable
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.writeTypedObjectCompat

class SelectablePhotoWrapper : Parcelable, Comparable<SelectablePhotoWrapper>, ISelectable {
    val photo: Photo
    override var isSelected = false
    var isDownloaded = false
        private set
    var index = 0
    var current = false

    constructor(photo: Photo) {
        this.photo = photo
    }

    internal constructor(`in`: Parcel) {
        photo = `in`.readTypedObjectCompat(Photo.CREATOR)!!
        isSelected = `in`.getBoolean()
        index = `in`.readInt()
        current = `in`.getBoolean()
        isDownloaded = `in`.getBoolean()
    }

    fun setDownloaded(downloaded: Boolean): SelectablePhotoWrapper {
        isDownloaded = downloaded
        return this
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedObjectCompat(photo, flags)
        dest.putBoolean(isSelected)
        dest.writeInt(index)
        dest.putBoolean(current)
        dest.putBoolean(isDownloaded)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as SelectablePhotoWrapper
        return photo == that.photo
    }

    override fun hashCode(): Int {
        return photo.hashCode()
    }

    override fun compareTo(other: SelectablePhotoWrapper): Int {
        return index - other.index
    }

    companion object CREATOR : Parcelable.Creator<SelectablePhotoWrapper> {
        override fun createFromParcel(parcel: Parcel): SelectablePhotoWrapper {
            return SelectablePhotoWrapper(parcel)
        }

        override fun newArray(size: Int): Array<SelectablePhotoWrapper?> {
            return arrayOfNulls(size)
        }
    }
}