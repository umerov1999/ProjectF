package dev.ragnarok.fenrir.model.catalog_v2_audio

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2ArtistItem
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AbsModelType
import dev.ragnarok.fenrir.putBoolean
import kotlin.math.abs

class CatalogV2ArtistItem : AbsModel {
    var name: String? = null
    var id: String? = null
    var is_album_cover: Boolean = false
    var photo: List<CatalogV2Cover>? = null

    constructor(object_v: VKApiCatalogV2ArtistItem) {
        name = object_v.name
        id = object_v.id
        is_album_cover = object_v.is_album_cover
        photo = object_v.photo?.let {
            val op = ArrayList<CatalogV2Cover>(it.size)
            for (i in it) {
                op.add(CatalogV2Cover(i))
            }
            op
        }
    }

    fun getPhoto(): String? {
        var minDifference = 100000
        val itemViewWidth: Int = Constants.SCREEN_WIDTH
        var imgUrl: String? = null
        for (i in photo.orEmpty()) {
            val difference = abs(itemViewWidth - i.width)
            if (difference < minDifference) {
                minDifference = difference
                imgUrl = i.url
            }
        }
        imgUrl ?: run {
            imgUrl = photo?.last()?.url
        }
        return imgUrl
    }

    constructor()

    constructor(parcel: Parcel) {
        name = parcel.readString()
        id = parcel.readString()
        is_album_cover = parcel.getBoolean()
        photo = parcel.createTypedArrayList(CatalogV2Cover.CREATOR)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(id)
        parcel.putBoolean(is_album_cover)
        parcel.writeTypedList(photo)
    }

    override fun describeContents(): Int {
        return 0
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_AUDIO_CATALOG_V2_ARTIST
    }

    class CatalogV2Cover : Parcelable {
        var width: Int = 0
        var url: String? = null

        constructor(object_v: VKApiCatalogV2ArtistItem.VKApiCatalogV2Cover) {
            width = object_v.width
            url = object_v.url
        }

        constructor(parcel: Parcel) {
            width = parcel.readInt()
            url = parcel.readString()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(width)
            parcel.writeString(url)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<CatalogV2Cover> {
            override fun createFromParcel(parcel: Parcel): CatalogV2Cover {
                return CatalogV2Cover(parcel)
            }

            override fun newArray(size: Int): Array<CatalogV2Cover?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<CatalogV2ArtistItem> {
        override fun createFromParcel(parcel: Parcel): CatalogV2ArtistItem {
            return CatalogV2ArtistItem(parcel)
        }

        override fun newArray(size: Int): Array<CatalogV2ArtistItem?> {
            return arrayOfNulls(size)
        }
    }
}
