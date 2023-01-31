package dev.ragnarok.fenrir.fragment.base.horizontal

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import java.util.Calendar

class HorizontalStoryAdapter(data: MutableList<Story>) :
    RecyclerBindableAdapter<Story, HorizontalStoryAdapter.Holder>(data) {
    private var listener: Listener? = null
    override fun onBindItemViewHolder(viewHolder: Holder, position: Int, type: Int) {
        val item = getItem(position)
        val context = viewHolder.itemView.context
        viewHolder.name.text = item.owner?.fullName
        if (item.isEmptyStory()) {
            viewHolder.story_empty.visibility = View.VISIBLE
        } else {
            viewHolder.story_empty.visibility = View.GONE
        }
        if (item.expires <= 0) viewHolder.expires.visibility = View.INVISIBLE else {
            if (item.isIs_expired) {
                viewHolder.expires.visibility = View.VISIBLE
                viewHolder.expires.setText(R.string.is_expired)
            } else {
                val exp = (item.expires - Calendar.getInstance().time.time / 1000) / 3600
                if (exp <= 0) {
                    viewHolder.expires.visibility = View.INVISIBLE
                } else {
                    viewHolder.expires.visibility = View.VISIBLE
                    viewHolder.expires.text = context.getString(
                        R.string.expires, exp.toString(), context.getString(
                            Utils.declOfNum(
                                exp,
                                intArrayOf(R.string.hour, R.string.hour_sec, R.string.hours)
                            )
                        )
                    )
                }
            }
        }
        if (item.owner == null) {
            displayAvatar(
                viewHolder.story_image,
                RoundTransformation(),
                null,
                Constants.PICASSO_TAG
            )
        } else {
            displayAvatar(
                viewHolder.story_image,
                RoundTransformation(),
                item.owner?.maxSquareAvatar,
                Constants.PICASSO_TAG
            )
        }
        viewHolder.itemView.setOnClickListener { listener?.onOptionClick(item, position) }
    }

    override fun viewHolder(view: View, type: Int): Holder {
        return Holder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_story
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    interface Listener {
        fun onOptionClick(item: Story, pos: Int)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val story_image: ImageView = itemView.findViewById(R.id.item_story_pic)
        val story_empty: ImageView = itemView.findViewById(R.id.item_story_empty)
        val name: TextView = itemView.findViewById(R.id.item_story_name)
        val expires: TextView = itemView.findViewById(R.id.item_story_expires)
    }
}