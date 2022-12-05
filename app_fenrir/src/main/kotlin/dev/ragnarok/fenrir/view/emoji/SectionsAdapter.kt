package dev.ragnarok.fenrir.view.emoji

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme

class SectionsAdapter(private val data: List<AbsSection>, private val mContext: Context) :
    RecyclerView.Adapter<SectionsAdapter.Holder>() {
    private var listener: Listener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(mContext).inflate(R.layout.emoji_section_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val section = data[position]
        when (section.type) {
            AbsSection.TYPE_EMOJI -> {
                val emojiSection = section as EmojiSection
                with()
                    .cancelRequest(holder.icon)
                holder.icon.setImageDrawable(emojiSection.drawable)
                holder.icon.drawable.setTint(CurrentTheme.getColorOnSurface(mContext))
            }
            AbsSection.TYPE_STICKER -> {
                val stickerSection = section as StickerSection
                if (stickerSection.stickerSet.getTitle() != null && stickerSection.stickerSet.getTitle() == "recent") {
                    holder.icon.setImageResource(R.drawable.pin)
                    holder.icon.drawable.setTint(CurrentTheme.getColorPrimary(mContext))
                } else {
                    with()
                        .load(stickerSection.stickerSet.getImageUrl(128))
                        .placeholder(R.drawable.sticker_pack_with_alpha)
                        .into(holder.icon)
                    holder.icon.colorFilter = null
                }
            }
            AbsSection.TYPE_PHOTO_ALBUM -> {
                with()
                    .cancelRequest(holder.icon)
                holder.icon.setImageResource(R.drawable.image)
                holder.icon.drawable.setTint(CurrentTheme.getColorOnSurface(mContext))
            }
        }
        if (section.active) {
            holder.root.setBackgroundResource(R.drawable.circle_back_white)
            holder.root.background.setTint(CurrentTheme.getMessageBackgroundSquare(mContext))
        } else {
            holder.root.background = null
        }
        holder.itemView.setOnClickListener {
            listener?.onClick(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    interface Listener {
        fun onClick(position: Int)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.findViewById(R.id.root)
        val icon: ImageView = itemView.findViewById(R.id.icon)

    }
}