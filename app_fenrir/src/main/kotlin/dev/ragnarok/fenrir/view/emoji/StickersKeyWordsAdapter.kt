package dev.ragnarok.fenrir.view.emoji

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Callback
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Sticker
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup.OnStickerClickedListener
import java.lang.ref.WeakReference

class StickersKeyWordsAdapter(private val context: Context, private var stickers: List<Sticker>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val isNightSticker: Boolean =
        Settings.get().ui().isStickers_by_theme && Settings.get().ui().isDarkModeEnabled(
            context
        )
    private var stickerClickedListener: OnStickerClickedListener? = null
    fun setStickerClickedListener(listener: OnStickerClickedListener?) {
        stickerClickedListener = listener
    }

    fun setData(data: List<Sticker>?) {
        stickers = if (data.isNullOrEmpty()) {
            emptyList()
        } else {
            data
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return StickerHolder(
            LayoutInflater.from(
                context
            ).inflate(R.layout.sticker_keyword_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = stickers[position]
        val normalHolder = holder as StickerHolder
        normalHolder.root.visibility = View.VISIBLE
        val url = item.getImage(256, isNightSticker).url
        if (url.isNullOrEmpty()) {
            with().cancelRequest(normalHolder.root)
            normalHolder.root.setImageResource(R.drawable.ic_avatar_unknown)
        } else {
            with()
                .load(url) //.networkPolicy(NetworkPolicy.OFFLINE)
                .tag(Constants.PICASSO_TAG)
                .into(normalHolder.root, LoadOnErrorCallback(normalHolder.root, url))
            normalHolder.root.setOnClickListener {
                stickerClickedListener?.onStickerClick(
                    item
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return stickers.size
    }

    private class LoadOnErrorCallback(view: ImageView, private val link: String) : Callback {
        val ref: WeakReference<ImageView> = WeakReference(view)
        override fun onSuccess() {
            // do nothink
        }

        override fun onError(t: Throwable) {
            val view = ref.get()
            try {
                if (view != null) {
                    with()
                        .load(link)
                        .into(view)
                }
            } catch (ignored: Exception) {
            }
        }

    }

    internal class StickerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: ImageView = itemView.findViewById(R.id.sticker)
    }
}