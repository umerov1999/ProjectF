package dev.ragnarok.fenrir.fragment.base.horizontal

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.model.CommunityDetails
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class HorizontalMenuAdapter(data: MutableList<CommunityDetails.Menu>) :
    RecyclerBindableAdapter<CommunityDetails.Menu, HorizontalMenuAdapter.Holder>(data) {
    private var listener: Listener? = null
    override fun onBindItemViewHolder(viewHolder: Holder, position: Int, type: Int) {
        val item = getItem(position)
        viewHolder.title.text = item.title
        if (item.cover != null) {
            viewHolder.menu_image.visibility = View.VISIBLE
            displayAvatar(viewHolder.menu_image, null, item.cover, Constants.PICASSO_TAG)
        } else viewHolder.menu_image.visibility = View.INVISIBLE
        viewHolder.itemView.setOnClickListener {
            listener?.onWallMenuClick(
                item,
                getItemRawPosition(viewHolder.bindingAdapterPosition)
            )
        }
        viewHolder.itemView.setOnLongClickListener {
            listener?.onWallMenuLongClick(
                item,
                getItemRawPosition(viewHolder.bindingAdapterPosition)
            )
            true
        }
    }

    override fun viewHolder(view: View, type: Int): Holder {
        return Holder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_wall_menu
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    interface Listener {
        fun onWallMenuClick(item: CommunityDetails.Menu, pos: Int)
        fun onWallMenuLongClick(item: CommunityDetails.Menu, pos: Int)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val menu_image: ShapeableImageView = itemView.findViewById(R.id.item_menu_pic)
        val title: TextView = itemView.findViewById(R.id.item_menu_title)
    }
}
