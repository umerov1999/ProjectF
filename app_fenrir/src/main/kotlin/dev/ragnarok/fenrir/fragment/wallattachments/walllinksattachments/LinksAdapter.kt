package dev.ragnarok.fenrir.fragment.wallattachments.walllinksattachments

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.model.Link
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import java.util.EventListener

class LinksAdapter(data: MutableList<Link>) :
    RecyclerBindableAdapter<Link, LinksAdapter.LinkViewHolder>(data) {
    private var mActionListener: ActionListener? = null
    private var linkConversationListener: LinkConversationListener? = null
    fun setActionListener(listener: ActionListener?) {
        mActionListener = listener
    }

    fun setLinkConversationListener(linkConversationListener: LinkConversationListener?) {
        this.linkConversationListener = linkConversationListener
    }

    private fun getImageUrl(link: Link): String? {
        if (link.photo == null && link.previewPhoto != null) return link.previewPhoto
        if (link.photo != null && link.photo?.sizes != null) {
            val sizes = link.photo?.sizes
            return sizes?.getUrlForSize(Settings.get().main().prefPreviewImageSize, true)
        }
        return null
    }

    override fun onBindItemViewHolder(viewHolder: LinkViewHolder, position: Int, type: Int) {
        val item = getItem(position)
        if (item.title.isNullOrEmpty()) viewHolder.tvTitle.visibility = View.GONE else {
            viewHolder.tvTitle.visibility = View.VISIBLE
            viewHolder.tvTitle.text = item.title
        }
        if (item.description.isNullOrEmpty()) viewHolder.tvDescription.visibility = View.GONE else {
            viewHolder.tvDescription.visibility = View.VISIBLE
            viewHolder.tvDescription.text = item.description
        }
        if (item.url.isNullOrEmpty()) viewHolder.tvURL.visibility = View.GONE else {
            viewHolder.tvURL.visibility = View.VISIBLE
            viewHolder.tvURL.text = item.url
        }
        val imageUrl = getImageUrl(item)
        if (imageUrl != null) {
            viewHolder.ivEmpty.visibility = View.GONE
            viewHolder.ivImage.visibility = View.VISIBLE
            displayAvatar(viewHolder.ivImage, null, imageUrl, Constants.PICASSO_TAG)
        } else {
            with().cancelRequest(viewHolder.ivImage)
            viewHolder.ivImage.visibility = View.GONE
            viewHolder.ivEmpty.visibility = View.VISIBLE
        }
        viewHolder.itemView.setOnClickListener {
            mActionListener?.onLinkClick(viewHolder.bindingAdapterPosition, item)
        }
        viewHolder.itemView.setOnLongClickListener {
            linkConversationListener ?: return@setOnLongClickListener false
            linkConversationListener?.onGoLinkConversation(item)
            true
        }
    }

    override fun viewHolder(view: View, type: Int): LinkViewHolder {
        return LinkViewHolder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_document_list
    }

    interface ActionListener : EventListener {
        fun onLinkClick(index: Int, doc: Link)
    }

    interface LinkConversationListener {
        fun onGoLinkConversation(doc: Link)
    }

    class LinkViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val ivImage: ImageView = root.findViewById(R.id.item_document_image)
        val ivEmpty: ImageView = root.findViewById(R.id.item_document_empty)
        val tvTitle: TextView = root.findViewById(R.id.item_document_title)
        val tvDescription: TextView = root.findViewById(R.id.item_document_description)
        val tvURL: TextView = root.findViewById(R.id.item_document_url)
    }
}