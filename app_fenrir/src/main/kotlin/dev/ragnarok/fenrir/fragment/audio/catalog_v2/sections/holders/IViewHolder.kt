package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.model.AbsModel

abstract class IViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(position: Int, itemDataHolder: AbsModel)
}