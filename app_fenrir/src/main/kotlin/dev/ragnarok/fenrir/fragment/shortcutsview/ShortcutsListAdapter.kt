package dev.ragnarok.fenrir.fragment.shortcutsview

import android.annotation.SuppressLint
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.ShortcutStored
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.view.AspectRatioImageView

class ShortcutsListAdapter(private var data: List<ShortcutStored>) :
    RecyclerView.Adapter<ShortcutsListAdapter.Holder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private var actionListener: ActionListener? = null
    private var recyclerView: RecyclerView? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): Holder {
        return Holder(
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_shortcut, viewGroup, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val shortcut = data[position]
        val userAvatarUrl = shortcut.cover
        if (userAvatarUrl.isEmpty()) {
            with()
                .load(R.drawable.ic_avatar_unknown)
                .transform(transformation)
                .into(holder.avatar)
        } else {
            with()
                .load(userAvatarUrl)
                .transform(transformation)
                .into(holder.avatar)
        }
        holder.name.text = shortcut.name
        holder.description.text = shortcut.action
        holder.itemView.setOnClickListener {
            actionListener?.onShortcutClick(shortcut)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<ShortcutStored>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setActionListener(actionListener: ActionListener?) {
        this.actionListener = actionListener
    }

    interface ActionListener {
        fun onShortcutClick(shortcutStored: ShortcutStored)
        fun onShortcutRemoved(pos: Int, shortcutStored: ShortcutStored)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnCreateContextMenuListener {
        val avatar: AspectRatioImageView
        val name: TextView
        val description: TextView

        init {
            itemView.setOnCreateContextMenuListener(this)
            avatar = itemView.findViewById(R.id.avatar)
            name = itemView.findViewById(R.id.name)
            description = itemView.findViewById(R.id.description)
        }

        override fun onCreateContextMenu(
            menu: ContextMenu,
            v: View,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            val position = recyclerView?.getChildAdapterPosition(v) ?: 0
            val page = data[position]
            menu.setHeaderTitle(page.name)
            menu.add(0, v.id, 0, R.string.delete).setOnMenuItemClickListener {
                actionListener?.onShortcutRemoved(position, page)
                true
            }
        }
    }

}