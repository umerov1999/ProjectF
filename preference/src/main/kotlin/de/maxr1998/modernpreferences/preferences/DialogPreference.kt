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

import androidx.fragment.app.FragmentManager
import de.maxr1998.modernpreferences.Preference
import de.maxr1998.modernpreferences.PreferencesAdapter


abstract class DialogPreference(key: String, val fragmentManager: FragmentManager) :
    Preference(key) {

    abstract fun createAndShowDialogFragment()

    override fun onClick(holder: PreferencesAdapter.ViewHolder) {
        createAndShowDialogFragment()
    }
}