package dev.ragnarok.fenrir.fragment.search.filteredit

import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.fragment.search.options.*
import dev.ragnarok.fenrir.util.AppTextUtils

class SearchOptionsAdapter(items: MutableList<BaseOption>) :
    RecyclerBindableAdapter<BaseOption, RecyclerView.ViewHolder>(items) {
    private var mOptionClickListener: OptionClickListener? = null
    override fun onBindItemViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        position: Int,
        type: Int
    ) {
        val option = getItem(position)
        when (type) {
            TYPE_NORMAL -> {
                val normalHolder = viewHolder as NormalHolder
                if (option is SimpleNumberOption) {
                    bindSimpleNumberHolder(option, normalHolder)
                }
                if (option is SpinnerOption) {
                    bindSpinnerHolder(option, normalHolder)
                }
                if (option is SimpleTextOption) {
                    bindSimpleTextHolder(option, normalHolder)
                }
                if (option is DatabaseOption) {
                    bindDatabaseHolder(option, normalHolder)
                }
                if (option is SimpleGPSOption) {
                    bindSimpleGpsHolder(option, normalHolder)
                }
                if (option is SimpleDateOption) {
                    bindSimpleDateHolder(option, normalHolder)
                }
            }
            TYPE_BOOLEAN -> {
                val simpleBooleanHolder = viewHolder as SimpleBooleanHolder
                bindSimpleBooleanHolder(option as SimpleBooleanOption, simpleBooleanHolder)
            }
        }
    }

    private fun bindSimpleDateHolder(option: SimpleDateOption, holder: NormalHolder) {
        holder.title.setText(option.title)
        holder.value.text = if (option.timeUnix == 0L) null else AppTextUtils.getDateFromUnixTime(
            holder.itemView.context,
            option.timeUnix
        )
        holder.delete.visibility = if (option.timeUnix == 0L) View.INVISIBLE else View.VISIBLE
        holder.itemView.setOnClickListener {
            mOptionClickListener?.onDateOptionClick(option)
        }
        holder.delete.setOnClickListener {
            holder.value.text = null
            holder.delete.visibility = View.INVISIBLE
            option.timeUnix = 0
            mOptionClickListener?.onOptionCleared(option)
        }
    }

    private fun bindSimpleGpsHolder(option: SimpleGPSOption, holder: NormalHolder) {
        holder.title.setText(option.title)
        holder.value.text = option.simpleGPS()
        holder.delete.visibility = View.VISIBLE
        holder.itemView.setOnClickListener {
            if (mOptionClickListener != null) {
                mOptionClickListener?.onGPSOptionClick(option)
                holder.value.text = holder.value.context.getString(R.string.please_wait)
            }
        }
        holder.delete.setOnClickListener {
            holder.value.text = null
            holder.delete.visibility = View.INVISIBLE
            option.long_gps = 0.0
            option.lat_gps = 0.0
            mOptionClickListener?.onOptionCleared(option)
        }
    }

    private fun bindDatabaseHolder(option: DatabaseOption, holder: NormalHolder) {
        holder.title.setText(option.title)
        holder.value.text = if (option.value == null) null else (option.value ?: return).title
        holder.delete.visibility = if (option.value == null) View.INVISIBLE else View.VISIBLE
        holder.itemView.setOnClickListener {
            mOptionClickListener?.onDatabaseOptionClick(option)
        }
        holder.delete.setOnClickListener {
            holder.value.text = null
            holder.delete.visibility = View.INVISIBLE
            option.value = null
            mOptionClickListener?.onOptionCleared(option)
        }
    }

    private fun bindSimpleBooleanHolder(option: SimpleBooleanOption, holder: SimpleBooleanHolder) {
        holder.checkableView.setText(option.title)
        holder.checkableView.setOnCheckedChangeListener(null)
        holder.checkableView.isChecked = option.checked
        holder.checkableView.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            option.checked = isChecked
            mOptionClickListener?.onSimpleBooleanOptionChanged(option)
        }
    }

    private fun bindSpinnerHolder(option: SpinnerOption, holder: NormalHolder) {
        if (option.value == null) {
            holder.value.text = null
        } else {
            holder.value.setText((option.value ?: return).name)
        }
        holder.delete.visibility = if (option.value == null) View.INVISIBLE else View.VISIBLE
        holder.title.setText(option.title)
        holder.itemView.setOnClickListener {
            mOptionClickListener?.onSpinnerOptionClick(option)
        }
        holder.delete.setOnClickListener {
            holder.value.text = null
            holder.delete.visibility = View.INVISIBLE
            option.value = null
            mOptionClickListener?.onOptionCleared(option)
        }
    }

    private fun bindSimpleNumberHolder(option: SimpleNumberOption, holder: NormalHolder) {
        holder.value.text = if (option.value == null) null else option.value.toString()
        holder.title.setText(option.title)
        holder.delete.visibility = if (option.value == null) View.INVISIBLE else View.VISIBLE
        holder.itemView.setOnClickListener {
            mOptionClickListener?.onSimpleNumberOptionClick(option)
        }
        holder.delete.setOnClickListener {
            holder.value.text = null
            holder.delete.visibility = View.INVISIBLE
            option.value = null
            mOptionClickListener?.onOptionCleared(option)
        }
    }

    private fun bindSimpleTextHolder(option: SimpleTextOption, holder: NormalHolder) {
        holder.value.text = option.value
        holder.title.setText(option.title)
        holder.delete.visibility =
            if (option.value.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        holder.itemView.setOnClickListener {
            mOptionClickListener?.onSimpleTextOptionClick(option)
        }
        holder.delete.setOnClickListener {
            holder.value.text = null
            holder.delete.visibility = View.INVISIBLE
            option.value = null
            mOptionClickListener?.onOptionCleared(option)
        }
    }

    override fun viewHolder(view: View, type: Int): RecyclerView.ViewHolder {
        when (type) {
            TYPE_NORMAL -> return NormalHolder(view)
            TYPE_BOOLEAN -> return SimpleBooleanHolder(view)
        }
        throw UnsupportedOperationException()
    }

    override fun layoutId(type: Int): Int {
        when (type) {
            TYPE_NORMAL -> return R.layout.item_search_option_text
            TYPE_BOOLEAN -> return R.layout.item_search_option_checkbox
        }
        return 0
    }

    override fun getItemType(position: Int): Int {
        val option = getItem(position - headersCount)
        if (option is SimpleNumberOption
            || option is SimpleTextOption
            || option is SpinnerOption
            || option is DatabaseOption
            || option is SimpleGPSOption
            || option is SimpleDateOption
        ) {
            return TYPE_NORMAL
        }
        return if (option is SimpleBooleanOption) {
            TYPE_BOOLEAN
        } else -1
    }

    fun setOptionClickListener(optionClickListener: OptionClickListener?) {
        mOptionClickListener = optionClickListener
    }

    interface OptionClickListener {
        fun onSpinnerOptionClick(spinnerOption: SpinnerOption)
        fun onDatabaseOptionClick(databaseOption: DatabaseOption)
        fun onSimpleNumberOptionClick(option: SimpleNumberOption)
        fun onSimpleTextOptionClick(option: SimpleTextOption)
        fun onSimpleBooleanOptionChanged(option: SimpleBooleanOption)
        fun onOptionCleared(option: BaseOption)
        fun onGPSOptionClick(gpsOption: SimpleGPSOption)
        fun onDateOptionClick(dateOption: SimpleDateOption)
    }

    class NormalHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val value: TextView = itemView.findViewById(R.id.value)
        val delete: ImageView = itemView.findViewById(R.id.delete)
    }

    class SimpleBooleanHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val checkableView: MaterialSwitch = itemView.findViewById(R.id.search_option)
    }

    companion object {
        private const val TYPE_NORMAL = 0
        private const val TYPE_BOOLEAN = 1
    }
}