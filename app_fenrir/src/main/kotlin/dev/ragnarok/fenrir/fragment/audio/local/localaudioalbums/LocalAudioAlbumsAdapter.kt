package dev.ragnarok.fenrir.fragment.audio.local.localaudioalbums

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.picasso.Content_Local
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.buildUriForPicasso
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView

class LocalAudioAlbumsAdapter(
    context: Context,
    private var data: List<LocalImageAlbum>
) : RecyclerView.Adapter<LocalAudioAlbumsAdapter.Holder>() {
    private val isDark: Boolean = Settings.get().ui().isDarkModeEnabled(context)
    private var clickListener: ClickListener? = null
    private var currentId: Int = -1
    fun setData(data: List<LocalImageAlbum>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun updateCurrentId(currentId: Int) {
        this.currentId = currentId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.local_audio_album_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val album = data[position]
        val uri = buildUriForPicasso(Content_Local.AUDIO, album.getCoverImageId())
        if (album.getId() != 0) {
            holder.title.text = album.getName()
            holder.subtitle.text =
                holder.itemView.context.getString(
                    R.string.local_audios_count,
                    album.getPhotoCount()
                )
            with()
                .load(uri)
                .tag(PICASSO_TAG)
                .placeholder(if (isDark) R.drawable.generic_audio_nowplaying_dark else R.drawable.generic_audio_nowplaying_light)
                .error(if (isDark) R.drawable.generic_audio_nowplaying_dark else R.drawable.generic_audio_nowplaying_light)
                .into(holder.image)
        } else {
            with().cancelRequest(holder.image)
            holder.image.setImageResource(if (isDark) R.drawable.generic_audio_nowplaying_dark else R.drawable.generic_audio_nowplaying_light)
            holder.title.setText(R.string.all_audios)
            holder.subtitle.text = ""
        }
        holder.itemView.setOnClickListener {
            clickListener?.onClick(album)
        }

        val isSelected = currentId == album.getId()
        holder.selected.visibility = if (isSelected) View.VISIBLE else View.GONE
        if (Utils.hasMarshmallow() && FenrirNative.isNativeLoaded) {
            if (isSelected) {
                holder.selected.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.theme_selected,
                    Utils.dp(120f),
                    Utils.dp(120f),
                    intArrayOf(
                        0x333333,
                        CurrentTheme.getColorWhiteContrastFix(holder.selected.context),
                        0x777777,
                        CurrentTheme.getColorPrimary(holder.selected.context),
                        0x999999,
                        CurrentTheme.getColorSecondary(holder.selected.context)
                    )
                )
                holder.selected.playAnimation()
            } else {
                holder.selected.clearAnimationDrawable()
            }
        } else {
            if (isSelected) {
                holder.selected.setImageResource(R.drawable.theme_select)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onClick(album: LocalImageAlbum)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.item_local_album_cover)
        val title: TextView = itemView.findViewById(R.id.item_local_album_name)
        val subtitle: TextView = itemView.findViewById(R.id.counter)
        val selected: RLottieImageView = itemView.findViewById(R.id.selected)
    }

    companion object {
        const val PICASSO_TAG = "LocalAudioAlbumsAdapter.TAG"
    }

}
