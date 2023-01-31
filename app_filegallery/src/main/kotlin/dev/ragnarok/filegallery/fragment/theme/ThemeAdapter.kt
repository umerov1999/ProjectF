package dev.ragnarok.filegallery.fragment.theme

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorPrimary
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorSecondary
import dev.ragnarok.filegallery.settings.CurrentTheme.getColorWhiteContrastFix
import dev.ragnarok.filegallery.settings.Settings.get
import dev.ragnarok.filegallery.settings.theme.ThemeValue
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.view.natives.rlottie.RLottieImageView

class ThemeAdapter(private var data: List<ThemeValue>, context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val isDark: Boolean
    private var clickListener: ClickListener? = null
    private var currentId: String
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_THEME -> return ThemeHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_theme, parent, false)
            )

            TYPE_SPECIAL -> return SpecialThemeHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_special_theme, parent, false)
            )
        }
        throw RuntimeException("ThemeAdapter.onCreateViewHolder")
    }

    override fun getItemViewType(position: Int): Int {
        return if (data[position].special) TYPE_SPECIAL else TYPE_THEME
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_THEME -> bindThemeHolder(holder as ThemeHolder, position)
            TYPE_SPECIAL -> bindSpecialHolder(holder as SpecialThemeHolder, position)
        }
    }

    private fun bindSpecialHolder(holder: SpecialThemeHolder, position: Int) {
        val category = data[position]
        holder.itemView.alpha = if (category.disabled) 0.55f else 1.0f
        val isSelected = currentId == category.id
        holder.title.text =
            if (category.disabled) holder.itemView.context.getString(R.string.not_available) else category.name
        if (category.name.nonNullNoEmpty()) {
            holder.special_title.visibility = View.VISIBLE
            var name = category.name
            if (name.length > 4) name = name.substring(0, 4)
            name = name.trim { it <= ' ' }
            holder.special_title.text = name
            holder.special_title.setTextColor(
                if (position % 2 == 0) getColorPrimary(holder.itemView.context) else getColorSecondary(
                    holder.itemView.context
                )
            )
        } else {
            holder.special_title.visibility = View.INVISIBLE
        }
        holder.selected.visibility = if (isSelected) View.VISIBLE else View.GONE
        if (Utils.hasMarshmallow() && FenrirNative.isNativeLoaded) {
            if (isSelected) {
                holder.selected.fromRes(
                    R.raw.theme_selected,
                    Utils.dp(120f),
                    Utils.dp(120f),
                    intArrayOf(
                        0x333333,
                        getColorWhiteContrastFix(holder.selected.context),
                        0x777777,
                        getColorPrimary(holder.selected.context),
                        0x999999,
                        getColorSecondary(holder.selected.context)
                    )
                )
                holder.selected.playAnimation()
            } else {
                holder.selected.clearAnimationDrawable()
            }
        } else {
            if (isSelected) {
                holder.selected.setImageResource(R.drawable.theme_select)
            }
        }
        holder.clicked.setOnClickListener {
            clickListener?.onClick(position, category)
        }
    }

    private fun bindThemeHolder(holder: ThemeHolder, position: Int) {
        val category = data[position]
        holder.itemView.alpha = if (category.disabled) 0.55f else 1.0f
        val isSelected = currentId == category.id
        holder.title.text =
            if (category.disabled) holder.itemView.context.getString(R.string.not_available) else category.name
        holder.primary.setBackgroundColor(if (isDark) category.colorNightPrimary else category.colorDayPrimary)
        holder.secondary.setBackgroundColor(if (isDark) category.colorNightSecondary else category.colorDaySecondary)
        holder.selected.visibility = if (isSelected) View.VISIBLE else View.GONE
        if (Utils.hasMarshmallow() && FenrirNative.isNativeLoaded) {
            if (isSelected) {
                holder.selected.fromRes(
                    R.raw.theme_selected,
                    Utils.dp(120f),
                    Utils.dp(120f),
                    intArrayOf(
                        0x333333,
                        getColorWhiteContrastFix(holder.selected.context),
                        0x777777,
                        getColorPrimary(holder.selected.context),
                        0x999999,
                        getColorSecondary(holder.selected.context)
                    )
                )
                holder.selected.playAnimation()
            } else {
                holder.selected.clearAnimationDrawable()
            }
        } else {
            if (isSelected) {
                holder.selected.setImageResource(R.drawable.theme_select)
            }
        }
        holder.clicked.setOnClickListener {
            clickListener?.onClick(position, category)
        }
        holder.gradient.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                if (isDark) category.colorNightPrimary else category.colorDayPrimary,
                if (isDark) category.colorNightSecondary else category.colorDaySecondary
            )
        )
    }

    fun updateCurrentId(id: String) {
        currentId = id
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: Array<ThemeValue>) {
        this.data = listOf(*data)
        notifyDataSetChanged()
    }

    interface ClickListener {
        fun onClick(index: Int, value: ThemeValue?)
    }

    internal class ThemeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val primary: ImageView = itemView.findViewById(R.id.theme_icon_primary)
        val secondary: ImageView = itemView.findViewById(R.id.theme_icon_secondary)
        val selected: RLottieImageView = itemView.findViewById(R.id.selected)
        val gradient: ImageView = itemView.findViewById(R.id.theme_icon_gradient)
        val clicked: ViewGroup = itemView.findViewById(R.id.theme_type)
        val title: TextView = itemView.findViewById(R.id.item_title)

    }

    internal class SpecialThemeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val selected: RLottieImageView = itemView.findViewById(R.id.selected)
        val clicked: ViewGroup = itemView.findViewById(R.id.theme_type)
        val title: TextView = itemView.findViewById(R.id.item_title)
        val special_title: TextView = itemView.findViewById(R.id.special_text)

    }

    companion object {
        private const val TYPE_THEME = 0
        private const val TYPE_SPECIAL = 1
    }

    init {
        currentId = get().main().getMainThemeKey()
        isDark = get().main().isDarkModeEnabled(context)
    }
}
