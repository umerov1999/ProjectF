package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.holders

import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import android.widget.TextView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Block
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Button
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.util.Utils

class HeaderViewHolder(itemView: View) : IViewHolder(itemView) {
    private val title: TextView
    private var badge: TextView? = null
    private val button: CatalogHeaderButton
    private val badgeStub: ViewStub?
    private val extension: ViewGroup
    private val top_title_icon: ImageView
    private val top_title: TextView

    init {
        title = itemView.findViewById(R.id.title)
        button = itemView.findViewById(R.id.button)
        badgeStub = itemView.findViewById(R.id.badgeStub)
        extension = itemView.findViewById(R.id.extension)
        top_title_icon = itemView.findViewById(R.id.top_title_icon)
        top_title = itemView.findViewById(R.id.top_title)
    }

    override fun bind(position: Int, itemDataHolder: AbsModel) {
        if (itemDataHolder !is CatalogV2Block) {
            title.visibility = View.GONE
            button.visibility = View.GONE
            badgeStub?.visibility = View.GONE
            itemView.layoutParams.height = Utils.dp(1f)
            extension.visibility = View.GONE
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
        if (catalogLayout.topTittleIcon.isNullOrEmpty() && catalogLayout.topTittleText.isNullOrEmpty()) {
            if (extension.visibility != View.GONE) {
                extension.visibility = View.GONE
            }
            if (s >= 3) {
                itemView.layoutParams.height = Utils.dp(1f)
            } else {
                itemView.layoutParams.height = Utils.dp(48f)
            }
        } else {
            if (extension.visibility != View.VISIBLE) {
                extension.visibility = View.VISIBLE
            }
            top_title.text = catalogLayout.topTittleText

            top_title_icon.visibility =
                if (catalogLayout.topTittleIcon.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            if (catalogLayout.topTittleIcon.nonNullNoEmpty()) {
                PicassoInstance.with()
                    .load(catalogLayout.topTittleIcon)
                    .tag(Constants.PICASSO_TAG)
                    .placeholder(R.drawable.background_gray)
                    .into(top_title_icon)
            } else {
                PicassoInstance.with().cancelRequest(top_title_icon)
            }
            itemView.layoutParams.height = Utils.dp(65f)
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