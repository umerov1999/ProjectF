package dev.ragnarok.fenrir.fragment.audio.catalog_v2.listedit

import android.os.Bundle
import dev.ragnarok.fenrir.fragment.base.core.AbsPresenter
import dev.ragnarok.fenrir.settings.Settings
import java.util.*

class CatalogV2ListEditPresenter(savedInstanceState: Bundle?) :
    AbsPresenter<ICatalogV2ListEditView>(savedInstanceState) {
    private val data: List<Int> = Settings.get().other().catalogV2ListSort

    override fun onGuiCreated(viewHost: ICatalogV2ListEditView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
    }

    private fun save() {
        Settings.get().other().catalogV2ListSort = data
    }

    fun fireSaveClick() {
        save()
        view?.goBackAndApplyChanges()
    }

    fun fireItemMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(data, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(data, i, i - 1)
            }
        }
    }

}
