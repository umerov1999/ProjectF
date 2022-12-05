package dev.ragnarok.fenrir.model.catalog_v2_audio

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2Badge

class CatalogV2Badge : Parcelable {
    var text: String? = null
        private set
    var type: String? = null
        private set

    constructor(object_v: VKApiCatalogV2Badge) {
        text = object_v.text
        type = object_v.type
    }

    constructor(parcel: Parcel) {
        text = parcel.readString()
        type = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(text)
        parcel.writeString(type)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CatalogV2Badge> {
        override fun createFromParcel(parcel: Parcel): CatalogV2Badge {
            return CatalogV2Badge(parcel)
        }

        override fun newArray(size: Int): Array<CatalogV2Badge?> {
            return arrayOfNulls(size)
        }
    }
}