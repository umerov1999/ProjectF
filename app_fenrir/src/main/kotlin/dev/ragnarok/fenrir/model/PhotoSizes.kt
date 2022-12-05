package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import dev.ragnarok.fenrir.module.parcel.ParcelNative
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.util.Utils.firstNonNull
import dev.ragnarok.fenrir.writeTypedObjectCompat
import kotlinx.serialization.Serializable

@Keep
@Serializable
class PhotoSizes : Parcelable, ParcelNative.ParcelableNative {
    private var s: Size? = null
    private var m: Size? = null
    private var x: Size? = null
    private var o: Size? = null
    private var p: Size? = null
    private var q: Size? = null
    private var r: Size? = null
    private var y: Size? = null
    private var z: Size? = null
    private var w: Size? = null

    constructor()
    internal constructor(`in`: ParcelNative) {
        s = `in`.readParcelable(Size.NativeCreator)
        m = `in`.readParcelable(Size.NativeCreator)
        x = `in`.readParcelable(Size.NativeCreator)
        o = `in`.readParcelable(Size.NativeCreator)
        p = `in`.readParcelable(Size.NativeCreator)
        q = `in`.readParcelable(Size.NativeCreator)
        r = `in`.readParcelable(Size.NativeCreator)
        y = `in`.readParcelable(Size.NativeCreator)
        z = `in`.readParcelable(Size.NativeCreator)
        w = `in`.readParcelable(Size.NativeCreator)
    }

    internal constructor(`in`: Parcel) {
        s = `in`.readTypedObjectCompat(Size.CREATOR)
        m = `in`.readTypedObjectCompat(Size.CREATOR)
        x = `in`.readTypedObjectCompat(Size.CREATOR)
        o = `in`.readTypedObjectCompat(Size.CREATOR)
        p = `in`.readTypedObjectCompat(Size.CREATOR)
        q = `in`.readTypedObjectCompat(Size.CREATOR)
        r = `in`.readTypedObjectCompat(Size.CREATOR)
        y = `in`.readTypedObjectCompat(Size.CREATOR)
        z = `in`.readTypedObjectCompat(Size.CREATOR)
        w = `in`.readTypedObjectCompat(Size.CREATOR)
    }

    fun getS(): Size? {
        return s
    }

    fun setS(s: Size?): PhotoSizes {
        this.s = s
        return this
    }

    fun getM(): Size? {
        return m
    }

    fun setM(m: Size?): PhotoSizes {
        this.m = m
        return this
    }

    fun getX(): Size? {
        return x
    }

    fun setX(x: Size?): PhotoSizes {
        this.x = x
        return this
    }

    fun getO(): Size? {
        return o
    }

    fun setO(o: Size?): PhotoSizes {
        this.o = o
        return this
    }

    fun getP(): Size? {
        return p
    }

    fun setP(p: Size?): PhotoSizes {
        this.p = p
        return this
    }

    fun getQ(): Size? {
        return q
    }

    fun setQ(q: Size?): PhotoSizes {
        this.q = q
        return this
    }

    fun getR(): Size? {
        return r
    }

    fun setR(r: Size?): PhotoSizes {
        this.r = r
        return this
    }

    fun getY(): Size? {
        return y
    }

    fun setY(y: Size?): PhotoSizes {
        this.y = y
        return this
    }

    fun getZ(): Size? {
        return z
    }

    fun setZ(z: Size?): PhotoSizes {
        this.z = z
        return this
    }

    fun getW(): Size? {
        return w
    }

    fun setW(w: Size?): PhotoSizes {
        this.w = w
        return this
    }

    fun getMaxSize(excludeNonAspectRatio: Boolean): Size? {
        return if (excludeNonAspectRatio) firstNonNull(
            w,
            z,
            y,
            x,
            m,
            s
        ) else firstNonNull(w, z, y, r, q, p, o, x, m, s)
    }

    fun getSize(@PhotoSize max: Int, excludeNonAspectRatio: Boolean): Size? {
        return when (max) {
            PhotoSize.S -> s
            PhotoSize.M -> firstNonNull(m, s)
            PhotoSize.X -> firstNonNull(x, m, s)
            PhotoSize.O -> if (excludeNonAspectRatio) firstNonNull(
                x,
                m,
                s
            ) else firstNonNull(o, x, m, s)
            PhotoSize.P -> if (excludeNonAspectRatio) firstNonNull(
                x,
                m,
                s
            ) else firstNonNull(p, o, x, m, s)
            PhotoSize.Q -> if (excludeNonAspectRatio) firstNonNull(
                x,
                m,
                s
            ) else firstNonNull(q, p, o, x, m, s)
            PhotoSize.R -> if (excludeNonAspectRatio) firstNonNull(
                x,
                m,
                s
            ) else firstNonNull(r, q, p, o, x, m, s)
            PhotoSize.Y -> if (excludeNonAspectRatio) firstNonNull(
                y,
                x,
                m,
                s
            ) else firstNonNull(y, r, q, p, o, x, m, s)
            PhotoSize.Z -> if (excludeNonAspectRatio) firstNonNull(
                z,
                y,
                x,
                m,
                s
            ) else firstNonNull(z, y, r, q, p, o, x, m, s)
            PhotoSize.W -> if (excludeNonAspectRatio) firstNonNull(
                w,
                z,
                y,
                x,
                m,
                s
            ) else firstNonNull(w, z, y, r, q, p, o, x, m, s)
            else -> throw IllegalArgumentException("Invalid max photo size: $max")
        }
    }

    fun getUrlForSize(@PhotoSize maxSize: Int, excludeNonAspectRatio: Boolean): String? {
        val s = getSize(maxSize, excludeNonAspectRatio)
        return s?.url
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeTypedObjectCompat(s, i)
        parcel.writeTypedObjectCompat(m, i)
        parcel.writeTypedObjectCompat(x, i)
        parcel.writeTypedObjectCompat(o, i)
        parcel.writeTypedObjectCompat(p, i)
        parcel.writeTypedObjectCompat(q, i)
        parcel.writeTypedObjectCompat(r, i)
        parcel.writeTypedObjectCompat(y, i)
        parcel.writeTypedObjectCompat(z, i)
        parcel.writeTypedObjectCompat(w, i)
    }

    fun isEmpty(): Boolean {
        return firstNonNull(s, m, x, o, p, q, r, y, z, w) == null
    }

    fun notEmpty(): Boolean {
        return firstNonNull(s, m, x, o, p, q, r, y, z, w) != null
    }

    override fun writeToParcelNative(dest: ParcelNative) {
        dest.writeParcelable(s)
        dest.writeParcelable(m)
        dest.writeParcelable(x)
        dest.writeParcelable(o)
        dest.writeParcelable(p)
        dest.writeParcelable(q)
        dest.writeParcelable(r)
        dest.writeParcelable(y)
        dest.writeParcelable(z)
        dest.writeParcelable(w)
    }

    @Keep
    @Serializable
    class Size : Parcelable, ParcelNative.ParcelableNative {
        private var width = 0
        private var height = 0
        var url: String? = null

        constructor()
        constructor(w: Int, h: Int, url: String?) {
            width = w
            height = h
            this.url = url
        }

        internal constructor(`in`: ParcelNative) {
            width = `in`.readInt()
            height = `in`.readInt()
            url = `in`.readString()
        }

        internal constructor(`in`: Parcel) {
            width = `in`.readInt()
            height = `in`.readInt()
            url = `in`.readString()
        }

        fun getW(): Int {
            return width
        }

        fun getH(): Int {
            return height
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(width)
            dest.writeInt(height)
            dest.writeString(url)
        }

        override fun writeToParcelNative(dest: ParcelNative) {
            dest.writeInt(width)
            dest.writeInt(height)
            dest.writeString(url)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<Size> = object : Parcelable.Creator<Size> {
                override fun createFromParcel(`in`: Parcel): Size {
                    return Size(`in`)
                }

                override fun newArray(size: Int): Array<Size?> {
                    return arrayOfNulls(size)
                }
            }
            val NativeCreator: ParcelNative.Creator<Size> =
                object : ParcelNative.Creator<Size> {
                    override fun readFromParcelNative(dest: ParcelNative): Size {
                        return Size(dest)
                    }

                }
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PhotoSizes> = object : Parcelable.Creator<PhotoSizes> {
            override fun createFromParcel(`in`: Parcel): PhotoSizes {
                return PhotoSizes(`in`)
            }

            override fun newArray(size: Int): Array<PhotoSizes?> {
                return arrayOfNulls(size)
            }
        }
        val NativeCreator: ParcelNative.Creator<PhotoSizes> =
            object : ParcelNative.Creator<PhotoSizes> {
                override fun readFromParcelNative(dest: ParcelNative): PhotoSizes {
                    return PhotoSizes(dest)
                }

            }
    }
}