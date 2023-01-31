package dev.ragnarok.fenrir.fragment.search.dialogssearch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.search.dialogssearch.DialogPreviewAdapter.DialogPreviewHolder
import dev.ragnarok.fenrir.ifNonNullNoEmpty
import dev.ragnarok.fenrir.model.Conversation
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import java.util.EventListener

class DialogPreviewAdapter(
    private var mData: List<Conversation>,
    private val actionListener: ActionListener
) :
    RecyclerView.Adapter<DialogPreviewHolder>() {
    private val mTransformation: Transformation = CurrentTheme.createTransformationForAvatar()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialogPreviewHolder {
        return DialogPreviewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dialog_preview, parent, false)
        )
    }

    override fun onBindViewHolder(holder: DialogPreviewHolder, position: Int) {
        val item = mData[position]
        holder.mTitle.text = item.getDisplayTitle(holder.mTitle.context)
        val url = item.imageUrl
        if (url.nonNullNoEmpty()) {
            holder.EmptyAvatar.visibility = View.INVISIBLE
            displayAvatar(
                holder.mAvatar,
                mTransformation,
                item.imageUrl,
                Constants.PICASSO_TAG
            )
        } else {
            with().cancelRequest(holder.mAvatar)
            item.getDisplayTitle(holder.mAvatar.context).ifNonNullNoEmpty({
                holder.EmptyAvatar.visibility = View.VISIBLE
                var name = it
                if (name.length > 2) name = name.substring(0, 2)
                name = name.trim { it1 -> it1 <= ' ' }
                holder.EmptyAvatar.text = name
            }, {
                holder.EmptyAvatar.visibility = View.INVISIBLE
            })
            holder.mAvatar.setImageBitmap(
                mTransformation.localTransform(
                    Utils.createGradientChatImage(
                        200,
                        200,
                        item.getId()
                    )
                )
            )
        }
        holder.itemView.setOnClickListener { actionListener.onEntryClick(item) }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setData(data: List<Conversation>) {
        mData = data
        notifyDataSetChanged()
    }

    interface ActionListener : EventListener {
        fun onEntryClick(o: Conversation)
    }

    class DialogPreviewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mAvatar: ImageView = itemView.findViewById(R.id.item_chat_avatar)
        val mTitle: TextView = itemView.findViewById(R.id.dialog_title)
        val EmptyAvatar: TextView = itemView.findViewById(R.id.empty_avatar_text)
    }

}