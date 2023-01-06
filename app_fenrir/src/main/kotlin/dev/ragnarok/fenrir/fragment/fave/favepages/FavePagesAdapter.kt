package dev.ragnarok.fenrir.fragment.fave.favepages

import android.content.Context
import android.graphics.Color
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SelectionUtils.addSelectionProfileSupport
import dev.ragnarok.fenrir.model.FavePage
import dev.ragnarok.fenrir.model.FavePageType
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.CurrentTheme.getColorPrimary
import dev.ragnarok.fenrir.settings.CurrentTheme.getColorSecondary
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon
import dev.ragnarok.fenrir.view.AspectRatioImageView
import dev.ragnarok.fenrir.view.OnlineView
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView

class FavePagesAdapter(private var data: List<FavePage>, private val context: Context) :
    RecyclerView.Adapter<FavePagesAdapter.Holder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val storyTransformation: Transformation =
        CurrentTheme.createTransformationStrokeForAvatar()
    private var recyclerView: RecyclerView? = null
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_fave_page, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val favePage = data[position]
        if (Settings.get().other().isMention_fave) {
            holder.itemView.setOnLongClickListener {
                if (favePage.getObjectId() >= 0) {
                    favePage.owner?.let { it1 -> clickListener?.onMention(it1) }
                }
                true
            }
        }
        holder.description.text = favePage.description
        holder.name.text = favePage.owner?.fullName
        holder.name.setTextColor(
            Utils.getVerifiedColor(
                context,
                favePage.owner?.isVerified == true
            )
        )
        holder.ivVerified.visibility =
            if (favePage.owner?.isVerified == true) View.VISIBLE else View.GONE

        if (Settings.get().other()
                .isOwnerInChangesMonitor(favePage.owner?.ownerId ?: favePage.getObjectId())
        ) {
            holder.ivMonitor.visibility = View.VISIBLE
            holder.ivMonitor.fromRes(
                dev.ragnarok.fenrir_common.R.raw.eye,
                Utils.dp(48f),
                Utils.dp(48f),
                intArrayOf(
                    0x333333,
                    Color.parseColor("#ffffff"),
                    0x777777,
                    Color.parseColor("#000000")
                )
            )
            holder.ivMonitor.playAnimation()
        } else {
            holder.ivMonitor.visibility = View.GONE
            holder.ivMonitor.clearAnimationDrawable()
        }
        if (favePage.type == FavePageType.USER) {
            val user = favePage.user
            displayAvatar(
                holder.avatar,
                if (user?.hasUnseenStories == true) storyTransformation else transformation,
                favePage.owner?.maxSquareAvatar,
                Constants.PICASSO_TAG,
                monochrome = user?.blacklisted == true
            )
            holder.ivOnline.visibility = View.VISIBLE
            if (user?.blacklisted == true) {
                holder.blacklisted.visibility = View.VISIBLE
                if (Utils.hasMarshmallow() && FenrirNative.isNativeLoaded) {
                    holder.blacklisted.fromRes(
                        dev.ragnarok.fenrir_common.R.raw.skull,
                        Utils.dp(48f),
                        Utils.dp(48f),
                        null
                    )
                    holder.blacklisted.playAnimation()
                } else {
                    holder.blacklisted.setImageResource(R.drawable.audio_died)
                    holder.blacklisted.setColorFilter(Color.parseColor("#AAFF0000"))
                }
            } else if (user?.isFriend == true && Utils.hasMarshmallow() && FenrirNative.isNativeLoaded) {
                holder.blacklisted.visibility = View.VISIBLE
                holder.blacklisted.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.infinity,
                    Utils.dp(48f),
                    Utils.dp(48f),
                    intArrayOf(
                        0x333333,
                        getColorPrimary(context),
                        0x777777,
                        getColorSecondary(context)
                    )
                )
                holder.blacklisted.playAnimation()
            } else {
                holder.blacklisted.visibility = View.GONE
                holder.blacklisted.clearAnimationDrawable()
            }

            val onlineIcon = getOnlineIcon(
                true,
                user?.isOnlineMobile == true,
                user?.platform.orZero(),
                user?.onlineApp.orZero()
            )
            if (user?.isOnline == false) holder.ivOnline.setCircleColor(
                CurrentTheme.getColorFromAttrs(
                    R.attr.icon_color_inactive,
                    context,
                    "#000000"
                )
            ) else holder.ivOnline.setCircleColor(
                CurrentTheme.getColorFromAttrs(
                    R.attr.icon_color_active,
                    context,
                    "#000000"
                )
            )
            if (onlineIcon != null) {
                holder.ivOnline.setIcon(onlineIcon)
            }
        } else {
            val group = favePage.group
            displayAvatar(
                holder.avatar,
                if (group?.hasUnseenStories == true) storyTransformation else transformation,
                favePage.owner?.maxSquareAvatar,
                Constants.PICASSO_TAG
            )
            holder.name.setTextColor(Utils.getVerifiedColor(context, false))
            holder.ivOnline.visibility = View.GONE
            if (group?.isBlacklisted == true) {
                holder.blacklisted.visibility = View.VISIBLE
                if (Utils.hasMarshmallow() && FenrirNative.isNativeLoaded) {
                    holder.blacklisted.fromRes(
                        dev.ragnarok.fenrir_common.R.raw.skull,
                        Utils.dp(48f),
                        Utils.dp(48f),
                        null
                    )
                    holder.blacklisted.playAnimation()
                } else {
                    holder.blacklisted.setImageResource(R.drawable.audio_died)
                    holder.blacklisted.setColorFilter(Color.parseColor("#AAFF0000"))
                }
            } else {
                holder.blacklisted.visibility = View.GONE
                holder.blacklisted.clearAnimationDrawable()
            }
        }
        addSelectionProfileSupport(context, holder.avatar_root, favePage)
        holder.itemView.setOnClickListener {
            favePage.owner?.let { it1 ->
                clickListener?.onPageClick(
                    holder.bindingAdapterPosition,
                    it1
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<FavePage>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onPageClick(index: Int, owner: Owner)
        fun onDelete(index: Int, owner: Owner)
        fun onPushFirst(index: Int, owner: Owner)
        fun onMention(owner: Owner)
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView),
        OnCreateContextMenuListener {
        val avatar_root: ViewGroup
        val avatar: AspectRatioImageView
        val blacklisted: RLottieImageView
        val name: TextView
        val description: TextView
        val ivOnline: OnlineView
        val ivVerified: ImageView
        val ivMonitor: RLottieImageView
        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
            val position = recyclerView?.getChildAdapterPosition(v) ?: 0
            val favePage = data[position]
            menu.setHeaderTitle(favePage.owner?.fullName)
            menu.add(0, v.id, 0, R.string.delete).setOnMenuItemClickListener {
                favePage.owner?.let { it1 -> clickListener?.onDelete(position, it1) }
                true
            }
            menu.add(0, v.id, 0, R.string.push_first)
                .setOnMenuItemClickListener {
                    favePage.owner?.let { it1 -> clickListener?.onPushFirst(position, it1) }
                    true
                }
        }

        init {
            if (!Settings.get().other().isMention_fave) {
                itemView.setOnCreateContextMenuListener(this)
            }
            ivOnline = itemView.findViewById(R.id.header_navi_menu_online)
            avatar = itemView.findViewById(R.id.avatar)
            name = itemView.findViewById(R.id.name)
            description = itemView.findViewById(R.id.description)
            blacklisted = itemView.findViewById(R.id.item_blacklisted)
            ivVerified = itemView.findViewById(R.id.item_verified)
            avatar_root = itemView.findViewById(R.id.avatar_root)
            ivMonitor = itemView.findViewById(R.id.item_monitor)
        }
    }

}