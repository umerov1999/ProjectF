package dev.ragnarok.fenrir.model

class DataWrapper<T>(private val data: MutableList<T>, var isEnabled: Boolean) {
    fun setEnabled(enabled: Boolean): DataWrapper<*> {
        isEnabled = enabled
        return this
    }

    fun size(): Int {
        return data.size
    }

    fun get(): MutableList<T> {
        return data
    }

    fun clear() {
        data.clear()
    }

    fun addAll(append: List<T>?) {
        append ?: return
        data.addAll(append)
    }

    fun replace(data: List<T>?) {
        this.data.clear()
        if (data != null) {
            this.data.addAll(data)
        }
        tryTrimToSize()
    }

    private fun tryTrimToSize() {
        if (data is ArrayList<*>) {
            (data as ArrayList<*>).trimToSize()
        }
    }
}