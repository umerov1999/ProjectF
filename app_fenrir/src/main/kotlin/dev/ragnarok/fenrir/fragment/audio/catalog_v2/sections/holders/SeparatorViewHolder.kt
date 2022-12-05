package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.holders

import android.view.View
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.AbsModel

class SeparatorViewHolder(itemView: View) : IViewHolder(itemView) {
    override fun bind(position: Int, itemDataHolder: AbsModel) {}
    class Fabric : ViewHolderFabric {
        override fun create(view: View): IViewHolder {
            return SeparatorViewHolder(
                view
            )
        }

        override fun getLayout(): Int {
            return R.layout.item_catalog_v2_separator
        }
    }
}