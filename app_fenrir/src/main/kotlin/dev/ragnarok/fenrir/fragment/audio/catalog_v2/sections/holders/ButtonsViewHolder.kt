package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.holders

import android.app.Activity
import android.view.View
import com.google.android.material.button.MaterialButton
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Block
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Button
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils

class ButtonsViewHolder(itemView: View) : IViewHolder(itemView) {
    private val button: MaterialButton
    private val context: Activity?

    init {
        button = itemView.findViewById(R.id.buttonNext)
        context = if (itemView.context is Activity) {
            itemView.context as Activity
        } else {
            null
        }
    }

    override fun bind(position: Int, itemDataHolder: AbsModel) {
        if (itemDataHolder !is CatalogV2Block) {
            button.visibility = View.GONE
            itemView.layoutParams.height = Utils.dp(1f)
            return
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
                itemView.layoutParams.height = Utils.dp(1f)
            } else {
                if (button.visibility != View.VISIBLE) {
                    button.visibility = View.VISIBLE
                }
                itemView.layoutParams.height = Utils.dp(48f)
                button.text = catalogAction.title
                button.setOnClickListener {
                    if (catalogAction.action?.type == "open_url") {
                        if (context != null) {
                            LinkHelper.openUrl(
                                context,
                                Settings.get().accounts().current,
                                catalogAction.action?.url,
                                false
                            )
                        }
                    } else {
                        catalogAction.section_id?.let { it1 ->
                            if (context != null) {
                                PlaceFactory.getCatalogV2AudioSectionPlace(
                                    Settings.get().accounts().current,
                                    it1
                                ).tryOpenWith(context)
                            }
                        }
                    }
                }
            }
        } else {
            if (button.visibility != View.GONE) {
                button.visibility = View.GONE
            }
            itemView.layoutParams.height = Utils.dp(1f)
        }
    }

    class Fabric : ViewHolderFabric {
        override fun create(view: View): IViewHolder {
            return ButtonsViewHolder(
                view
            )
        }

        override fun getLayout(): Int {
            return R.layout.item_catalog_v2_extra_button
        }
    }
}
