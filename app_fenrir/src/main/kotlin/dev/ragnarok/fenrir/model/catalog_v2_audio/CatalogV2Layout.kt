package dev.ragnarok.fenrir.model.catalog_v2_audio

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2Layout
import dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.holders.*
import dev.ragnarok.fenrir.model.AbsModelType

class CatalogV2Layout : Parcelable {
    var name: String? = null
        private set
    var data_type: String? = null
        private set
    var title: String? = null
        private set

    @IntDef(
        CATALOG_V2_HOLDER.TYPE_CATALOG_HEADER,
        CATALOG_V2_HOLDER.TYPE_CATALOG_SEPARATOR,
        CATALOG_V2_HOLDER.TYPE_CATALOG_SLIDER,
        CATALOG_V2_HOLDER.TYPE_CATALOG_TRIPLE_STACKED_SLIDER,
        CATALOG_V2_HOLDER.TYPE_CATALOG_LIST,
        CATALOG_V2_HOLDER.TYPE_ARTIST_BANNER,
        CATALOG_V2_HOLDER.TYPE_EMPTY
    )
    @Retention(
        AnnotationRetention.SOURCE
    )
    annotation class CATALOG_V2_HOLDER {
        companion object {
            const val TYPE_CATALOG_HEADER = -7
            const val TYPE_CATALOG_SEPARATOR = -6
            const val TYPE_CATALOG_SLIDER = -5
            const val TYPE_CATALOG_TRIPLE_STACKED_SLIDER = -4
            const val TYPE_CATALOG_LIST = -3
            const val TYPE_ARTIST_BANNER = -2
            const val TYPE_EMPTY = -1
        }
    }

    fun getViewHolderType(): Int {
        return when (name) {
            "header", "header_extended", "header_compact", "horizontal_buttons" -> {
                CATALOG_V2_HOLDER.TYPE_CATALOG_HEADER
            }
            "separator", "in_block_separator" -> {
                CATALOG_V2_HOLDER.TYPE_CATALOG_SEPARATOR
            }
            "list", "music_chart_list" -> {
                CATALOG_V2_HOLDER.TYPE_CATALOG_LIST
            }
            "categories_list", "large_list", "slider", "recomms_slider", "large_slider", "music_chart_large_slider" -> {
                CATALOG_V2_HOLDER.TYPE_CATALOG_SLIDER
            }
            "triple_stacked_slider", "music_chart_triple_stacked_slider" -> {
                CATALOG_V2_HOLDER.TYPE_CATALOG_TRIPLE_STACKED_SLIDER
            }
            "banner" -> {
                when (data_type) {
                    "artist" -> {
                        CATALOG_V2_HOLDER.TYPE_ARTIST_BANNER
                    }
                    else -> {
                        CATALOG_V2_HOLDER.TYPE_EMPTY
                    }
                }
            }
            else -> {
                CATALOG_V2_HOLDER.TYPE_EMPTY
            }
        }
    }

    constructor()

    constructor(object_v: VKApiCatalogV2Layout, data_type: String?) {
        name = object_v.name
        title = object_v.title
        this.data_type = data_type
    }

    constructor(parcel: Parcel) {
        name = parcel.readString()
        title = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(title)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<CatalogV2Layout> =
            object : Parcelable.Creator<CatalogV2Layout> {
                override fun createFromParcel(parcel: Parcel): CatalogV2Layout {
                    return CatalogV2Layout(parcel)
                }

                override fun newArray(size: Int): Array<CatalogV2Layout?> {
                    return arrayOfNulls(size)
                }
            }

        fun createHolder(viewType: Int): ViewHolderFabric {
            return when (viewType) {
                CATALOG_V2_HOLDER.TYPE_ARTIST_BANNER -> {
                    ArtistBannerViewHolder.Fabric()
                }
                CATALOG_V2_HOLDER.TYPE_CATALOG_HEADER -> {
                    HeaderViewHolder.Fabric()
                }
                CATALOG_V2_HOLDER.TYPE_CATALOG_SEPARATOR -> {
                    SeparatorViewHolder.Fabric()
                }
                AbsModelType.MODEL_CATALOG_V2_LINK -> {
                    LinkViewHolder.Fabric()
                }
                AbsModelType.MODEL_VIDEO -> {
                    VideoViewHolder.Fabric()
                }
                else -> HeaderViewHolder.Fabric()
            }
        }
    }
}