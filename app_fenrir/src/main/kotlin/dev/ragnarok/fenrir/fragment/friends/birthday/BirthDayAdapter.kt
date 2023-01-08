package dev.ragnarok.fenrir.fragment.friends.birthday

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
import dev.ragnarok.fenrir.activity.SelectionUtils.addSelectionProfileSupport
import dev.ragnarok.fenrir.model.BirthDay
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon

class BirthDayAdapter(private val mContext: Context, private var mData: List<BirthDay>) :
    RecyclerView.Adapter<BirthDayAdapter.BirthDayHolder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val transformationWithStory: Transformation =
        CurrentTheme.createTransformationStrokeForAvatar()
    private var mClickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BirthDayHolder {
        return BirthDayHolder(
            LayoutInflater.from(mContext).inflate(R.layout.item_birth_day, parent, false)
        )
    }

    private fun needShowMonth(position: Int): Boolean {
        if (position <= 0) {
            return true
        }
        return mData[position].month != mData[position - 1].month
    }

    override fun onBindViewHolder(holder: BirthDayHolder, position: Int) {
        val bth = mData[position]
        val user = bth.user
        holder.name.text = user.fullName
        holder.subtitle.text = user.bdate
        holder.subtitle.setTextColor(if (user.isOnline) CurrentTheme.getColorPrimary(mContext) else STATUS_COLOR_OFFLINE)
        holder.name.setTextColor(Utils.getVerifiedColor(mContext, user.isVerified))
        holder.ivVerified.visibility = if (user.isVerified) View.VISIBLE else View.GONE
        holder.blacklisted.visibility = if (user.blacklisted) View.VISIBLE else View.GONE
        holder.online.visibility = if (user.isOnline) View.VISIBLE else View.GONE
        val onlineIcon =
            getOnlineIcon(user.isOnline, user.isOnlineMobile, user.platform, user.onlineApp)
        if (onlineIcon != null) {
            holder.online.setImageResource(onlineIcon)
        } else {
            holder.online.setImageDrawable(null)
        }
        val avaUrl = user.maxSquareAvatar
        displayAvatar(
            holder.avatar,
            if (user.hasUnseenStories) transformationWithStory else transformation,
            avaUrl,
            Constants.PICASSO_TAG,
            monochrome = user.blacklisted
        )
        holder.itemView.setOnClickListener {
            mClickListener?.onUserClick(user)
        }
        addSelectionProfileSupport(mContext, holder.avatarRoot, user)
        if (needShowMonth(position)) {
            holder.month.visibility = View.VISIBLE
            val opMonth = bth.month - 1
            if (opMonth in 0..11) {
                holder.month.text =
                    mContext.resources.getTextArray(R.array.array_month_items)[opMonth]
            } else {
                holder.month.text = bth.month.toString()
            }
        } else {
            holder.month.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setItems(data: List<BirthDay>) {
        mData = data
        notifyDataSetChanged()
    }

    fun setClickListener(clickListener: ClickListener?) {
        mClickListener = clickListener
    }

    interface ClickListener {
        fun onUserClick(user: User)
    }

    inner class BirthDayHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.item_people_name)
        val month: TextView = itemView.findViewById(R.id.header_month)
        val subtitle: TextView = itemView.findViewById(R.id.item_people_subtitle)
        val avatar: ImageView = itemView.findViewById(R.id.item_people_avatar)
        val online: ImageView = itemView.findViewById(R.id.item_people_online)
        val blacklisted: ImageView = itemView.findViewById(R.id.item_blacklisted)
        val ivVerified: ImageView = itemView.findViewById(R.id.item_verified)
        val avatarRoot: ViewGroup = itemView.findViewById(R.id.avatar_root)

        init {
            Utils.setColorFilter(online, CurrentTheme.getColorPrimary(mContext))
        }
    }

    companion object {
        private val STATUS_COLOR_OFFLINE = Color.parseColor("#999999")
    }

}
