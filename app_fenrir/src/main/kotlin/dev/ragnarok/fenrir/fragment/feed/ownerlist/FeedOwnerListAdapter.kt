package dev.ragnarok.fenrir.fragment.feed.ownerlist

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.model.entity.FeedOwnersEntity
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.toColor

class FeedOwnerListAdapter(private var data: List<FeedOwnersEntity>, private val context: Context) :
    RecyclerView.Adapter<FeedOwnerListAdapter.Holder>() {
    private var recyclerView: RecyclerView? = null
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(context).inflate(R.layout.item_feed_owner_list, parent, false)
        )
    }

    private fun fixNumerical(num: Int): String? {
        if (num < 0) {
            return null
        }
        val preLastDigit = num % 100 / 10
        if (preLastDigit == 1) {
            return context.getString(R.string.owner_count_c, num)
        }
        return when (num % 10) {
            1 -> context.getString(R.string.owner_count_a, num)
            2, 3, 4 -> context.getString(R.string.owner_count_b, num)
            else -> context.getString(R.string.owner_count_c, num)
        }
    }

    private fun createGradientImage(width: Int, height: Int, owner_id: Long): Bitmap {
        val color1: String
        val color2: String
        when (owner_id % 10) {
            1L -> {
                color1 = "#cfe1b9"
                color2 = "#718355"
            }

            2L -> {
                color1 = "#e3d0d8"
                color2 = "#c6d2ed"
            }

            3L -> {
                color1 = "#38a3a5"
                color2 = "#80ed99"
            }

            4L -> {
                color1 = "#9400D6"
                color2 = "#D6008E"
            }

            5L -> {
                color1 = "#cd8fff"
                color2 = "#9100ff"
            }

            6L -> {
                color1 = "#ff7f69"
                color2 = "#fe0bdb"
            }

            7L -> {
                color1 = "#07beb8"
                color2 = "#c4fff9"
            }

            8L -> {
                color1 = "#3a7ca5"
                color2 = "#d9dcd6"
            }

            9L -> {
                color1 = "#004e64"
                color2 = "#7ae582"
            }

            else -> {
                color1 = "#f5efff"
                color2 = "#adadff"
            }
        }
        val bitmap: Bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val gradient = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            color1.toColor(),
            color2.toColor(),
            Shader.TileMode.CLAMP
        )
        val canvas = Canvas(bitmap)
        val paint2 = Paint(Paint.ANTI_ALIAS_FLAG)
        paint2.shader = gradient
        val pth = (width + height).toFloat() / 2
        canvas.drawRoundRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            pth * 0.35f,
            pth * 0.35f,
            paint2
        )
        return bitmap
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = data[position]
        if (item.title.isNullOrEmpty()) holder.tvTitle.visibility = View.GONE else {
            holder.tvTitle.visibility = View.VISIBLE
            holder.tvTitle.text = item.title
        }
        holder.tvCount.text = fixNumerical(item.ownersIds?.size.orZero())
        holder.itemView.setOnClickListener {
            clickListener?.onFeedOwnerListClick(item)
        }

        if (item.title.nonNullNoEmpty()) {
            var name: String = item.title ?: ""
            if (name.length > 2) name = name.substring(0, 2)
            name = name.trim()
            holder.tvBackgroundText.text = name
        } else {
            holder.tvBackgroundText.visibility = View.GONE
        }
        holder.tvBackgroundImage.setImageBitmap(
            createGradientImage(
                200,
                200,
                item.id
            )
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<FeedOwnersEntity>) {
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
        fun onFeedOwnerListClick(owner: FeedOwnersEntity)
        fun onFeedOwnerListDelete(index: Int, owner: FeedOwnersEntity)
        fun onFeedOwnerListRename(index: Int, owner: FeedOwnersEntity)
    }

    inner class Holder(root: View) : RecyclerView.ViewHolder(root), OnCreateContextMenuListener {
        val tvTitle: TextView = root.findViewById(R.id.item_feed_owner_list_title)
        val tvCount: TextView = root.findViewById(R.id.item_feed_owner_list_count)
        val tvBackgroundText: TextView =
            root.findViewById(R.id.item_feed_owner_list_background_text)
        val tvBackgroundImage: ImageView = root.findViewById(R.id.item_feed_owner_list_background)

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
            val position = recyclerView?.getChildAdapterPosition(v).orZero()
            val owner = data[position]
            menu.setHeaderTitle(owner.title)
            menu.add(0, v.id, 0, R.string.rename).setOnMenuItemClickListener {
                clickListener?.onFeedOwnerListRename(position, owner)
                true
            }
            menu.add(0, v.id, 0, R.string.delete).setOnMenuItemClickListener {
                clickListener?.onFeedOwnerListDelete(position, owner)
                true
            }
        }

        init {
            itemView.setOnCreateContextMenuListener(this)
        }
    }
}
