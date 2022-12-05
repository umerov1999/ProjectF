package dev.ragnarok.fenrir.model.menu.options

import androidx.annotation.IntDef

@IntDef(
    DocsOption.add_item_doc,
    DocsOption.delete_item_doc,
    DocsOption.share_item_doc,
    DocsOption.open_item_doc,
    DocsOption.go_to_owner_doc
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class DocsOption {
    companion object {
        const val add_item_doc = 1
        const val delete_item_doc = 2
        const val share_item_doc = 3
        const val open_item_doc = 4
        const val go_to_owner_doc = 5
    }
}