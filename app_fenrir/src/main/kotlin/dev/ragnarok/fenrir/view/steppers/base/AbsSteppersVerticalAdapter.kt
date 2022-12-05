package dev.ragnarok.fenrir.view.steppers.base

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.fragment.base.holder.SharedHolders
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils

abstract class AbsSteppersVerticalAdapter<H : AbsStepsHost<*>>(
    private val mHost: H,
    actionListener: BaseHolderListener
) : RecyclerView.Adapter<AbsStepHolder<H>>() {
    private val mSharedHolders: SharedHolders<AbsStepHolder<H>> = SharedHolders(false)
    private val mActionListener: BaseHolderListener = actionListener
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbsStepHolder<H> {
        return createHolderForStep(parent, mHost, viewType)
    }

    private fun findHolderByStepIndex(step: Int): AbsStepHolder<H>? {
        return mSharedHolders.findOneByEntityId(step)
    }

    abstract fun createHolderForStep(parent: ViewGroup, host: H, step: Int): AbsStepHolder<H>

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AbsStepHolder<H>, position: Int) {
        mSharedHolders.put(position, holder)
        holder.counterText.text = (position + 1).toString()
        val isCurrent = mHost.currentStep == position
        val isLast = position == itemCount - 1
        val isActive = position <= mHost.currentStep
        val activeColor = CurrentTheme.getColorPrimary(holder.itemView.context)
        val inactiveColor = Color.parseColor("#2cb1b1b1")
        val tintColor = if (isActive) activeColor else inactiveColor
        holder.counterRoot.isEnabled = isCurrent
        Utils.setBackgroundTint(holder.counterRoot, tintColor)
        holder.contentRoot.visibility = if (isCurrent) View.VISIBLE else View.GONE
        holder.line.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
        holder.buttonNext.setText(mHost.getNextButtonText(position))
        holder.buttonNext.setOnClickListener { mActionListener.onNextButtonClick(holder.bindingAdapterPosition) }
        holder.buttonCancel.setText(mHost.getCancelButtonText(position))
        holder.buttonCancel.setOnClickListener {
            mActionListener.onCancelButtonClick(
                holder.bindingAdapterPosition
            )
        }
        holder.titleText.setText(mHost.getStepTitle(position))
        holder.titleText.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        holder.bindInternalStepViews(mHost)
        holder.setNextButtonAvailable(mHost.canMoveNext(position))
        val px16dp = Utils.dpToPx(16f, holder.itemView.context)
            .toInt()
        holder.itemView.setPadding(0, 0, 0, if (position == itemCount - 1) px16dp else 0)
    }

    fun updateNextButtonAvailability(step: Int) {
        findHolderByStepIndex(step)?.setNextButtonAvailable(mHost.canMoveNext(step))
    }

    override fun getItemCount(): Int {
        return mHost.stepsCount
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

}