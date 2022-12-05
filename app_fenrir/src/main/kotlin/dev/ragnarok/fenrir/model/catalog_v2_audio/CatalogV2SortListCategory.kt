package dev.ragnarok.fenrir.model.catalog_v2_audio

import androidx.annotation.IntDef

@IntDef(
    CatalogV2SortListCategory.TYPE_CATALOG,
    CatalogV2SortListCategory.TYPE_LOCAL_AUDIO,
    CatalogV2SortListCategory.TYPE_LOCAL_SERVER_AUDIO,
    CatalogV2SortListCategory.TYPE_AUDIO,
    CatalogV2SortListCategory.TYPE_PLAYLIST,
    CatalogV2SortListCategory.TYPE_RECOMMENDATIONS
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class CatalogV2SortListCategory {
    companion object {
        const val TYPE_CATALOG = 0
        const val TYPE_LOCAL_AUDIO = 1
        const val TYPE_LOCAL_SERVER_AUDIO = 2
        const val TYPE_AUDIO = 3
        const val TYPE_PLAYLIST = 4
        const val TYPE_RECOMMENDATIONS = 5
    }
}