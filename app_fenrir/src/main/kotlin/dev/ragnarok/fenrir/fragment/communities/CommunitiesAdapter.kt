package dev.ragnarok.fenrir.fragment.communities

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SelectionUtils.addSelectionProfileSupport
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.fragment.base.multidata.MultyDataAdapter
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.DataWrapper
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class CommunitiesAdapter(
    private val context: Context,
    dataWrappers: List<DataWrapper<Community>>,
    @StringRes titles: Array<Int?>
) : MultyDataAdapter<Community, CommunitiesAdapter.Holder>(dataWrappers, titles) {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private var actionListener: ActionListener? = null
    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): Holder {
        return Holder(
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_community, viewGroup, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        get(position, INFO)
        val community = INFO.item ?: return
        holder.headerRoot.visibility =
            if (INFO.internalPosition == 0) if (INFO.sectionTitleRes == null) View.GONE else View.VISIBLE else View.GONE
        if (INFO.sectionTitleRes != null) {
            holder.headerTitle.setText(INFO.sectionTitleRes ?: return)
        }
        displayAvatar(
            holder.ivAvatar,
            transformation,
            community.maxSquareAvatar,
            Constants.PICASSO_TAG
        )
        holder.tvName.text = community.fullName
        holder.tvName.setTextColor(Utils.getVerifiedColor(context, community.isVerified))
        holder.ivVerified.visibility = if (community.isVerified) View.VISIBLE else View.GONE
        holder.subtitle.text = getCommunityType(context, community)
        addSelectionProfileSupport(context, holder.avatar_root, community)
        holder.contentRoot.setOnClickListener {
            actionListener?.onCommunityClick(community)
        }
        holder.contentRoot.setOnLongClickListener {
            actionListener?.onCommunityLongClick(community) == true
        }
    }

    fun setActionListener(actionListener: ActionListener?) {
        this.actionListener = actionListener
    }

    interface ActionListener {
        fun onCommunityClick(community: Community)
        fun onCommunityLongClick(community: Community): Boolean
    }

    class Holder(root: View) : RecyclerView.ViewHolder(root) {
        val avatar_root: ViewGroup = itemView.findViewById(R.id.avatar_root)
        val headerRoot: View = root.findViewById(R.id.header_root)
        val headerTitle: TextView = root.findViewById(R.id.header_title)
        val contentRoot: View = root.findViewById(R.id.content_root)
        val tvName: TextView = root.findViewById(R.id.name)
        val ivAvatar: ImageView = root.findViewById(R.id.avatar)
        val subtitle: TextView = root.findViewById(R.id.subtitle)
        val ivVerified: ImageView = itemView.findViewById(R.id.item_verified)
    }

    companion object {
        private val INFO = ItemInfo<Community>()


        fun getCommunityType(context: Context, community: Community): String {
            when (community.communityType) {
                VKApiCommunity.Type.EVENT -> return context.getString(
                    if (community.closed == VKApiCommunity.Status.OPEN) R.string.type_opened else R.string.type_closed,
                    context.getString(R.string.type_event)
                )
                VKApiCommunity.Type.PAGE -> return context.getString(R.string.type_page)
            }
            return context.getString(
                if (community.closed == VKApiCommunity.Status.OPEN) R.string.type_opened else R.string.type_closed,
                context.getString(R.string.type_community)
            )
        }
    }

}