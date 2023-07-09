package dev.ragnarok.fenrir.model.catalog_v2_audio

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.IIdComparable
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2SectionResponse
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.util.Utils
import kotlin.math.abs

class CatalogV2Section : Parcelable {
    var blocks: List<AbsModel>? = null
        private set

    /*
    var buttons: List<CatalogV2Button>? = null
        private set
     */
    var id: String? = null
        private set
    var title: String? = null
        private set
    var url: String? = null
        private set
    var next_from: String? = null
        private set
    var listContentType: String? = null
        private set

    private inline fun <reified T : AbsModel, reified S : IIdComparable> parseAllItemsByIds(
        list: List<S>?,
        keys: List<String>?,
        transform: (S) -> T
    ): ArrayList<T>? {
        keys ?: return null
        val ret = ArrayList<T>()
        for (i in list.orEmpty()) {
            for (s in keys) {
                if (i.compareFullId(s)) {
                    ret.add(transform(i))
                }
            }
        }
        if (ret.isEmpty()) {
            return null
        }
        return ret
    }

    private inline fun <reified T : AbsModel, reified S : IIdComparable> insertAllItemsByIds(
        block: CatalogV2Block,
        list: List<S>?,
        keys: List<String>?,
        transform: (S) -> T
    ) {
        val ret = parseAllItemsByIds(list, keys, transform)
        if (ret.isNullOrEmpty()) {
            return
        }
        block.items ?: run { block.items = ArrayList() }
        block.items?.addAll(ret)
    }

    private fun parsePlaylistCover(object_v: VKApiCatalogV2SectionResponse) {
        for (m in object_v.playlists.orEmpty()) {
            if (m.thumb_image.isNullOrEmpty()) {
                if (m.owner_id >= 0) {
                    for (d in object_v.profiles.orEmpty()) {
                        if (d.id == m.owner_id) {
                            m.thumb_image =
                                Utils.firstNonEmptyString(d.photo_200, d.photo_100, d.photo_50)
                            break
                        }
                    }
                } else {
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

    constructor(object_v: VKApiCatalogV2SectionResponse) {
        blocks = object_v.section?.blocks?.let { obj ->
            val op = ArrayList<AbsModel>(obj.size)
            for ((ss, i) in obj.withIndex()) {
                val s = CatalogV2Block(i)
                if (i.data_type == "music_recommended_playlists") {
                    insertAllItemsByIds(s, object_v.recommended_playlists, i.playlists_ids) {
                        CatalogV2RecommendationPlaylist(
                            it,
                            object_v.profiles,
                            object_v.groups,
                            object_v.playlists
                        ) { op ->
                            parseAllItemsByIds(object_v.audios, op) { ist ->
                                Dto2Model.transform(ist)
                            }
                        }
                    }
                } else {
                    insertAllItemsByIds(s, object_v.audios, i.audios_ids) {
                        Dto2Model.transform(it)
                    }
                    parsePlaylistCover(object_v)
                    insertAllItemsByIds(s, object_v.playlists, i.playlists_ids) {
                        Dto2Model.transform(it)
                    }
                    insertAllItemsByIds(s, object_v.artists, i.artists_ids) {
                        CatalogV2ArtistItem(it)
                    }
                    insertAllItemsByIds(s, object_v.links, i.links_ids) {
                        CatalogV2Link(it).setParentLayout(i.layout?.name)
                    }
                    insertAllItemsByIds(s, object_v.artist_videos, i.artist_videos_ids) {
                        Dto2Model.transform(it)
                    }
                    insertAllItemsByIds(s, object_v.videos, i.videos_ids) {
                        Dto2Model.transform(it)
                    }
                }
                if (s.layout.getViewHolderType() == CatalogV2Layout.CATALOG_V2_HOLDER.TYPE_CATALOG_LIST && ss >= obj.size - 1) {
                    s.items?.let {
                        op.addAll(it)
                        listContentType = s.data_type
                    }
                } else {
                    listContentType = null
                    op.add(s)
                }
            }
            op
        }
        /*
        buttons = object_v.section?.buttons?.let {
            val op = ArrayList<CatalogV2Button>(it.size)
            for (i in it) {
                op.add(CatalogV2Button(i))
            }
            op
        }
         */
        id = object_v.section?.id
        title = object_v.section?.title
        url = object_v.section?.url
        next_from = object_v.section?.next_from
    }

    constructor(parcel: Parcel) {
        blocks = parcel.createTypedArrayList(CatalogV2Block)
        //buttons = parcel.createTypedArrayList(CatalogV2Button)
        id = parcel.readString()
        title = parcel.readString()
        url = parcel.readString()
        next_from = parcel.readString()
        listContentType = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(blocks)
        //parcel.writeTypedList(buttons)
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(url)
        parcel.writeString(next_from)
        parcel.writeString(listContentType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CatalogV2Section> {
        override fun createFromParcel(parcel: Parcel): CatalogV2Section {
            return CatalogV2Section(parcel)
        }

        override fun newArray(size: Int): Array<CatalogV2Section?> {
            return arrayOfNulls(size)
        }
    }
}
