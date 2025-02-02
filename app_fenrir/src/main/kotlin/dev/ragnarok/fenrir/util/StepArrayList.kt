package dev.ragnarok.fenrir.util

import androidx.core.content.edit
import de.maxr1998.modernpreferences.PreferenceScreen
import dev.ragnarok.fenrir.Includes

class StepArrayList<T>(list: List<T>, private val key: String? = null) : ArrayList<T>(list) {
    private var currentItem = 0
    fun getNext(): T? {
        if (isEmpty()) {
            return null
        }
        val curVl = get(currentItem)
        currentItem++
        if (currentItem >= size) {
            currentItem = 0
        }
        key?.let {
            PreferenceScreen.getPreferences(Includes.provideApplicationContext()).edit {
                putInt(key, currentItem)
            }
        }
        return curVl
    }

    init {
        key?.let {
            currentItem =
                PreferenceScreen.getPreferences(Includes.provideApplicationContext()).getInt(key, 0)
            if (currentItem >= size) {
                currentItem = 0
            }
        }
    }
}
