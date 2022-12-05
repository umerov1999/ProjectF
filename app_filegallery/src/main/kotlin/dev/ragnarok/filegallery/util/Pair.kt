package dev.ragnarok.filegallery.util

class Pair<F, S>(val first: F, val second: S) {

    companion object {

        fun <F, S> create(first: F, second: S): Pair<F, S> {
            return Pair(first, second)
        }
    }
}
