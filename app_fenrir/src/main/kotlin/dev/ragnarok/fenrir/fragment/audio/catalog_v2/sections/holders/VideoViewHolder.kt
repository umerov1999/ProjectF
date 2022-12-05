package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections.holders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.place.PlaceFactory.getVideoPreviewPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.view.VideoServiceIcons.getIconByType

class VideoViewHolder(itemView: View) : IViewHolder(itemView) {
    val card: View = itemView.findViewById(R.id.card_view)
    val image: ImageView = itemView.findViewById(R.id.video_image)
    val videoLenght: TextView = itemView.findViewById(R.id.video_lenght)
    val videoService: ImageView = itemView.findViewById(R.id.video_service)
    val title: TextView = itemView.findViewById(R.id.title)
    val viewsCount: TextView = itemView.findViewById(R.id.view_count)

    override fun bind(position: Int, itemDataHolder: AbsModel) {
        val video = itemDataHolder as Video
        viewsCount.text = video.views.toString()
        title.text = video.title
        videoLenght.text = AppTextUtils.getDurationString(video.duration)
        val photoUrl = video.image
        if (photoUrl.nonNullNoEmpty()) {
            PicassoInstance.with()
                .load(photoUrl)
                .tag(Constants.PICASSO_TAG)
                .into(image)
        } else {
            PicassoInstance.with().cancelRequest(image)
        }
        val serviceIcon = getIconByType(video.platform)
        if (serviceIcon != null) {
            videoService.visibility = View.VISIBLE
            videoService.setImageResource(serviceIcon)
        } else {
            videoService.visibility = View.GONE
        }
        card.setOnClickListener {
            getVideoPreviewPlace(
                Settings.get().accounts().current, video
            ).tryOpenWith(itemView.context)
        }
    }

    class Fabric : ViewHolderFabric {
        override fun create(view: View): IViewHolder {
            return VideoViewHolder(
                view
            )
        }

        override fun getLayout(): Int {
            return R.layout.item_fave_video
        }
    }
}
