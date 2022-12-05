package dev.ragnarok.fenrir.fragment.fave.favelinks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.FaveLink
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast

class FaveLinksAdapter(private var data: List<FaveLink>, private val context: Context) :
    RecyclerView.Adapter<FaveLinksAdapter.Holder>() {
    private var recyclerView: RecyclerView? = null
    private var clickListener: ClickListener? = null
    private fun getImageUrl(link: FaveLink): String? {
        return link.photo?.sizes?.getUrlForSize(Settings.get().main().prefPreviewImageSize, true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_fave_link, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = data[position]
        if (item.title.isNullOrEmpty()) holder.tvTitle.visibility = View.GONE else {
            holder.tvTitle.visibility = View.VISIBLE
            holder.tvTitle.text = item.title
        }
        if (item.description.isNullOrEmpty()) holder.tvDescription.visibility = View.GONE else {
            holder.tvDescription.visibility = View.VISIBLE
            holder.tvDescription.text = item.description
        }
        if (item.url.isNullOrEmpty()) holder.tvURL.visibility = View.GONE else {
            holder.tvURL.visibility = View.VISIBLE
            holder.tvURL.text = item.url
        }
        val imageUrl = getImageUrl(item)
        if (imageUrl != null) {
            holder.ivEmpty.visibility = View.GONE
            holder.ivImage.visibility = View.VISIBLE
            displayAvatar(holder.ivImage, null, imageUrl, Constants.PICASSO_TAG)
        } else {
            with().cancelRequest(holder.ivImage)
            holder.ivImage.visibility = View.GONE
            holder.ivEmpty.visibility = View.VISIBLE
        }
        holder.itemView.setOnClickListener {
            clickListener?.onLinkClick(holder.bindingAdapterPosition, item)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<FaveLink>) {
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
        fun onLinkClick(index: Int, link: FaveLink)
        fun onLinkDelete(index: Int, link: FaveLink)
    }

    inner class Holder(root: View) : RecyclerView.ViewHolder(root), OnCreateContextMenuListener {
        val ivImage: ImageView
        val ivEmpty: ImageView
        val tvTitle: TextView
        val tvDescription: TextView
        val tvURL: TextView
        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
            val position = recyclerView?.getChildAdapterPosition(v) ?: 0
            val faveLink = data[position]
            menu.setHeaderTitle(faveLink.title)
            menu.add(0, v.id, 0, R.string.delete).setOnMenuItemClickListener {
                clickListener?.onLinkDelete(position, faveLink)
                true
            }
            menu.add(0, v.id, 0, R.string.copy_url).setOnMenuItemClickListener {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                val clip = ClipData.newPlainText("response", faveLink.url)
                clipboard?.setPrimaryClip(clip)
                createCustomToast(context).showToast(R.string.copied)
                true
            }
        }

        init {
            itemView.setOnCreateContextMenuListener(this)
            ivImage = root.findViewById(R.id.item_fave_link_image)
            ivEmpty = root.findViewById(R.id.item_fave_link_empty)
            tvTitle = root.findViewById(R.id.item_fave_link_title)
            tvDescription = root.findViewById(R.id.item_fave_link_description)
            tvURL = root.findViewById(R.id.item_fave_link_url)
        }
    }
}