package dev.ragnarok.fenrir.crypt

import io.reactivex.rxjava3.core.Single

interface ISessionIdGenerator {
    fun generateNextId(): Single<Long>
}