package dev.ragnarok.fenrir.model.catalog_v2_audio

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2Action

class CatalogV2Action : Parcelable {
    var type: String? = null
        private set
    var target: String? = null
        private set
    var url: String? = null
        private set

    constructor(object_v: VKApiCatalogV2Action) {
        type = object_v.type
        target = object_v.target
        url = object_v.url
    }

    constructor(parcel: Parcel) {
        type = parcel.readString()
        target = parcel.readString()
        url = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        parcel.writeString(target)
        parcel.writeString(url)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CatalogV2Action> {
        override fun createFromParcel(parcel: Parcel): CatalogV2Action {
            return CatalogV2Action(parcel)
        }

        override fun newArray(size: Int): Array<CatalogV2Action?> {
            return arrayOfNulls(size)
        }
    }
}
