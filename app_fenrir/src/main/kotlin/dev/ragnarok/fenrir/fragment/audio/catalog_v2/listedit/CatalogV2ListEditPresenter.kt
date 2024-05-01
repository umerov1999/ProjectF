package dev.ragnarok.fenrir.fragment.audio.catalog_v2.listedit

import dev.ragnarok.fenrir.fragment.base.core.AbsPresenter
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.swap

class CatalogV2ListEditPresenter :
    AbsPresenter<ICatalogV2ListEditView>() {
    private val data: ArrayList<Int> = ArrayList(Settings.get().main().catalogV2ListSort)

    override fun onGuiCreated(viewHost: ICatalogV2ListEditView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
    }

    private fun save() {
        Settings.get().main().catalogV2ListSort = data
    }

    fun fireSaveClick() {
        save()
        view?.goBackAndApplyChanges()
    }

    fun fireItemMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                data.swap(i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                data.swap(i, i - 1)
            }
        }
    }

}
