package dev.ragnarok.fenrir.model

import android.os.Parcel
import android.os.Parcelable

class StickersKeywords : Parcelable {
    private val keywords: List<String>
    private val stickers: List<Sticker>

    internal constructor(parcel: Parcel) {
        keywords = ArrayList()
        parcel.readStringList(keywords)
        stickers = parcel.createTypedArrayList(Sticker.CREATOR).orEmpty()
    }

    constructor(keywords: List<String>, stickers: List<Sticker>) {
        this.keywords = keywords
        this.stickers = stickers
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeStringList(keywords)
        dest.writeTypedList(stickers)
    }

    fun getKeywords(): List<String> {
        return keywords
    }

    fun getStickers(): List<Sticker> {
        return stickers
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<StickersKeywords> {
        override fun createFromParcel(parcel: Parcel): StickersKeywords {
            return StickersKeywords(parcel)
        }

        override fun newArray(size: Int): Array<StickersKeywords?> {
            return arrayOfNulls(size)
        }
    }
}