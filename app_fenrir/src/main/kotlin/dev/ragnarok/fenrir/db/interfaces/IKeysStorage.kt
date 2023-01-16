package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.crypt.AesKeyPair
import dev.ragnarok.fenrir.util.Optional
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

interface IKeysStorage : IStorage {
    @CheckResult
    fun saveKeyPair(pair: AesKeyPair): Completable

    @CheckResult
    fun getAll(accountId: Long): Single<List<AesKeyPair>>

    @CheckResult
    fun getKeys(accountId: Long, peerId: Long): Single<List<AesKeyPair>>

    @CheckResult
    fun findLastKeyPair(accountId: Long, peerId: Long): Single<Optional<AesKeyPair>>

    @CheckResult
    fun findKeyPairFor(accountId: Long, sessionId: Long): Maybe<AesKeyPair>

    @CheckResult
    fun deleteAll(accountId: Long): Completable
}