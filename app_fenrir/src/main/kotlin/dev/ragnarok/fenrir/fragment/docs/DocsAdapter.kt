package dev.ragnarok.fenrir.fragment.docs

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.util.AppTextUtils
import java.util.*

class DocsAdapter(data: MutableList<Document>) :
    RecyclerBindableAdapter<Document, DocsAdapter.DocViewHolder>(data) {
    private var mActionListener: ActionListener? = null
    fun setActionListener(listener: ActionListener?) {
        mActionListener = listener
    }

    override fun onBindItemViewHolder(viewHolder: DocViewHolder, position: Int, type: Int) {
        val item = getItem(position)
        val targetExt = item.ext?.uppercase(Locale.getDefault())
        viewHolder.tvExt.text = targetExt
        viewHolder.tvTitle.text = item.title
        viewHolder.tvSize.text = AppTextUtils.getSizeString(item.size)
        val previewUrl = item.getPreviewWithSize(PhotoSize.M, false)
        val withImage = previewUrl.nonNullNoEmpty()
        viewHolder.ivImage.visibility = if (withImage) View.VISIBLE else View.GONE
        viewHolder.ivImage.setBackgroundColor(Color.TRANSPARENT)
        if (withImage) {
            with()
                .load(previewUrl)
                .tag(Constants.PICASSO_TAG)
                .into(viewHolder.ivImage)
        }
        viewHolder.itemView.setOnClickListener {
            mActionListener?.onDocClick(
                getItemRawPosition(viewHolder.bindingAdapterPosition),
                item
            )
        }
        viewHolder.itemView.setOnLongClickListener {
            mActionListener?.onDocLongClick(
                getItemRawPosition(viewHolder.bindingAdapterPosition),
                item
            ) == true
        }
    }

    override fun viewHolder(view: View, type: Int): DocViewHolder {
        return DocViewHolder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_document_big
    }

    interface ActionListener : EventListener {
        fun onDocClick(index: Int, doc: Document)
        fun onDocLongClick(index: Int, doc: Document): Boolean
    }

    class DocViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val tvExt: TextView = root.findViewById(R.id.item_document_big_ext)
        val ivImage: ImageView = root.findViewById(R.id.item_document_big_image)
        val tvTitle: TextView = root.findViewById(R.id.item_document_big_title)
        val tvSize: TextView = root.findViewById(R.id.item_document_big_size)
    }
}