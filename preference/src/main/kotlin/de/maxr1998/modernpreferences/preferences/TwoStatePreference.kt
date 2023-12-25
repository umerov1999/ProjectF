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

package de.maxr1998.modernpreferences.preferences

import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.widget.CompoundButton
import androidx.annotation.StringRes
import de.maxr1998.modernpreferences.PreferencesAdapter
import de.maxr1998.modernpreferences.helpers.DISABLED_RESOURCE_ID

@Suppress("MemberVisibilityCanBePrivate")
abstract class TwoStatePreference(key: String) : StatefulPreference(key) {
    private var checkedInternal = false
    var checked: Boolean
        get() = checkedInternal
        set(value) {
            checkNotNull(parent) {
                "Setting the checked value before the preference was attached isn't supported. Consider using `defaultValue` instead."
            }
            if (value != checkedInternal) updateState(null, value)
        }

    /**
     * The default value of this preference, when nothing was committed to storage yet
     */
    var defaultValue = false
    var checkedBeforeChangeListener: OnCheckedBeforeChangeListener? = null

    var checkedAfterChangeListener: OnCheckedAfterChangeListener? = null

    @StringRes
    var summaryOnRes: Int = DISABLED_RESOURCE_ID
    var summaryOn: CharSequence? = null

    /**
     * When set to true, dependents are disabled when this preference is checked,
     * and are enabled when it's not
     */
    var disableDependents = false

    fun copyTwoState(other: TwoStatePreference): TwoStatePreference {
        defaultValue = other.defaultValue
        summaryOn = other.summaryOn
        summaryOnRes = other.summaryOnRes
        disableDependents = other.disableDependents
        checkedBeforeChangeListener = other.checkedBeforeChangeListener
        checkedAfterChangeListener = other.checkedAfterChangeListener
        return this
    }

    override val state: Boolean get() = checkedInternal xor disableDependents

    override fun onAttach() {
        checkedInternal = getBoolean(defaultValue)
        super.onAttach()
    }

    override fun resolveSummary(context: Context) = when {
        checkedInternal && summaryOnRes != DISABLED_RESOURCE_ID -> context.resources.getText(
            summaryOnRes
        )

        checkedInternal && summaryOn != null -> summaryOn
        else -> super.resolveSummary(context)
    }

    override fun bindViews(holder: PreferencesAdapter.ViewHolder) {
        super.bindViews(holder)
        updateButton(holder)
    }

    private fun updateState(holder: PreferencesAdapter.ViewHolder?, new: Boolean) {
        if (checkedBeforeChangeListener?.onCheckedBeforeChanged(this, holder, new) != false) {
            commitBoolean(new)
            checkedInternal = new // Update internal state
            if (holder != null) {
                if (summaryOnRes != DISABLED_RESOURCE_ID || summaryOn != null) {
                    bindViews(holder)
                } else updateButton(holder)
            } else requestRebind()
            publishState()
            checkedAfterChangeListener?.onCheckedAfterChanged(this, holder, new)
        }
    }

    private fun updateButton(holder: PreferencesAdapter.ViewHolder) {
        holder.icon?.drawable?.apply {
            if (this is StateListDrawable) {
                setState(
                    if (checkedInternal) intArrayOf(android.R.attr.state_checked) else IntArray(
                        0
                    )
                )
            }
        }
        (holder.widget as CompoundButton?)?.isChecked = checkedInternal
    }

    override fun onClick(holder: PreferencesAdapter.ViewHolder) {
        updateState(holder, !checkedInternal)
    }

    fun interface OnCheckedAfterChangeListener {
        fun onCheckedAfterChanged(
            preference: TwoStatePreference,
            holder: PreferencesAdapter.ViewHolder?,
            checked: Boolean
        )
    }

    fun interface OnCheckedBeforeChangeListener {
        /**
         * Notified when the [checked][TwoStatePreference.checked] state of the connected [TwoStatePreference] changes.
         * This is called before the change gets persisted and can be prevented by returning false.
         *
         * @param holder the [ViewHolder][PreferencesAdapter.ViewHolder] with the views of the Preference instance,
         * or null if the change didn't occur as part of a click event
         * @param checked the new state
         *
         * @return true to commit the new button state to [SharedPreferences][android.content.SharedPreferences]
         */
        fun onCheckedBeforeChanged(
            preference: TwoStatePreference,
            holder: PreferencesAdapter.ViewHolder?,
            checked: Boolean
        ): Boolean
    }
}