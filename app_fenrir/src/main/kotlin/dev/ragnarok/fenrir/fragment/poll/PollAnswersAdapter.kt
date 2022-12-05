package dev.ragnarok.fenrir.fragment.poll

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.model.Poll
import dev.ragnarok.fenrir.settings.CurrentTheme

class PollAnswersAdapter(private val context: Context, items: MutableList<Poll.Answer>) :
    RecyclerBindableAdapter<Poll.Answer, PollAnswersAdapter.ViewHolder>(items) {
    private var checkedIds: MutableSet<Long>
    private var checkable = false
    private var listener: OnAnswerChangedCallback? = null
    private var multiple = false
    override fun onBindItemViewHolder(viewHolder: ViewHolder, position: Int, type: Int) {
        val answer = getItem(position)
        viewHolder.tvTitle.text = answer.text
        viewHolder.rbButton.text = answer.text
        viewHolder.tvCount.text = answer.voteCount.toString()
        viewHolder.pbRate.progress = answer.rate.toInt()
        viewHolder.pbRate.progressTintList = ColorStateList.valueOf(
            CurrentTheme.getColorPrimary(
                context
            )
        )
        val isMyAnswer = checkedIds.contains(answer.id)
        viewHolder.rbButton.setOnCheckedChangeListener(null)
        viewHolder.rbButton.isChecked = isMyAnswer
        viewHolder.rbButton.setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
            changeChecked(
                answer.id, checked
            )
        }

        //holder.mVotedRoot.setVisibility(checkable ? View.GONE : View.VISIBLE);
        viewHolder.tvTitle.visibility = if (checkable) View.GONE else View.VISIBLE
        viewHolder.rbButton.visibility = if (checkable) View.VISIBLE else View.GONE

        viewHolder.pbRate.setOnClickListener {
            listener?.onAnswerClick(answer.id)
        }
    }

    override fun viewHolder(view: View, type: Int): ViewHolder {
        return ViewHolder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_poll_answer
    }

    private fun changeChecked(id: Long, isChecked: Boolean) {
        if (checkable) {
            if (isChecked) {
                if (!multiple) {
                    checkedIds.clear()
                }
                checkedIds.add(id)
            } else {
                checkedIds.remove(id)
            }
            listener?.onAnswerChanged(checkedIds)
            notifyDataSetChanged()
        }
    }

    fun setData(
        answers: MutableList<Poll.Answer>?,
        checkable: Boolean,
        multiple: Boolean,
        checkedIds: MutableSet<Long>
    ) {
        setItems(answers, false)
        this.checkable = checkable
        this.multiple = multiple
        this.checkedIds = checkedIds
        notifyDataSetChanged()
    }

    fun setListener(listener: OnAnswerChangedCallback?) {
        this.listener = listener
    }

    interface OnAnswerChangedCallback {
        fun onAnswerChanged(checked: MutableSet<Long>)
        fun onAnswerClick(id: Long)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCount: TextView = itemView.findViewById(R.id.item_poll_answer_count)
        val rbButton: MaterialCheckBox = itemView.findViewById(R.id.item_poll_answer_radio)
        val tvTitle: TextView = itemView.findViewById(R.id.item_poll_answer_title)
        val pbRate: ProgressBar = itemView.findViewById(R.id.item_poll_answer_progress)
    }

    init {
        checkedIds = HashSet()
    }
}