package dev.ragnarok.fenrir.util

import de.maxr1998.modernpreferences.PreferenceScreen
import dev.ragnarok.fenrir.Includes

class StepArrayList<T>(list: List<T>, private val key: String? = null) : ArrayList<T>(list) {
    private var currentItem = 0
    fun getNext(): T? {
        if (size <= 0) {
            return null
        }
        currentItem++
        if (currentItem >= size - 1) {
            currentItem = 0
        }
        key?.let {
            PreferenceScreen.getPreferences(Includes.provideApplicationContext()).edit()
                .putInt(key, currentItem).apply()
        }
        return get(currentItem)
    }

    init {
        key?.let {
            currentItem =
                PreferenceScreen.getPreferences(Includes.provideApplicationContext()).getInt(key, 0)
        }
    }
}
