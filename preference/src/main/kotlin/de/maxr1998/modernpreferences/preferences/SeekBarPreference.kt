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

import android.view.LayoutInflater
import android.widget.Space
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.slider.Slider
import com.google.android.material.slider.TickVisibilityMode
import de.maxr1998.modernpreferences.Preference
import de.maxr1998.modernpreferences.PreferencesAdapter
import de.maxr1998.modernpreferences.R
import de.maxr1998.modernpreferences.helpers.onSeek

class SeekBarPreference(key: String) : Preference(key) {
    var min = 0
    var max = 0
    var default: Int? = null
    var step = 1
        set(value) {
            require(value > 0) { "Stepping value must be >= 1" }
            field = value
        }

    var showTickMarks = false

    /**
     * The internal backing field of [value]
     */
    private var valueInternal = 0
    var value: Int
        get() = valueInternal
        set(v) {
            if (v != valueInternal && seekBeforeListener?.onSeekBefore(this, v) != false) {
                valueInternal = v
                commitInt(value)
                requestRebind()
                seekAfterListener?.onSeekAfter(this, v)
            }
        }

    var seekBeforeListener: OnSeekBeforeListener? = null
    var seekAfterListener: OnSeekAfterListener? = null
    var formatter: (Int) -> String = Int::toString

    fun copySeek(o: SeekBarPreference): SeekBarPreference {
        min = o.min
        max = o.max
        default = o.default
        step = o.step
        showTickMarks = o.showTickMarks
        seekBeforeListener = o.seekBeforeListener
        seekAfterListener = o.seekAfterListener
        formatter = o.formatter
        return this
    }

    override fun getWidgetLayoutResource() = R.layout.map_preference_widget_seekbar_stub

    override fun onAttach() {
        check(min <= max) { "Minimum value can't be greater than maximum!" }
        default?.let { default ->
            check(default in min..max) { "Default value must be in between min and max!" }
        }
        valueInternal = getInt(default ?: min)
    }

    override fun bindViews(holder: PreferencesAdapter.ViewHolder) {
        super.bindViews(holder)
        holder.root.apply {
            background = null
            clipChildren = false
        }
        holder.iconFrame.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            @Suppress("MagicNumber")
            bottomMargin = (40 * holder.itemView.resources.displayMetrics.density).toInt()
        }
        holder.title.updateLayoutParams<ConstraintLayout.LayoutParams> {
            goneBottomMargin = 0
        }
        holder.summary?.updateLayoutParams<ConstraintLayout.LayoutParams> {
            bottomMargin = 0
        }
        val widget = holder.widget as Space?
        val sb = (widget?.tag
            ?: LayoutInflater.from(widget?.context)
                .inflate(R.layout.map_preference_widget_seekbar, holder.root)
                .findViewById(android.R.id.progress)) as Slider?
        val tv = (sb?.tag ?: holder.itemView.findViewById(R.id.progress_text)) as TextView?
        widget?.tag = sb?.apply {
            isEnabled = enabled
            valueTo = max.toFloat()
            value = valueInternal.toFloat()
            valueFrom = min.toFloat()
            stepSize = step.toFloat()
            tickVisibilityMode =
                if (showTickMarks) TickVisibilityMode.TICK_VISIBILITY_AUTO_LIMIT else TickVisibilityMode.TICK_VISIBILITY_HIDDEN

            onSeek { v, done ->
                if (done) {
                    // Commit the last selected value
                    commitInt(valueInternal)
                    seekAfterListener?.onSeekAfter(this@SeekBarPreference, v)
                } else {
                    // Check if listener allows the value change
                    if (seekBeforeListener?.onSeekBefore(this@SeekBarPreference, v) != false) {
                        // Update internal value
                        valueInternal = v
                    } else {
                        // Restore previous value
                        value = valueInternal.toFloat()
                    }
                    // Update preview text
                    tv?.text = formatter(valueInternal)
                }
            }
        }
        sb?.tag = tv?.apply {
            isEnabled = enabled
            text = formatter(valueInternal)
        }
    }

    fun interface OnSeekAfterListener {
        fun onSeekAfter(
            preference: SeekBarPreference,
            value: Int
        )
    }

    fun interface OnSeekBeforeListener {
        /**
         * Notified when the [value][SeekBarPreference.value] of the connected [SeekBarPreference] changes.
         * This is called *before* the change gets persisted, which can be prevented by returning false.
         * or null if the change didn't occur as part of a click event
         * @param value the new state
         *
         * @return true to commit the new slider value to [SharedPreferences][android.content.SharedPreferences]
         */
        fun onSeekBefore(
            preference: SeekBarPreference,
            value: Int
        ): Boolean
    }
}
