package dev.ragnarok.fenrir.fragment.accounts

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Account
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.UserInfoResolveUtil
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon
import dev.ragnarok.fenrir.view.OnlineView
import dev.ragnarok.fenrir.view.natives.rlottie.RLottieImageView
import kotlin.math.abs

class AccountAdapter(
    private val context: Context,
    private val data: List<Account>,
    private val callback: Callback
) : RecyclerView.Adapter<AccountAdapter.AccountHolder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountHolder {
        return AccountHolder(
            LayoutInflater.from(
                context
            ).inflate(R.layout.item_account, parent, false)
        )
    }

    fun getByPosition(position: Int): Account {
        return data[position]
    }

    fun checkPosition(position: Int): Boolean {
        return position >= 0 && data.size > position
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AccountHolder, position: Int) {
        val account = getByPosition(position)
        val owner = account.owner
        if (owner == null) {
            holder.firstName.text = account.getObjectId().toString()
            displayAvatar(holder.avatar, transformation, null, Constants.PICASSO_TAG)
        } else {
            holder.firstName.text = owner.fullName
            displayAvatar(
                holder.avatar,
                transformation,
                owner.maxSquareAvatar,
                Constants.PICASSO_TAG
            )
        }
        if (owner is User) {
            val online = owner.isOnline
            if (owner.domain.nonNullNoEmpty()) {
                holder.domain.text = "@" + owner.domain
            } else {
                holder.domain.text = "@id" + account.getObjectId()
            }
            holder.tvLastTime.visibility = View.VISIBLE
            holder.tvLastTime.text = UserInfoResolveUtil.getUserActivityLine(context, owner, false)
            holder.tvLastTime.setTextColor(if (owner.isOnline) CurrentTheme.getColorPrimary(context) else STATUS_COLOR_OFFLINE)
            val iconRes = getOnlineIcon(
                online,
                owner.isOnlineMobile,
                owner.platform,
                owner.onlineApp
            )
            holder.vOnline.setIcon(iconRes ?: 0)
            holder.vOnline.visibility = if (online) View.VISIBLE else View.GONE
        } else {
            holder.domain.text = "club" + abs(account.getObjectId())
            holder.tvLastTime.visibility = View.GONE
            holder.vOnline.visibility = View.GONE
        }
        val isCurrent = account.getObjectId() == Settings.get()
            .accounts()
            .current
        holder.active.visibility = if (isCurrent) View.VISIBLE else View.INVISIBLE
        if (Utils.hasMarshmallow() && FenrirNative.isNativeLoaded) {
            if (isCurrent) {
                holder.active.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.select_check_box,
                    Utils.dp(40f),
                    Utils.dp(40f),
                    intArrayOf(
                        0x333333, CurrentTheme.getColorPrimary(
                            context
                        ), 0x777777, CurrentTheme.getColorSecondary(context)
                    )
                )
                holder.active.playAnimation()
            } else {
                holder.active.clearAnimationDrawable()
            }
        } else {
            if (isCurrent) {
                holder.active.setImageResource(R.drawable.check)
            }
        }
        holder.account.setOnClickListener { callback.onClick(account) }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    interface Callback {
        fun onClick(account: Account)
    }

    class AccountHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val firstName: TextView = itemView.findViewById(R.id.first_name)
        val domain: TextView = itemView.findViewById(R.id.domain)
        val vOnline: OnlineView = itemView.findViewById(R.id.item_user_online)
        val tvLastTime: TextView = itemView.findViewById(R.id.last_time)
        val avatar: ImageView = itemView.findViewById(R.id.avatar)
        val active: RLottieImageView = itemView.findViewById(R.id.active)
        val account: View = itemView.findViewById(R.id.account_select)
    }

    companion object {
        private val STATUS_COLOR_OFFLINE = Color.parseColor("#999999")
    }
}