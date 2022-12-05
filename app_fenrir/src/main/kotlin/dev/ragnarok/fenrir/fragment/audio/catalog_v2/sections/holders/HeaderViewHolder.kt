package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.holders

import android.view.View
import android.view.ViewStub
import android.widget.TextView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Block
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Button
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils

class HeaderViewHolder(itemView: View) : IViewHolder(itemView) {
    private val title: TextView
    private var badge: TextView? = null
    private val button: CatalogHeaderButton
    private val badgeStub: ViewStub?

    init {
        title = itemView.findViewById(R.id.title)
        button = itemView.findViewById(R.id.button)
        badgeStub = itemView.findViewById(R.id.badgeStub)
    }

    override fun bind(position: Int, itemDataHolder: AbsModel) {
        if (itemDataHolder !is CatalogV2Block) {
            title.visibility = View.GONE
            button.visibility = View.GONE
            badgeStub?.visibility = View.GONE
            itemView.layoutParams.height = Utils.dp(1f)
            return
        }
        var s = 0
        val catalogLayout = itemDataHolder.layout
        title.text = catalogLayout.title
        if (catalogLayout.title.isNullOrEmpty()) {
            if (title.visibility != View.GONE) {
                title.visibility = View.GONE
            }
            s++
        } else {
            if (title.visibility != View.VISIBLE) {
                title.visibility = View.VISIBLE
            }
        }
        if (itemDataHolder.badge != null) {
            if (badge == null) {
                badge = if (badgeStub != null) {
                    badgeStub.inflate() as TextView
                } else {
                    itemView.findViewById(R.id.badge)
                }
            }
            if (badge?.visibility != View.VISIBLE) {
                badge?.visibility = View.VISIBLE
            }
            badge?.text = itemDataHolder.badge?.text
        } else {
            if (badge?.visibility != View.GONE) {
                badge?.visibility = View.GONE
            }
            s++
        }
        if (itemDataHolder.buttons.nonNullNoEmpty()) {
            var catalogAction: CatalogV2Button? = null
            for (i in itemDataHolder.buttons.orEmpty()) {
                if (i.action?.type == "open_section") {
                    //"select_sorting" -> {}
                    catalogAction = i
                }
            }
            if (catalogAction == null) {
                for (i in itemDataHolder.buttons.orEmpty()) {
                    if (i.action?.type == "open_url") {
                        catalogAction = i
                    }
                }
            }
            if (catalogAction == null) {
                if (button.visibility != View.GONE) {
                    button.visibility = View.GONE
                }
                s++
            } else {
                if (button.visibility != View.VISIBLE) {
                    button.visibility = View.VISIBLE
                }
                button.setUpWithCatalogAction(catalogAction)
                button.setOnClickListener {
                }
            }
        } else {
            if (button.visibility != View.GONE) {
                button.visibility = View.GONE
            }
            s++
        }
        if (s == 3) {
            itemView.layoutParams.height = Utils.dp(1f)
        } else {
            itemView.layoutParams.height = Utils.dp(48f)
        }
    }

    class Fabric : ViewHolderFabric {
        override fun create(view: View): IViewHolder {
            return HeaderViewHolder(
                view
            )
        }

        override fun getLayout(): Int {
            return R.layout.item_catalog_v2_header
        }
    }
}