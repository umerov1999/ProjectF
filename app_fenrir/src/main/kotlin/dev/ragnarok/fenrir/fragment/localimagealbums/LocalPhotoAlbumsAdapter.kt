package dev.ragnarok.fenrir.fragment.localimagealbums

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.MemoryPolicy
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.LocalImageAlbum
import dev.ragnarok.fenrir.picasso.Content_Local
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.buildUriForPicasso
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.Settings

class LocalPhotoAlbumsAdapter(
    context: Context,
    private var data: List<LocalImageAlbum>,
    @Content_Local private val type: Int
) : RecyclerView.Adapter<LocalPhotoAlbumsAdapter.Holder>() {
    private val isDark: Boolean = Settings.get().ui().isDarkModeEnabled(context)
    private var clickListener: ClickListener? = null
    fun setData(data: List<LocalImageAlbum>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.local_album_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val album = data[position]
        val uri = buildUriForPicasso(type, album.getCoverImageId())
        if (type == Content_Local.AUDIO) {
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
        } else {
            holder.title.text = album.getName()
            holder.subtitle.text =
                holder.itemView.context.getString(R.string.photos_count, album.getPhotoCount())
            with()
                .load(uri)
                .tag(PICASSO_TAG)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .placeholder(R.drawable.background_gray)
                .into(holder.image)
        }
        holder.itemView.setOnClickListener {
            clickListener?.onClick(album)
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
    }

    companion object {
        const val PICASSO_TAG = "LocalPhotoAlbumsAdapter.TAG"
    }

}