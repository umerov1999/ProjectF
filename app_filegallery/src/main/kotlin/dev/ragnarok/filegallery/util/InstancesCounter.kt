package dev.ragnarok.filegallery.util

import java.util.concurrent.atomic.AtomicLong

class InstancesCounter {
    private val map: MutableMap<Class<*>, AtomicLong>
    fun incrementAndGet(c: Class<*>): Long {
        var counter = map[c]
        if (counter == null) {
            counter = AtomicLong()
            map[c] = counter
        }
        return counter.incrementAndGet()
    }

    fun fireExists(c: Class<*>, id: Long) {
        var counter = map[c]
        if (counter == null) {
            counter = AtomicLong()
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
