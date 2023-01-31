package dev.ragnarok.filegallery.fragment.tagowner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.model.tags.TagOwner
import dev.ragnarok.filegallery.nonNullNoEmpty


class TagOwnerAdapter(private var data: List<TagOwner>, private val context: Context) :
    RecyclerView.Adapter<TagOwnerAdapter.Holder>() {
    private var recyclerView: RecyclerView? = null
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_tag_owner, parent, false))
    }

    private fun fixNumerical(num: Int): String? {
        if (num < 0) {
            return null
        }
        val preLastDigit = num % 100 / 10
        if (preLastDigit == 1) {
            return context.getString(R.string.tag_count_c, num)
        }
        return when (num % 10) {
            1 -> context.getString(R.string.tag_count_a, num)
            2, 3, 4 -> context.getString(R.string.tag_count_b, num)
            else -> context.getString(R.string.tag_count_c, num)
        }
    }

    private fun createGradientImage(width: Int, height: Int, owner_id: Int): Bitmap {
        val color1: String
        val color2: String
        when (owner_id % 10) {
            1 -> {
                color1 = "#cfe1b9"
                color2 = "#718355"
            }

            2 -> {
                color1 = "#e3d0d8"
                color2 = "#c6d2ed"
            }

            3 -> {
                color1 = "#38a3a5"
                color2 = "#80ed99"
            }

            4 -> {
                color1 = "#9400D6"
                color2 = "#D6008E"
            }

            5 -> {
                color1 = "#cd8fff"
                color2 = "#9100ff"
            }

            6 -> {
                color1 = "#ff7f69"
                color2 = "#fe0bdb"
            }

            7 -> {
                color1 = "#07beb8"
                color2 = "#c4fff9"
            }

            8 -> {
                color1 = "#3a7ca5"
                color2 = "#d9dcd6"
            }

            9 -> {
                color1 = "#004e64"
                color2 = "#7ae582"
            }

            else -> {
                color1 = "#f5efff"
                color2 = "#adadff"
            }
        }
        val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val gradient = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            Color.parseColor(color1),
            Color.parseColor(color2),
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
        if (item.name.isNullOrEmpty()) holder.tvTitle.visibility = View.GONE else {
            holder.tvTitle.visibility = View.VISIBLE
            holder.tvTitle.text = item.name
        }
        holder.tvPath.text = fixNumerical(item.count)
        holder.itemView.setOnClickListener {
            clickListener?.onTagOwnerClick(holder.bindingAdapterPosition, item)
        }

        if (item.name.nonNullNoEmpty()) {
            var name: String = item.name ?: ""
            if (name.length > 2) name = name.substring(0, 2)
            name = name.trim { it <= ' ' }
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

    fun setData(data: List<TagOwner>) {
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
        fun onTagOwnerClick(index: Int, owner: TagOwner)
        fun onTagOwnerDelete(index: Int, owner: TagOwner)
        fun onTagOwnerRename(index: Int, owner: TagOwner)
    }

    inner class Holder(root: View) : RecyclerView.ViewHolder(root), OnCreateContextMenuListener {
        val tvTitle: TextView = root.findViewById(R.id.item_tag_owner_title)
        val tvPath: TextView = root.findViewById(R.id.item_tag_owner_path)
        val tvBackgroundText: TextView = root.findViewById(R.id.tag_background_text)
        val tvBackgroundImage: ImageView = root.findViewById(R.id.tag_background)

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
            val position = recyclerView?.getChildAdapterPosition(v) ?: 0
            val owner = data[position]
            menu.setHeaderTitle(owner.name)
            menu.add(0, v.id, 0, R.string.rename).setOnMenuItemClickListener {
                clickListener?.onTagOwnerRename(position, owner)
                true
            }
            menu.add(0, v.id, 0, R.string.delete).setOnMenuItemClickListener {
                clickListener?.onTagOwnerDelete(position, owner)
                true
            }
        }

        init {
            itemView.setOnCreateContextMenuListener(this)
        }
    }
}