package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class VideoSize : Parcelable {
    var width: Int
        private set
    var height: Int
        private set

    constructor(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    internal constructor(`in`: Parcel) {
        width = `in`.readInt()
        height = `in`.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setWidth(width: Int): VideoSize {
        this.width = width
        return this
    }

    fun setHeight(height: Int): VideoSize {
        this.height = height
        return this
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(width)
        parcel.writeInt(height)
    }

    override fun toString(): String {
        return "[$width*$height]"
    }

    companion object CREATOR : Parcelable.Creator<VideoSize> {
        override fun createFromParcel(parcel: Parcel): VideoSize {
            return VideoSize(parcel)
        }

        override fun newArray(size: Int): Array<VideoSize?> {
            return arrayOfNulls(size)
        }
    }
}