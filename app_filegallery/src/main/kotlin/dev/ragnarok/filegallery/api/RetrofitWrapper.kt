package dev.ragnarok.filegallery.api

import retrofit2.Retrofit
import java.util.*

class RetrofitWrapper private constructor(
    private val retrofit: Retrofit,
    private val withCaching: Boolean
) {
    private val servicesCache: MutableMap<Class<*>, Any?>? =
        if (withCaching) Collections.synchronizedMap(HashMap(4)) else null

    @Suppress("UNCHECKED_CAST")
    fun <T> create(serviceClass: Class<T>): T {
        if (!withCaching || servicesCache == null) {
            return retrofit.create(serviceClass)
        }
        if (servicesCache.containsKey(serviceClass)) {
            return servicesCache[serviceClass] as T
        }
        val service = retrofit.create(serviceClass)
        servicesCache[serviceClass] = service
        return service
    }

    fun cleanup() {
        servicesCache?.clear()
    }

    companion object {
        fun wrap(retrofit: Retrofit): RetrofitWrapper {
            return RetrofitWrapper(retrofit, true)
        }

        fun wrap(retrofit: Retrofit, withCaching: Boolean): RetrofitWrapper {
            return RetrofitWrapper(retrofit, withCaching)
        }
    }

}