package dev.ragnarok.fenrir.util

interface Action<T> {
    fun call(target: T)
}