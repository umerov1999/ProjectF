package dev.ragnarok.fenrir.fragment.audio.catalog_v2.listedit

import dev.ragnarok.fenrir.fragment.base.core.IMvpView

interface ICatalogV2ListEditView : IMvpView {
    fun displayData(data: List<Int>)
    fun goBackAndApplyChanges()
}
