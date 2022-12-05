package dev.ragnarok.filegallery.util

class Optional<T> private constructor(private val value: T?) {
    fun get(): T? {
        return value
    }

    val isEmpty: Boolean
        get() = value == null

    fun nonEmpty(): Boolean {
        return value != null
    }

    fun requireNonEmpty(): T {
        return value!!
    }

    companion object {
        fun <T> wrap(value: T?): Optional<T> {
            return Optional(value)
        }

        fun <T> empty(): Optional<T> {
            return Optional(null)
        }
    }

}