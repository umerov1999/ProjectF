package dev.ragnarok.fenrir.fragment.poll.createpoll

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.radiobutton.MaterialRadioButton
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.view.PollGradientDrawable

class HorizontalPollBackgroundAdapter(
    private var data: List<Poll.PollBackground>,
    private val context: Context,
    private var selected: Int
) :
    RecyclerView.Adapter<HorizontalPollBackgroundAdapter.Holder>() {
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(
                context
            ).inflate(R.layout.item_poll_background, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val background = data[position]
        holder.ivSelected.isChecked = selected == position
        if (background.name == "default") {
            holder.ivSelected.setText(R.string.empty_background)
        } else {
            holder.ivSelected.text = background.name
        }
        holder.ivPollGradient.setImageDrawable(
            if (background.points == null) null else PollGradientDrawable(
                background
            )
        )

        holder.ivSelected.setOnClickListener {
            val oldChecked = selected
            selected = holder.bindingAdapterPosition
            if (oldChecked != selected) {
                clickListener?.onSelect(selected)
                notifyItemChanged(oldChecked)
                notifyItemChanged(selected)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<Poll.PollBackground>, selected: Int) {
        this.data = data
        this.selected = selected
        notifyDataSetChanged()
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onSelect(position: Int)
    }

    class Holder(root: View) : RecyclerView.ViewHolder(root) {
        val ivPollGradient: ImageView = itemView.findViewById(R.id.item_background_poll_gradient)
        val ivSelected: MaterialRadioButton =
            itemView.findViewById(R.id.item_background_poll_selected)
    }
}
