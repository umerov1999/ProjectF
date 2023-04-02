package dev.ragnarok.fenrir.fragment.docs

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

class DocsAsImagesAdapter(data: MutableList<Document>) :
    RecyclerBindableAdapter<Document, DocsAsImagesAdapter.DocViewHolder>(data) {
    private var mActionListener: ActionListener? = null
    fun setData(data: MutableList<Document>) {
        setItems(data)
    }

    fun setActionListener(listener: ActionListener?) {
        mActionListener = listener
    }

    override fun onBindItemViewHolder(viewHolder: DocViewHolder, position: Int, type: Int) {
        val item = getItem(position)
        viewHolder.title.text = item.title
        val previewUrl = item.getPreviewWithSize(PhotoSize.Q, false)
        val withImage = previewUrl.nonNullNoEmpty()
        if (withImage) {
            with()
                .load(previewUrl)
                .tag(Constants.PICASSO_TAG)
                .into(viewHolder.image)
        } else {
            with()
                .cancelRequest(viewHolder.image)
        }
        viewHolder.itemView.setOnClickListener {
            mActionListener?.onDocClick(getItemRawPosition(viewHolder.bindingAdapterPosition), item)
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
        return R.layout.item_doc_as_image
    }

    interface ActionListener {
        fun onDocClick(index: Int, doc: Document)
        fun onDocLongClick(index: Int, doc: Document): Boolean
    }

    class DocViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val image: ImageView = root.findViewById(R.id.image)
        val title: TextView = root.findViewById(R.id.title)
    }
}