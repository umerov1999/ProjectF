package dev.ragnarok.filegallery.util

object AssertUtils {
    /**
     * Returns `o` if non-null, or throws `NullPointerException`.
     */
    fun <T> requireNonNull(o: T?): T {
        if (o == null) {
            throw NullPointerException()
        }
        return o
    }

    /**
     * Returns `o` if non-null, or throws `NullPointerException`
     * with the given detail message.
     */
    fun <T> requireNonNull(o: T?, message: String?): T {
        if (o == null) {
            throw NullPointerException(message)
        }
        return o
    }

    fun assertPositive(value: Int) {
        check(value > 0)
    }

    fun assertTrue(value: Boolean) {
        check(value)
    }
}