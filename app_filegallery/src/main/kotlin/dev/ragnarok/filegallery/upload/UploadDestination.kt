package dev.ragnarok.filegallery.upload

import android.os.Parcel
import android.os.Parcelable

open class UploadDestination : Parcelable {
    val id: Int
    val ownerId: Int

    @Method
    val method: Int

    constructor(id: Int, ownerId: Int, method: Int) {
        this.id = id
        this.ownerId = ownerId
        this.method = method
    }

    internal constructor(`in`: Parcel) {
        id = `in`.readInt()
        ownerId = `in`.readInt()
        method = `in`.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeInt(ownerId)
        dest.writeInt(method)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as UploadDestination
        return id == that.id && ownerId == that.ownerId && method == that.method
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + ownerId
        result = 31 * result + method
        return result
    }

    fun compareTo(destination: UploadDestination?): Boolean {
        destination ?: return false
        return compareTo(destination.id, destination.ownerId, destination.method)
    }

    fun compareTo(id: Int, ownerId: Int, type: Int): Boolean {
        return this.id == id && this.ownerId == ownerId && method == type
    }

    override fun toString(): String {
        return "UploadDestination{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", method=" + method +
                '}'
    }

    companion object {
        private const val NO_ID = 0

        @JvmField
        val CREATOR: Parcelable.Creator<UploadDestination> =
            object : Parcelable.Creator<UploadDestination> {
                override fun createFromParcel(`in`: Parcel): UploadDestination {
                    return UploadDestination(`in`)
                }

                override fun newArray(size: Int): Array<UploadDestination?> {
                    return arrayOfNulls(size)
                }
            }


        fun forRemotePlay(): UploadDestination {
            return UploadDestination(
                NO_ID,
                NO_ID,
                Method.REMOTE_PLAY_AUDIO
            )
        }
    }
}