package dev.ragnarok.fenrir.view.steppers.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.holder.IdentificableHolder

abstract class AbsStepHolder<T : AbsStepsHost<*>>(
    parent: ViewGroup,
    internalLayoutRes: Int,
    val index: Int
) : RecyclerView.ViewHolder(
    createVerticalMainHolderView(parent)
), IdentificableHolder {
    val counterRoot: View = itemView.findViewById(R.id.counter_root)
    val counterText: TextView = itemView.findViewById(R.id.counter)
    val titleText: TextView = itemView.findViewById(R.id.title)
    val line: View = itemView.findViewById(R.id.step_line)
    val contentRoot: View = itemView.findViewById(R.id.content_root)
    val content: ViewGroup = itemView.findViewById(R.id.content)
    val buttonNext: MaterialButton = itemView.findViewById(R.id.buttonNext)
    val buttonCancel: MaterialButton = itemView.findViewById(R.id.buttonCancel)
    private val mContentView: View =
        LayoutInflater.from(itemView.context).inflate(internalLayoutRes, parent, false)

    override val holderId: Int = itemView.tag as Int

    abstract fun initInternalView(contentView: View)
    fun bindInternalStepViews(host: T) {
        bindViews(host)
    }

    protected abstract fun bindViews(host: T)
    fun setNextButtonAvailable(enable: Boolean) {
        buttonNext.isEnabled = enable
    }

    companion object {
        private var nextHolderId = 0
        internal fun createVerticalMainHolderView(parent: ViewGroup): View {
            val itemView =
                LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false)
            itemView.tag = generateHolderId()
            return itemView
        }

        private fun generateHolderId(): Int {
            nextHolderId++
            return nextHolderId
        }
    }

    init {
        content.addView(mContentView)
        this.initInternalView(mContentView)
    }
}