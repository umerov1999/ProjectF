package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import kotlinx.serialization.Serializable

@Serializable
class PhotoTags : Parcelable, ParcelNative.ParcelableNative {
    private var id = 0
    private var user_id = 0L
    private var placer_id = 0L
    private var tagged_name: String? = null
    private var date: Long = 0L
    private var x = 0.0
    private var y = 0.0
    private var x2 = 0.0
    private var y2 = 0.0
    private var viewed = 0

    constructor(id: Int, user_id: Long) {
        this.id = id
        this.user_id = user_id
    }

    internal constructor(parcel: Parcel) {
        id = parcel.readInt()
        user_id = parcel.readLong()
        placer_id = parcel.readLong()
        tagged_name = parcel.readString()
        date = parcel.readLong()
        x = parcel.readDouble()
        y = parcel.readDouble()
        x2 = parcel.readDouble()
        y2 = parcel.readDouble()
        viewed = parcel.readInt()
    }

    internal constructor(parcel: ParcelNative) {
        id = parcel.readInt()
        user_id = parcel.readLong()
        placer_id = parcel.readLong()
        tagged_name = parcel.readString()
        date = parcel.readLong()
        x = parcel.readDouble()
        y = parcel.readDouble()
        x2 = parcel.readDouble()
        y2 = parcel.readDouble()
        viewed = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(id)
        parcel.writeLong(user_id)
        parcel.writeLong(placer_id)
        parcel.writeString(tagged_name)
        parcel.writeLong(date)
        parcel.writeDouble(x)
        parcel.writeDouble(y)
        parcel.writeDouble(x2)
        parcel.writeDouble(y2)
        parcel.writeInt(viewed)
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeInt(id)
        dest.writeLong(user_id)
        dest.writeLong(placer_id)
        dest.writeString(tagged_name)
        dest.writeLong(date)
        dest.writeDouble(x)
        dest.writeDouble(y)
        dest.writeDouble(x2)
        dest.writeDouble(y2)
        dest.writeInt(viewed)
    }

    fun getId(): Int {
        return id
    }

    fun getUserId(): Long {
        return user_id
    }

    fun getPlacerId(): Long {
        return placer_id
    }

    fun setPlacerId(placer_id: Long): PhotoTags {
        this.placer_id = placer_id
        return this
    }

    fun getTaggedName(): String? {
        return tagged_name
    }

    fun setTaggedName(tagged_name: String?): PhotoTags {
        this.tagged_name = tagged_name
        return this
    }

    fun getDate(): Long {
        return date
    }

    fun setDate(date: Long): PhotoTags {
        this.date = date
        return this
    }

    fun getX(): Double {
        return x
    }

    fun setX(x: Double): PhotoTags {
        this.x = x
        return this
    }

    fun getY(): Double {
        return y
    }

    fun setY(y: Double): PhotoTags {
        this.y = y
        return this
    }

    fun getX2(): Double {
        return x2
    }

    fun setX2(x2: Double): PhotoTags {
        this.x2 = x2
        return this
    }

    fun getY2(): Double {
        return y2
    }

    fun setY2(y2: Double): PhotoTags {
        this.y2 = y2
        return this
    }

    fun getViewed(): Int {
        return viewed
    }

    fun setViewed(viewed: Int): PhotoTags {
        this.viewed = viewed
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PhotoTags> = object : Parcelable.Creator<PhotoTags> {
            override fun createFromParcel(parcel: Parcel): PhotoTags {
                return PhotoTags(parcel)
            }

            override fun newArray(size: Int): Array<PhotoTags?> {
                return arrayOfNulls(size)
            }
        }
        val NativeCreator: ParcelNative.Creator<PhotoTags> =
            object : ParcelNative.Creator<PhotoTags> {
                override fun readFromParcelNative(dest: ParcelNative): PhotoTags {
                    return PhotoTags(dest)
                }

            }
    }
}
