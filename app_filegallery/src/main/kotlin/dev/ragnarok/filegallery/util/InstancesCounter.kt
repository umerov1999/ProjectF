package dev.ragnarok.filegallery.util

import java.util.concurrent.atomic.AtomicInteger

class InstancesCounter {
    private val map: MutableMap<Class<*>, AtomicInteger>
    fun incrementAndGet(c: Class<*>): Int {
        var counter = map[c]
        if (counter == null) {
            counter = AtomicInteger()
            map[c] = counter
        }
        return counter.incrementAndGet()
    }

    fun fireExists(c: Class<*>, id: Int) {
        var counter = map[c]
        if (counter == null) {
            counter = AtomicInteger()
            map[c] = counter
        }
        if (counter.get() < id) {
            counter.set(id)
        }
    }

    init {
        map = HashMap()
    }
}