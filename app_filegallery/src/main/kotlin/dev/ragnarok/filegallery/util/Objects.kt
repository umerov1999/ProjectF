package dev.ragnarok.filegallery.util

object Objects {
    /**
     * Perform a safe equals between 2 objects.
     *
     *
     * It manages the case where the first object is null and it would have resulted in a
     * [NullPointerException] if `o1.equals(o2)` was used.
     *
     * @param o1 First object to check.
     * @param o2 Second object to check.
     * @return `true` if both objects are equal. `false` otherwise
     * @see Object.equals
     */
    fun safeEquals(o1: Any?, o2: Any?): Boolean {
        return if (o1 == null) {
            o2 == null
        } else {
            o1 == o2
        }
    }
}
