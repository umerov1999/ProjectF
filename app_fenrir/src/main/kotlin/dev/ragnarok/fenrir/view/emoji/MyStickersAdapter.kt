package dev.ragnarok.fenrir.view.emoji

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup.OnMyStickerClickedListener
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import java.io.File

class MyStickersAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var myStickerClickedListener: OnMyStickerClickedListener? = null
    fun setMyStickerClickedListener(listener: OnMyStickerClickedListener?) {
        myStickerClickedListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_IMAGE -> return StickerHolder(
                LayoutInflater.from(
                    context
                ).inflate(R.layout.sticker_grid_item, parent, false)
            )

            TYPE_ANIMATED -> return StickerAnimatedHolder(
                LayoutInflater.from(
                    context
                ).inflate(R.layout.sticker_grid_item_animated, parent, false)
            )
        }
        throw UnsupportedOperationException()
    }

    override fun getItemViewType(position: Int): Int {
        return if (Utils.getCachedMyStickers()[position].isAnimated) TYPE_ANIMATED else TYPE_IMAGE
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = Utils.getCachedMyStickers()[position]
        when (getItemViewType(position)) {
            TYPE_ANIMATED -> {
                val animatedHolder = holder as StickerAnimatedHolder
                animatedHolder.animation.fromFile(File(item.path), Utils.dp(128f), Utils.dp(128f))
                animatedHolder.root.setOnClickListener {
                    myStickerClickedListener?.onMyStickerClick(
                        item
                    )
                }
                animatedHolder.root.setOnLongClickListener {
                    animatedHolder.animation.playAnimation()
                    true
                }
            }

            TYPE_IMAGE -> {
                val normalHolder = holder as StickerHolder
                normalHolder.image.visibility = View.VISIBLE
                val url = item.previewPath
                if (url.isEmpty()) {
                    with().cancelRequest(normalHolder.image)
                    normalHolder.image.setImageResource(R.drawable.ic_avatar_unknown)
                } else {
                    with()
                        .load(url) //.networkPolicy(NetworkPolicy.OFFLINE)
                        .tag(Constants.PICASSO_TAG)
                        .into(normalHolder.image)
                    normalHolder.root.setOnClickListener {
                        myStickerClickedListener?.onMyStickerClick(
                            item
                        )
                    }
                }
            }

            else -> {
                val animatedHolder = holder as StickerAnimatedHolder
                animatedHolder.animation.fromFile(File(item.path), Utils.dp(128f), Utils.dp(128f))
                animatedHolder.root.setOnClickListener {
                    myStickerClickedListener?.onMyStickerClick(
                        item
                    )
                }
                animatedHolder.root.setOnLongClickListener {
                    animatedHolder.animation.playAnimation()
                    true
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return Utils.getCachedMyStickers().size
    }

    internal class StickerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.rootView
        val image: ImageView = itemView.findViewById(R.id.sticker)
    }

    internal class StickerAnimatedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.rootView
        val animation: RLottieImageView = itemView.findViewById(R.id.sticker_animated)
    }

    companion object {
        const val TYPE_IMAGE = 0
        const val TYPE_ANIMATED = 1
    }
}