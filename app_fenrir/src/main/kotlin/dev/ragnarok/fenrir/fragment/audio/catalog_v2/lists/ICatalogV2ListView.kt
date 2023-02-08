package dev.ragnarok.fenrir.fragment.audio.catalog_v2.lists

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2List

interface ICatalogV2ListView : IMvpView, IErrorView {
    fun displayData(sections: List<CatalogV2List.CatalogV2ListItem>)
    fun setSection(position: Int)
    fun notifyDataSetChanged()
    fun resolveLoading(visible: Boolean)
}
