/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.ragnarok.fenrir.util.serializeble.json.internal

import kotlinx.serialization.descriptors.SerialDescriptor

private typealias DescriptorData<T> = MutableMap<DescriptorSchemaCache.Key<T>, T>

/**
 * A type-safe map for storing custom information (such as format schema), associated with [SerialDescriptor].
 *
 * This cache uses ConcurrentHashMap on JVM and regular maps on other platforms.
 * To be able to work with it from multiple threads in Kotlin/Native, use @[ThreadLocal] in appropriate places.
 */
internal class DescriptorSchemaCache {
    // 16 is default CHM size, as we do not know number of descriptors in an application (but it's likely not 1)
    private val map: MutableMap<SerialDescriptor, DescriptorData<Any>> = createMapForCache(16)

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> set(descriptor: SerialDescriptor, key: Key<T>, value: T) {
        // Initial capacity = number of known DescriptorSchemaCache.Key instances
        map.getOrPut(descriptor) { createMapForCache(2) }[key as Key<Any>] = value as Any
    }

    fun <T : Any> getOrPut(descriptor: SerialDescriptor, key: Key<T>, defaultValue: () -> T): T {
        get(descriptor, key)?.let { return it }
        val value = defaultValue()
        set(descriptor, key, value)
        return value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(descriptor: SerialDescriptor, key: Key<T>): T? {
        return map[descriptor]?.get(key as Key<Any>) as? T
    }

    /**
     * A key for associating user data of type [T] with a given [SerialDescriptor].
     */
    class Key<T : Any>
}
