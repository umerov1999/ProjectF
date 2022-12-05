package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.holders

import android.view.View
import androidx.annotation.LayoutRes

interface ViewHolderFabric {
    fun create(view: View): IViewHolder

    @LayoutRes
    fun getLayout(): Int
}