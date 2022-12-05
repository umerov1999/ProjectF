package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.holders

import android.view.View
import android.widget.ImageView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2ArtistItem
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Block
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.picasso.transforms.BlurTransformation

class ArtistBannerViewHolder(itemView: View) : IViewHolder(itemView) {
    private val photo: ImageView

    init {
        photo = itemView.findViewById(R.id.photo)
    }

    override fun bind(position: Int, itemDataHolder: AbsModel) {
        if (itemDataHolder !is CatalogV2Block || itemDataHolder.items?.get(0) !is CatalogV2ArtistItem) {
            photo.visibility = View.GONE
            return
        }
        val artistItem: CatalogV2ArtistItem =
            itemDataHolder.items?.get(0) as CatalogV2ArtistItem? ?: CatalogV2ArtistItem()
        with().cancelRequest(photo)
        val imgUrl = artistItem.getPhoto()
        if (imgUrl.nonNullNoEmpty()) {
            if (artistItem.is_album_cover) {
                with().load(imgUrl).transform(BlurTransformation(2.1f, itemView.context))
                    .into(photo)
            } else {
                with().load(imgUrl).into(photo)
            }
        }
    }

    class Fabric : ViewHolderFabric {
        override fun create(view: View): IViewHolder {
            return ArtistBannerViewHolder(
                view
            )
        }

        override fun getLayout(): Int {
            return R.layout.item_catalog_v2_artist_item_banner
        }
    }
}