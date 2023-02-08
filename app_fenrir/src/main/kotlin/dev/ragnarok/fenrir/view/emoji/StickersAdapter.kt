package dev.ragnarok.fenrir.view.emoji

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.StickerSet
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.emoji.EmojiconsPopup.OnStickerClickedListener
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView

class StickersAdapter(private val context: Context, private val stickers: StickerSet) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val isNightSticker: Boolean =
        Settings.get().ui().isStickers_by_theme && Settings.get().ui().isDarkModeEnabled(
            context
        )
    private var stickerClickedListener: OnStickerClickedListener? = null

    //    @Override
    //    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
    //        holder.setIsRecyclable(false);
    //        super.onViewAttachedToWindow(holder);
    //    }
    //
    //    @Override
    //    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
    //        holder.setIsRecyclable(true);
    //        super.onViewDetachedFromWindow(holder);
    //    }
    fun setStickerClickedListener(listener: OnStickerClickedListener?) {
        stickerClickedListener = listener
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
        //return stickers.getStickers().get(position).isAnimated() ? TYPE_ANIMATED : TYPE_IMAGE;
        return TYPE_IMAGE
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = stickers.getStickers()?.get(position)
        when (getItemViewType(position)) {
            TYPE_ANIMATED -> {
                val animatedHolder = holder as StickerAnimatedHolder
                animatedHolder.animation.fromNet(
                    item?.getAnimationByType(if (isNightSticker) "dark" else "light"),
                    Utils.createOkHttp(Constants.GIF_TIMEOUT, true),
                    Utils.dp(128f),
                    Utils.dp(128f)
                )
                animatedHolder.root.setOnClickListener {
                    if (item != null) {
                        stickerClickedListener?.onStickerClick(
                            item
                        )
                    }
                }
                animatedHolder.root.setOnLongClickListener {
                    animatedHolder.animation.playAnimation()
                    true
                }
            }

            TYPE_IMAGE -> {
                val normalHolder = holder as StickerHolder
                normalHolder.image.visibility = View.VISIBLE
                val url = item?.getImage(256, isNightSticker)?.url
                if (url.isNullOrEmpty()) {
                    with().cancelRequest(normalHolder.image)
                    normalHolder.image.setImageResource(R.drawable.ic_avatar_unknown)
                } else {
                    with()
                        .load(url) //.networkPolicy(NetworkPolicy.OFFLINE)
                        .tag(Constants.PICASSO_TAG)
                        .into(normalHolder.image)
                    normalHolder.root.setOnClickListener {
                        stickerClickedListener?.onStickerClick(
                            item
                        )
                    }
                }
            }

            else -> {
                val animatedHolder = holder as StickerAnimatedHolder
                animatedHolder.animation.fromNet(
                    item?.getAnimationByType(if (isNightSticker) "dark" else "light"),
                    Utils.createOkHttp(Constants.GIF_TIMEOUT, true),
                    Utils.dp(128f),
                    Utils.dp(128f)
                )
                animatedHolder.root.setOnClickListener {
                    if (item != null) {
                        stickerClickedListener?.onStickerClick(
                            item
                        )
                    }
                }
                animatedHolder.root.setOnLongClickListener {
                    animatedHolder.animation.playAnimation()
                    true
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is StickerAnimatedHolder) {
            holder.animation.clearAnimationDrawable()
        }
    }

    override fun getItemCount(): Int {
        return stickers.getStickers()?.size.orZero()
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