package dev.ragnarok.fenrir.model.catalog_v2_audio

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2Link
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AbsModelType

class CatalogV2Link : AbsModel {
    var id: String? = null
        private set
    var url: String? = null
        private set
    var title: String? = null
        private set
    var subtitle: String? = null
        private set
    var preview_photo: String? = null
        private set
    var parentLayout: String? = null
        private set

    fun setParentLayout(layout: String?): CatalogV2Link {
        parentLayout = layout
        return this
    }

    constructor(object_v: VKApiCatalogV2Link) {
        id = object_v.id
        url = object_v.url
        title = object_v.title
        subtitle = object_v.subtitle
        preview_photo = object_v.preview_photo
    }

    constructor(parcel: Parcel) {
        id = parcel.readString()
        url = parcel.readString()
        title = parcel.readString()
        subtitle = parcel.readString()
        preview_photo = parcel.readString()
        parentLayout = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(url)
        parcel.writeString(title)
        parcel.writeString(subtitle)
        parcel.writeString(preview_photo)
        parcel.writeString(parentLayout)
    }

    override fun getModelType(): Int {
        return AbsModelType.MODEL_CATALOG_V2_LINK
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CatalogV2Link> {
        override fun createFromParcel(parcel: Parcel): CatalogV2Link {
            return CatalogV2Link(parcel)
        }

        override fun newArray(size: Int): Array<CatalogV2Link?> {
            return arrayOfNulls(size)
        }
    }
}