package dev.ragnarok.fenrir.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.util.ParcelUtils.readObjectInteger
import dev.ragnarok.fenrir.util.ParcelUtils.writeObjectInteger

class Text : Parcelable {
    @StringRes
    private var res: Int? = null
    private var text: String? = null

    constructor(res: Int?) {
        this.res = res
    }

    constructor(text: String?) {
        this.text = text
    }

    internal constructor(`in`: Parcel) {
        res = readObjectInteger(`in`)
        text = `in`.readString()
    }

    fun getText(context: Context): String? {
        return res?.let { context.getString(it) } ?: text
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        writeObjectInteger(parcel, res)
        parcel.writeString(text)
    }

    companion object CREATOR : Parcelable.Creator<Text> {
        override fun createFromParcel(parcel: Parcel): Text {
            return Text(parcel)
        }

        override fun newArray(size: Int): Array<Text?> {
            return arrayOfNulls(size)
        }
    }
}