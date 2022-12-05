package dev.ragnarok.fenrir.fragment.messages.chatusersdomain

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.ifNonNullNoEmpty
import dev.ragnarok.fenrir.model.AppChatUser
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UserPlatform
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon
import dev.ragnarok.fenrir.view.OnlineView
import dev.ragnarok.fenrir.view.WeakViewAnimatorAdapter

class ChatMembersListDomainAdapter(context: Context, private var data: List<AppChatUser>) :
    RecyclerView.Adapter<ChatMembersListDomainAdapter.ViewHolder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val paddingForFirstLast: Int = if (Utils.is600dp(context)) Utils.dpToPx(16f, context)
        .toInt() else 0
    private var actionListener: ActionListener? = null
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_chat_user_list_second, viewGroup, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        val user = item.getMember()
        var online = false
        var onlineMobile = false
        @UserPlatform var platform = UserPlatform.UNKNOWN
        var app = 0
        if (user is User) {
            online = user.isOnline
            onlineMobile = user.isOnlineMobile
            platform = user.platform
            app = user.onlineApp
        }
        val iconRes = getOnlineIcon(online, onlineMobile, platform, app)
        holder.vOnline.setIcon(iconRes ?: 0)
        holder.vOnline.visibility = if (online) View.VISIBLE else View.GONE
        val userAvatarUrl = user?.maxSquareAvatar
        if (userAvatarUrl.isNullOrEmpty()) {
            with()
                .load(R.drawable.ic_avatar_unknown)
                .transform(transformation)
                .into(holder.ivAvatar)
        } else {
            with()
                .load(userAvatarUrl)
                .transform(transformation)
                .into(holder.ivAvatar)
        }
        holder.tvName.text = user?.fullName
        user?.domain.ifNonNullNoEmpty({
            holder.tvDomain.text = "@$it"
        }, {
            holder.tvDomain.text = "@id" + user?.ownerId.orZero()
        })
        if (actionListener != null) {
            holder.itemView.setOnClickListener {
                actionListener?.onUserClick(item)
                holder.startSomeAnimation()
            }
            holder.itemView.setOnLongClickListener {
                actionListener?.onUserLongClick(
                    item
                ) == true
            }
        }
        val view = holder.itemView
        view.setPadding(
            view.paddingLeft,
            if (position == 0) paddingForFirstLast else 0,
            view.paddingRight,
            if (position == itemCount - 1) paddingForFirstLast else 0
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<AppChatUser>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setActionListener(actionListener: ActionListener?) {
        this.actionListener = actionListener
    }

    interface ActionListener {
        fun onUserClick(user: AppChatUser)
        fun onUserLongClick(user: AppChatUser): Boolean
    }

    class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val vOnline: OnlineView = root.findViewById(R.id.item_user_online)
        val ivAvatar: ImageView = root.findViewById(R.id.item_user_avatar)
        val tvName: TextView = root.findViewById(R.id.item_user_name)
        val tvDomain: TextView = root.findViewById(R.id.item_user_domain)
        val selectionView: MaterialCardView = root.findViewById(R.id.item_user_selection)
        val animationAdapter: Animator.AnimatorListener
        var animator: ObjectAnimator? = null
        fun startSomeAnimation() {
            selectionView.setCardBackgroundColor(CurrentTheme.getColorSecondary(selectionView.context))
            selectionView.alpha = 0.5f
            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f)
            animator?.duration = 500
            animator?.addListener(animationAdapter)
            animator?.start()
        }

        init {
            animationAdapter = object : WeakViewAnimatorAdapter<View>(selectionView) {
                override fun onAnimationEnd(view: View) {
                    view.visibility = View.GONE
                }

                override fun onAnimationStart(view: View) {
                    view.visibility = View.VISIBLE
                }

                override fun onAnimationCancel(view: View) {
                    view.visibility = View.GONE
                }
            }
        }
    }

}