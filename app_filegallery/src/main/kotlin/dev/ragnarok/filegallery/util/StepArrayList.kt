package dev.ragnarok.filegallery.util

class StepArrayList<T>(list: List<T>) : ArrayList<T>(list) {
    private var currentItem = 0
    fun getNext(): T? {
        if (size <= 0) {
            return null
        }
        if (currentItem >= size) {
            currentItem = 0
        }
        return get(currentItem++)
    }
}