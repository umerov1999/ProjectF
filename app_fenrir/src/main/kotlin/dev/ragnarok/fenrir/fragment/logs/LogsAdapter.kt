package dev.ragnarok.fenrir.fragment.logs

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.model.LogEventWrapper
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils

class LogsAdapter(data: MutableList<LogEventWrapper>, private val actionListener: ActionListener) :
    RecyclerBindableAdapter<LogEventWrapper, LogsAdapter.Holder>(data) {
    override fun onBindItemViewHolder(viewHolder: Holder, position: Int, type: Int) {
        val wrapper = getItem(position)
        val event = wrapper.getEvent()
        viewHolder.body.text = event?.body
        viewHolder.tag.text = event?.tag
        val unixtime = event?.date.orZero() / 1000
        viewHolder.datetime.text = AppTextUtils.getDateFromUnixTime(unixtime)
        viewHolder.buttonShare.setOnClickListener { actionListener.onShareClick(wrapper) }
        viewHolder.buttonCopy.setOnClickListener { actionListener.onCopyClick(wrapper) }
        viewHolder.bodyRoot.setOnClickListener {
            if (!canReduce(event?.body)) {
                return@setOnClickListener
            }
            wrapper.setExpanded(!wrapper.isExpanded())
            notifyItemChanged(position + headersCount)
        }
        setupBodyRoot(viewHolder, wrapper)
    }

    private fun canReduce(body: String?): Boolean {
        return Utils.safeLenghtOf(body) > MAX_BODY_LENGTH
    }

    private fun setupBodyRoot(holder: Holder, wrapper: LogEventWrapper) {
        val body = wrapper.getEvent()?.body
        val canReduce = canReduce(body)
        if (!canReduce || wrapper.isExpanded()) {
            holder.buttonExpand.visibility = View.GONE
            holder.body.text = body
        } else {
            holder.buttonExpand.visibility = View.VISIBLE
            holder.body.text = AppTextUtils.reduceText(body, MAX_BODY_LENGTH)
        }
    }

    override fun viewHolder(view: View, type: Int): Holder {
        return Holder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_log
    }

    interface ActionListener {
        fun onShareClick(wrapper: LogEventWrapper)
        fun onCopyClick(wrapper: LogEventWrapper)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tag: TextView = itemView.findViewById(R.id.log_tag)
        val datetime: TextView = itemView.findViewById(R.id.log_datetime)
        val body: TextView = itemView.findViewById(R.id.log_body)
        val buttonShare: View = itemView.findViewById(R.id.log_button_share)
        val buttonCopy: View = itemView.findViewById(R.id.log_button_copy)
        val bodyRoot: View = itemView.findViewById(R.id.log_body_root)
        val buttonExpand: View = itemView.findViewById(R.id.log_button_expand)
    }

    companion object {
        private const val MAX_BODY_LENGTH = 400
    }
}