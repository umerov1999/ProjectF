package dev.ragnarok.fenrir.domain.mappers

interface MapF<O, R> {
    fun map(orig: O): R
}