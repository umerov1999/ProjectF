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

package de.maxr1998.modernpreferences.helpers

import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.google.android.material.slider.Slider

const val KEY_ROOT_SCREEN = "root"

/**
 * A resource ID as a default value for optional attributes.
 */
const val DISABLED_RESOURCE_ID = -1

internal fun Slider.onSeek(callback: (Int, Boolean) -> Unit) {
    addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: Slider) {}

        override fun onStopTrackingTouch(slider: Slider) {
            callback(slider.value.toInt(), true)
        }
    })

    addOnChangeListener { slider, value, fromUser ->
        if (fromUser) callback(value.toInt(), false)
    }
}

internal inline fun <reified T : Parcelable> Parcel.readTypedObjectCompat(c: Parcelable.Creator<T>): T? {
    return if (readInt() != 0) {
        c.createFromParcel(this)
    } else {
        null
    }
}

internal inline fun <reified T : Parcelable> Parcel.writeTypedObjectCompat(
    parcel: T?,
    parcelableFlags: Int
) {
    if (parcel != null) {
        writeInt(1)
        parcel.writeToParcel(this, parcelableFlags)
    } else {
        writeInt(0)
    }
}

internal inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String): ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, T::class.java)
    } else {
        @Suppress("deprecation")
        getParcelableArrayList(key)
    }
}