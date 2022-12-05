package dev.ragnarok.fenrir.fragment.communitycontrol.communitylinks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme

class CommunityLinksAdapter(private var links: List<VKApiCommunity.Link>) :
    RecyclerView.Adapter<CommunityLinksAdapter.Holder>() {
    private var actionListener: ActionListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_community_link, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val link = links[position]
        holder.title.text = link.name
        holder.subtitle.text = link.desc
        holder.itemView.setOnClickListener {
            actionListener?.onClick(link)
        }
        holder.itemView.setOnLongClickListener {
            actionListener?.onLongClick(link)
            true
        }
        val photoUrl = link.photo_50
        if (photoUrl.nonNullNoEmpty()) {
            holder.icon.visibility = View.VISIBLE
            with()
                .load(photoUrl)
                .resize(50, 50)
                .centerCrop()
                .transform(CurrentTheme.createTransformationForAvatar())
                .into(holder.icon)
        } else {
            with()
                .cancelRequest(holder.icon)
            holder.icon.visibility = View.GONE
        }
    }

    fun setActionListener(actionListener: ActionListener?) {
        this.actionListener = actionListener
    }

    override fun getItemCount(): Int {
        return links.size
    }

    fun setData(data: List<VKApiCommunity.Link>) {
        links = data
        notifyDataSetChanged()
    }

    interface ActionListener {
        fun onClick(link: VKApiCommunity.Link)
        fun onLongClick(link: VKApiCommunity.Link)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        val icon: ImageView = itemView.findViewById(R.id.icon)
    }
}