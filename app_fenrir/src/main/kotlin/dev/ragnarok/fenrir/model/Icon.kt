package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import dev.ragnarok.fenrir.util.ParcelUtils.readObjectInteger
import dev.ragnarok.fenrir.util.ParcelUtils.writeObjectInteger

class Icon : Parcelable {
    @DrawableRes
    val res: Int?
    val url: String?

    private constructor(res: Int?, url: String?) {
        this.res = res
        this.url = url
    }

    internal constructor(`in`: Parcel) {
        res = readObjectInteger(`in`)
        url = `in`.readString()
    }

    val isRemote: Boolean
        get() = url != null

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        writeObjectInteger(dest, res)
        dest.writeString(url)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Icon> = object : Parcelable.Creator<Icon> {
            override fun createFromParcel(`in`: Parcel): Icon {
                return Icon(`in`)
            }

            override fun newArray(size: Int): Array<Icon?> {
                return arrayOfNulls(size)
            }
        }

        fun fromUrl(url: String?): Icon {
            return Icon(null, url)
        }

        fun fromResources(@DrawableRes res: Int): Icon {
            return Icon(res, null)
        }
    }
}