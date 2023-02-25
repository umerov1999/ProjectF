/*
 * Copyright (C) 2018 Max Rumpf alias Maxr1998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused", "TooManyFunctions")

package de.maxr1998.modernpreferences.helpers

import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentManager
import de.maxr1998.modernpreferences.Preference
import de.maxr1998.modernpreferences.PreferenceScreen
import de.maxr1998.modernpreferences.preferences.AccentButtonPreference
import de.maxr1998.modernpreferences.preferences.CategoryHeader
import de.maxr1998.modernpreferences.preferences.CheckBoxPreference
import de.maxr1998.modernpreferences.preferences.CollapsePreference
import de.maxr1998.modernpreferences.preferences.ColorPickPreference
import de.maxr1998.modernpreferences.preferences.CustomTextPreference
import de.maxr1998.modernpreferences.preferences.EditTextPreference
import de.maxr1998.modernpreferences.preferences.ExpandableTextPreference
import de.maxr1998.modernpreferences.preferences.ImagePreference
import de.maxr1998.modernpreferences.preferences.MultiLineEditTextPreference
import de.maxr1998.modernpreferences.preferences.SeekBarPreference
import de.maxr1998.modernpreferences.preferences.SeparatorSpaceTextPreference
import de.maxr1998.modernpreferences.preferences.SwitchPreference
import de.maxr1998.modernpreferences.preferences.TwoStatePreference
import de.maxr1998.modernpreferences.preferences.choice.MultiChoiceDialogPreference
import de.maxr1998.modernpreferences.preferences.choice.SelectionItem
import de.maxr1998.modernpreferences.preferences.choice.SingleChoiceDialogPreference

// PreferenceScreen DSL functions
inline fun screen(context: Context?, block: PreferenceScreen.Builder.() -> Unit): PreferenceScreen {
    return PreferenceScreen.Builder(context).apply(block).build()
}

inline fun PreferenceScreen.Builder.subScreen(
    key: String = "",
    block: PreferenceScreen.Builder.() -> Unit
): PreferenceScreen {
    return PreferenceScreen.Builder(this, key).apply(block).build().also(::addPreferenceItem)
}

// Preference DSL functions
inline fun PreferenceScreen.Appendable.categoryHeader(key: String, block: Preference.() -> Unit) {
    addPreferenceItem(CategoryHeader(key).apply(block))
}

inline fun PreferenceScreen.Appendable.pref(key: String, block: Preference.() -> Unit): Preference {
    return Preference(key).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.accentButtonPref(
    key: String,
    block: Preference.() -> Unit
): Preference {
    return AccentButtonPreference(key).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.switch(
    key: String,
    block: SwitchPreference.() -> Unit
): SwitchPreference {
    return SwitchPreference(key).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.colorPick(
    key: String,
    fragmentManager: FragmentManager,
    block: ColorPickPreference.() -> Unit
): ColorPickPreference {
    return ColorPickPreference(key, fragmentManager).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.checkBox(
    key: String,
    block: CheckBoxPreference.() -> Unit
): CheckBoxPreference {
    return CheckBoxPreference(key).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.image(
    key: String,
    block: ImagePreference.() -> Unit
): ImagePreference {
    return ImagePreference(key).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.seekBar(
    key: String,
    block: SeekBarPreference.() -> Unit
): SeekBarPreference {
    return SeekBarPreference(key).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.expandText(
    key: String,
    block: ExpandableTextPreference.() -> Unit
): ExpandableTextPreference {
    return ExpandableTextPreference(key).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.singleChoice(
    key: String,
    items: ArrayList<SelectionItem>,
    fragmentManager: FragmentManager,
    block: SingleChoiceDialogPreference.() -> Unit
): SingleChoiceDialogPreference {
    return SingleChoiceDialogPreference(key, items, fragmentManager).apply(block)
        .also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.multiChoice(
    key: String,
    items: ArrayList<SelectionItem>,
    fragmentManager: FragmentManager,
    block: MultiChoiceDialogPreference.() -> Unit
): MultiChoiceDialogPreference {
    return MultiChoiceDialogPreference(key, items, fragmentManager).apply(block)
        .also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.editText(
    key: String,
    fragmentManager: FragmentManager,
    block: EditTextPreference.() -> Unit
): EditTextPreference {
    return EditTextPreference(key, fragmentManager).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.separatorSpace(
    key: String,
    fragmentManager: FragmentManager,
    block: SeparatorSpaceTextPreference.() -> Unit
): SeparatorSpaceTextPreference {
    return SeparatorSpaceTextPreference(key, fragmentManager).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.multiLineText(
    key: String,
    fragmentManager: FragmentManager,
    block: MultiLineEditTextPreference.() -> Unit
): MultiLineEditTextPreference {
    return MultiLineEditTextPreference(key, fragmentManager).apply(block).also(::addPreferenceItem)
}

inline fun PreferenceScreen.Appendable.customText(
    key: String,
    fragmentManager: FragmentManager,
    block: CustomTextPreference.() -> Unit
): CustomTextPreference {
    return CustomTextPreference(key, fragmentManager).apply(block).also(::addPreferenceItem)
}

inline fun <reified T : Preference> PreferenceScreen.Appendable.custom(
    key: String,
    block: T.() -> Unit
): T {
    return T::class.java.getConstructor(String::class.java).newInstance(key).apply(block)
        .also(::addPreferenceItem)
}

inline fun PreferenceScreen.Builder.collapse(
    key: String = "advanced",
    block: CollapsePreference.() -> Unit
): CollapsePreference {
    return CollapsePreference(this, key).also(::addPreferenceItem).apply {
        block()
        clearContext()
    }
}

inline fun CollapsePreference.subScreen(
    key: String = "",
    block: PreferenceScreen.Builder.() -> Unit
) {
    addPreferenceItem(PreferenceScreen.Builder(this, key).apply(block).build())
}

// Listener helpers

/**
 * [Preference.OnClickListener] shorthand without parameters.
 * Callback return value determines whether the Preference changed/requires a rebind.
 */
inline fun Preference.onClick(crossinline callback: () -> Boolean) {
    clickListener = Preference.OnClickListener { _, _ -> callback() }
}

/**
 * [Preference.OnClickListener] shorthand without parameters.
 * Callback return value determines whether the Preference changed/requires a rebind.
 */
inline fun Preference.onLongClick(crossinline callback: () -> Boolean) {
    longClickListener = Preference.OnLongClickListener { _, _ -> callback() }
}

inline fun EditTextPreference.onTextChanged(crossinline callback: (CharSequence?) -> Unit) {
    textChangeAfterListener = EditTextPreference.OnTextChangeAfterListener { _, text ->
        callback(text)
    }
}

inline fun EditTextPreference.onTextBeforeChanged(crossinline callback: (CharSequence?) -> Boolean) {
    textChangeBeforeListener = EditTextPreference.OnTextChangeBeforeListener { _, text ->
        callback(text)
    }
}

inline fun SeparatorSpaceTextPreference.onSeparatorTextChanged(crossinline callback: (CharSequence?) -> Unit) {
    separatorTextChangeAfterListener =
        SeparatorSpaceTextPreference.OnSeparatorTextChangeAfterListener { _, text ->
            callback(text)
        }
}

inline fun SeparatorSpaceTextPreference.onSeparatorTextBeforeChanged(crossinline callback: (CharSequence?) -> Boolean) {
    separatorTextChangeBeforeListener =
        SeparatorSpaceTextPreference.OnSeparatorTextChangeBeforeListener { _, text ->
            callback(text)
        }
}

inline fun MultiLineEditTextPreference.onMultiLineTextChange(crossinline callback: (Set<String>?) -> Unit) {
    multiLineTextChangeAfterListener =
        MultiLineEditTextPreference.OnMultiLineChangeAfterListener { _, data ->
            callback(data)
        }
}

inline fun MultiLineEditTextPreference.onMultiLineTextBeforeChange(crossinline callback: (Set<String>?) -> Boolean) {
    multiLineTextChangeBeforeListener =
        MultiLineEditTextPreference.OnMultiLineChangeBeforeListener { _, data ->
            callback(data)
        }
}

inline fun CustomTextPreference.onCustomTextChanged(crossinline callback: (CharSequence?) -> Unit) {
    customTextChangeAfterListener =
        CustomTextPreference.OnCustomTextAfterChangeListener { _, text ->
            callback(text)
        }
}

inline fun CustomTextPreference.onCustomTextBeforeChanged(crossinline callback: (CharSequence?) -> Boolean) {
    customTextChangeBeforeListener =
        CustomTextPreference.OnCustomTextBeforeChangeListener { _, text ->
            callback(text)
        }
}

/**
 * [Preference.OnClickListener] shorthand without parameters that returns false by default,
 * meaning the Preference didn't get changed and doesn't require a rebind/redraw.
 */
inline fun Preference.defaultOnClick(crossinline callback: () -> Unit) {
    clickListener = Preference.OnClickListener { _, _ ->
        callback()
        false
    }
}

/**
 * [Preference.OnClickListener] shorthand that only passes the view of the clicked item and returns false by default,
 * meaning the Preference didn't get changed and doesn't require a rebind/redraw.
 */
inline fun Preference.onClickView(crossinline callback: (View) -> Unit) {
    clickListener = Preference.OnClickListener { _, holder ->
        callback(holder.itemView)
        false
    }
}

inline fun TwoStatePreference.onCheckedChange(crossinline callback: (Boolean) -> Unit) {
    checkedAfterChangeListener = TwoStatePreference.OnCheckedAfterChangeListener { _, _, checked ->
        callback(checked)
    }
}

/**
 * [TwoStatePreference.onCheckedBeforeChange] shorthand.
 * Supplies the changed state, return value determines whether that state should be persisted
 * to [SharedPreferences][android.content.SharedPreferences].
 */
inline fun TwoStatePreference.onCheckedBeforeChange(crossinline callback: (Boolean) -> Boolean) {
    checkedBeforeChangeListener =
        TwoStatePreference.OnCheckedBeforeChangeListener { _, _, checked ->
            callback(checked)
        }
}

inline fun SingleChoiceDialogPreference.onSelectionChange(crossinline callback: (String) -> Unit) {
    selectionAfterChangeListener =
        SingleChoiceDialogPreference.OnSelectionAfterChangeListener { _, selection ->
            callback(selection)
        }
}

/**
 * [SingleChoiceDialogPreference.onSelectionBeforeChange] shorthand.
 * Supplies the changed selection, return value determines whether that state should be persisted
 * to [SharedPreferences][android.content.SharedPreferences].
 */
inline fun SingleChoiceDialogPreference.onSelectionBeforeChange(crossinline callback: (String) -> Boolean) {
    selectionBeforeChangeListener =
        SingleChoiceDialogPreference.OnSelectionBeforeChangeListener { _, selection ->
            callback(selection)
        }
}

inline fun MultiChoiceDialogPreference.onSelectionChange(crossinline callback: (Set<String>) -> Unit) {
    selectionAfterChangeListener =
        MultiChoiceDialogPreference.OnSelectionAfterChangeListener { _, selection ->
            callback(selection)
        }
}

/**
 * [MultiChoiceDialogPreference.onSelectionBeforeChange] shorthand.
 * Supplies the changed selections, return value determines whether that state should be persisted
 * to [SharedPreferences][android.content.SharedPreferences].
 */
inline fun MultiChoiceDialogPreference.onSelectionBeforeChange(crossinline callback: (Set<String>) -> Boolean) {
    selectionBeforeChangeListener =
        MultiChoiceDialogPreference.OnSelectionBeforeChangeListener { _, selection ->
            callback(selection)
        }
}

inline fun ColorPickPreference.onColorChange(crossinline callback: (Int) -> Unit) {
    colorAfterChangeListener =
        ColorPickPreference.OnColorAfterChangeListener { _, selection ->
            callback(selection)
        }
}

inline fun ColorPickPreference.onColorBeforeChange(crossinline callback: (Int) -> Boolean) {
    colorBeforeChangeListener =
        ColorPickPreference.OnColorBeforeChangeListener { _, selection ->
            callback(selection)
        }
}

inline fun SeekBarPreference.onSeek(crossinline callback: (Int) -> Unit) {
    seekAfterListener =
        SeekBarPreference.OnSeekAfterListener { _, selection ->
            callback(selection)
        }
}

inline fun SeekBarPreference.onSeekBefore(crossinline callback: (Int) -> Boolean) {
    seekBeforeListener =
        SeekBarPreference.OnSeekBeforeListener { _, selection ->
            callback(selection)
        }
}