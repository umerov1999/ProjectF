package de.maxr1998.modernpreferences.preferences

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import de.maxr1998.modernpreferences.PreferencesAdapter
import de.maxr1998.modernpreferences.PreferencesExtra
import de.maxr1998.modernpreferences.R
import de.maxr1998.modernpreferences.helpers.DEFAULT_RES_ID
import de.maxr1998.modernpreferences.preferences.colorpicker.ColorCircleDrawable
import de.maxr1998.modernpreferences.preferences.colorpicker.ColorPickerView.WHEEL_TYPE
import de.maxr1998.modernpreferences.preferences.colorpicker.builder.ColorPickerClickListener
import de.maxr1998.modernpreferences.preferences.colorpicker.builder.ColorPickerDialogBuilder

class ColorPickPreference(key: String, fragmentManager: FragmentManager) :
    DialogPreference(key, fragmentManager) {
    override fun getWidgetLayoutResource() = R.layout.color_widget

    var alphaSlider = false
    var lightSlider = false
    private var border = false

    @ColorInt
    var selectedColor = 0
        private set

    private var wheelType: WHEEL_TYPE = WHEEL_TYPE.FLOWER
    var density = 0

    @ColorInt
    var defaultValue: Int = Color.WHITE
    var colorBeforeChangeListener: OnColorBeforeChangeListener? = null
    var colorAfterChangeListener: OnColorAfterChangeListener? = null

    private var colorIndicator: ImageView? = null

    fun copyColorPick(other: ColorPickPreference): ColorPickPreference {
        alphaSlider = other.alphaSlider
        lightSlider = other.lightSlider
        border = other.border
        wheelType = other.wheelType
        density = other.density
        colorAfterChangeListener = other.colorAfterChangeListener
        colorBeforeChangeListener = other.colorBeforeChangeListener
        return this
    }

    override fun onAttach() {
        super.onAttach()
        selectedColor = getInt(defaultValue)
    }

    override fun bindViews(holder: PreferencesAdapter.ViewHolder) {
        super.bindViews(holder)
        colorIndicator = (holder.widget as ImageView)
        val tmpColor =
            if (enabled) selectedColor else darken(selectedColor, .5f)

        val colorChoiceDrawable = ColorCircleDrawable(tmpColor)
        colorIndicator?.setImageDrawable(colorChoiceDrawable)
    }

    private fun darken(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(
            a,
            (r * factor).toInt().coerceAtLeast(0),
            (g * factor).toInt().coerceAtLeast(0),
            (b * factor).toInt().coerceAtLeast(0)
        )
    }

    fun persist(@ColorInt input: Int) {
        if (colorBeforeChangeListener?.onColorBeforeChange(this, input) != false) {
            selectedColor = input
            commitInt(input)
            requestRebind()
            colorAfterChangeListener?.onColorAfterChange(this, input)
        }
    }

    override fun createAndShowDialogFragment() {
        ColorPickDialog.newInstance(
            title,
            titleRes,
            alphaSlider,
            lightSlider,
            border,
            selectedColor,
            WHEEL_TYPE.toInt(wheelType),
            density,
            key,
            parent?.key
        ).show(fragmentManager, "ColorPickDialog")
    }

    class ColorPickDialog : DialogFragment() {
        companion object {
            fun newInstance(
                title: CharSequence?,
                @StringRes titleRes: Int,
                alphaSlider: Boolean,
                lightSlider: Boolean,
                border: Boolean,
                @ColorInt selectedColor: Int,
                wheelType: Int,
                density: Int,
                key: String,
                screenKey: String?
            ): ColorPickDialog {
                val args = Bundle()
                args.putInt(PreferencesExtra.TITLE_RES, titleRes)
                args.putCharSequence(PreferencesExtra.TITLE, title)

                args.putBoolean(PreferencesExtra.COLOR_ALPHA_SLIDER, alphaSlider)
                args.putBoolean(PreferencesExtra.COLOR_LIGHT_SLIDER, lightSlider)
                args.putBoolean(PreferencesExtra.COLOR_BORDER, border)
                args.putInt(PreferencesExtra.COLOR_WHEEL_TYPE, wheelType)
                args.putInt(PreferencesExtra.COLOR_DENSITY, density)
                args.putInt(PreferencesExtra.DEFAULT_VALUE, selectedColor)

                args.putString(PreferencesExtra.PREFERENCE_KEY, key)
                args.putString(PreferencesExtra.PREFERENCE_SCREEN_KEY, screenKey)
                val dialog = ColorPickDialog()
                dialog.arguments = args
                return dialog
            }
        }

        private var title: CharSequence? = null

        @StringRes
        private var titleRes: Int = DEFAULT_RES_ID

        private var alphaSlider = false
        private var lightSlider = false
        private var border = false

        @ColorInt
        private var selectedColor = 0

        private var wheelType: WHEEL_TYPE = WHEEL_TYPE.FLOWER
        private var density = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            titleRes = requireArguments().getInt(PreferencesExtra.TITLE_RES)
            title = requireArguments().getCharSequence(PreferencesExtra.TITLE)
            alphaSlider = requireArguments().getBoolean(PreferencesExtra.COLOR_ALPHA_SLIDER)
            lightSlider = requireArguments().getBoolean(PreferencesExtra.COLOR_LIGHT_SLIDER)
            border = requireArguments().getBoolean(PreferencesExtra.COLOR_BORDER)
            wheelType =
                WHEEL_TYPE.indexOf(requireArguments().getInt(PreferencesExtra.COLOR_WHEEL_TYPE))
            density = requireArguments().getInt(PreferencesExtra.COLOR_DENSITY)
            selectedColor = requireArguments().getInt(PreferencesExtra.DEFAULT_VALUE)
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = ColorPickerDialogBuilder
                .with(requireActivity())
                .initialColor(selectedColor)
                .showBorder(border)
                .wheelType(wheelType)
                .density(density)
                .showColorEdit(true)
                .setPositiveButton(
                    android.R.string.ok,
                    object : ColorPickerClickListener {
                        override fun onClick(
                            d: DialogInterface?,
                            lastSelectedColor: Int,
                            allColors: Array<Int?>?
                        ) {
                            val intent = Bundle()
                            intent.putInt(PreferencesExtra.RESULT_VALUE, lastSelectedColor)
                            intent.putString(
                                PreferencesExtra.PREFERENCE_KEY,
                                requireArguments().getString(PreferencesExtra.PREFERENCE_KEY)
                            )
                            intent.putString(
                                PreferencesExtra.PREFERENCE_SCREEN_KEY,
                                requireArguments().getString(PreferencesExtra.PREFERENCE_SCREEN_KEY)
                            )
                            parentFragmentManager.setFragmentResult(
                                PreferencesExtra.COLOR_DIALOG_REQUEST,
                                intent
                            )
                            dismiss()
                        }

                    })
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    dismiss()
                }
            if (titleRes != DEFAULT_RES_ID) builder.setTitle(titleRes) else builder.setTitle(title?.toString())

            if (!alphaSlider && !lightSlider) builder.noSliders() else if (!alphaSlider) builder.lightnessSliderOnly() else if (!lightSlider) builder.alphaSliderOnly()

            return builder.build()
        }
    }

    fun interface OnColorBeforeChangeListener {
        fun onColorBeforeChange(preference: ColorPickPreference, @ColorInt color: Int): Boolean
    }

    fun interface OnColorAfterChangeListener {
        fun onColorAfterChange(preference: ColorPickPreference, @ColorInt color: Int)
    }
}
