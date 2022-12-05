package dev.ragnarok.fenrir.model.catalog_v2_audio

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.IIdComparable
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2Block
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2BlockResponse
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.AbsModelType
import dev.ragnarok.fenrir.model.ParcelableModelWrapper
import dev.ragnarok.fenrir.putBoolean
import dev.ragnarok.fenrir.readTypedObjectCompat
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.writeTypedObjectCompat
import kotlin.math.abs

class CatalogV2Block : AbsModel {
    var id: String? = null
        private set
    var data_type: String? = null
        private set
    var next_from: String? = null
        private set
    var buttons: List<CatalogV2Button>? = null
        private set
    val layout: CatalogV2Layout
    var badge: CatalogV2Badge? = null
        private set

    var items: ArrayList<AbsModel>? = null

    private var scrollToIt = false

    fun setScroll() {
        scrollToIt = true
    }

    fun isScroll(): Boolean {
        val ret = scrollToIt
        scrollToIt = false
        return ret
    }

    private inline fun <reified T : AbsModel, reified S : IIdComparable> parseAllItemsByIds(
        list: List<S>?,
        keys: List<String>?,
        transform: (S) -> T
    ) {
        keys ?: return
        val ret = ArrayList<T>()
        for (i in list.orEmpty()) {
            for (s in keys) {
                if (i.compareFullId(s)) {
                    ret.add(transform(i))
                }
            }
        }
        if (ret.isEmpty()) {
            return
        }
        items ?: run { items = ArrayList() }
        items?.addAll(ret)
    }

    fun update(object_v: CatalogV2Block) {
        next_from = object_v.next_from
        object_v.items?.let {
            items ?: run { items = ArrayList() }
            items?.addAll(it)
        }
    }

    constructor(object_v: VKApiCatalogV2Block) {
        id = object_v.id
        data_type = object_v.data_type
        next_from = object_v.next_from
        buttons = object_v.buttons?.let {
            val op = ArrayList<CatalogV2Button>(it.size)
            for (i in it) {
                op.add(CatalogV2Button(i))
            }
            op
        }
        layout = object_v.layout?.let { CatalogV2Layout(it, data_type) } ?: CatalogV2Layout()
        badge = object_v.badge?.let { CatalogV2Badge(it) }
    }

    private fun parsePlaylistCover(object_v: VKApiCatalogV2BlockResponse) {
        for (m in object_v.playlists.orEmpty()) {
            if (m.thumb_image.isNullOrEmpty()) {
                var fuser = false
                for (d in object_v.profiles.orEmpty()) {
                    if (d.id == m.owner_id) {
                        m.thumb_image =
                            Utils.firstNonEmptyString(d.photo_200, d.photo_100, d.photo_50)
                        fuser = true
                        break
                    }
                }
                if (!fuser) {
                    for (d in object_v.groups.orEmpty()) {
                        if (d.id == abs(m.owner_id)) {
                            m.thumb_image =
                                Utils.firstNonEmptyString(d.photo_200, d.photo_100, d.photo_50)
                            break
                        }
                    }
                }
            }
        }
    }

    constructor(object_v: VKApiCatalogV2BlockResponse) {
        val pobj = object_v.block ?: run {
            this@CatalogV2Block.layout = CatalogV2Layout()
            return
        }
        id = pobj.id
        data_type = pobj.data_type
        next_from = pobj.next_from
        buttons = pobj.buttons?.let {
            val op = ArrayList<CatalogV2Button>(it.size)
            for (i in it) {
                op.add(CatalogV2Button(i))
            }
            op
        }
        layout = pobj.layout?.let { CatalogV2Layout(it, data_type) } ?: CatalogV2Layout()
        badge = pobj.badge?.let { CatalogV2Badge(it) }

        if (data_type == "music_recommended_playlists") {
            parsePlaylistCover(object_v)
            parseAllItemsByIds(object_v.playlists, pobj.playlists_ids) {
                Dto2Model.transform(it)
            }
            if (items.isNullOrEmpty()) {
                parseAllItemsByIds(object_v.audios, pobj.audios_ids) {
                    Dto2Model.transform(it)
                }
            }
        } else {
            parseAllItemsByIds(object_v.audios, pobj.audios_ids) {
                Dto2Model.transform(it)
            }
            parsePlaylistCover(object_v)
            parseAllItemsByIds(object_v.playlists, pobj.playlists_ids) {
                Dto2Model.transform(it)
            }
            parseAllItemsByIds(object_v.artists, pobj.artists_ids) {
                CatalogV2ArtistItem(it)
            }
            parseAllItemsByIds(object_v.links, pobj.links_ids) {
                CatalogV2Link(it).setParentLayout(layout.name)
            }
            parseAllItemsByIds(object_v.artist_videos, pobj.artist_videos_ids) {
                Dto2Model.transform(it)
            }
            parseAllItemsByIds(object_v.videos, pobj.videos_ids) {
                Dto2Model.transform(it)
            }
        }
    }

    constructor() {
        layout = CatalogV2Layout()
    }

    constructor(parcel: Parcel) {
        id = parcel.readString()
        data_type = parcel.readString()
        next_from = parcel.readString()
        buttons = parcel.createTypedArrayList(CatalogV2Button)
        layout = parcel.readTypedObjectCompat(CatalogV2Layout.CREATOR) ?: CatalogV2Layout()
        badge = parcel.readTypedObjectCompat(CatalogV2Badge.CREATOR)
        items = ParcelableModelWrapper.readModels(parcel)
        scrollToIt = parcel.getBoolean()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(data_type)
        parcel.writeString(next_from)
        parcel.writeTypedList(buttons)
        parcel.writeTypedObjectCompat(layout, flags)
        parcel.writeTypedObjectCompat(badge, flags)
        ParcelableModelWrapper.writeModels(parcel, flags, items)
        parcel.putBoolean(scrollToIt)
    }

    @AbsModelType
    override fun getModelType(): Int {
        return AbsModelType.MODEL_CATALOG_V2_BLOCK
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CatalogV2Block> {
        override fun createFromParcel(parcel: Parcel): CatalogV2Block {
            return CatalogV2Block(parcel)
        }

        override fun newArray(size: Int): Array<CatalogV2Block?> {
            return arrayOfNulls(size)
        }
    }
}