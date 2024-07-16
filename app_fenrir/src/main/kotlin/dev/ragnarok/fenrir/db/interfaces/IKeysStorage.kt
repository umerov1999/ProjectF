package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.crypt.AesKeyPair
import dev.ragnarok.fenrir.util.Optional
import kotlinx.coroutines.flow.Flow

interface IKeysStorage : IStorage {
    @CheckResult
    fun saveKeyPair(pair: AesKeyPair): Flow<Boolean>

    @CheckResult
    fun getAll(accountId: Long): Flow<List<AesKeyPair>>

    @CheckResult
    fun getKeys(accountId: Long, peerId: Long): Flow<List<AesKeyPair>>

    @CheckResult
    fun findLastKeyPair(accountId: Long, peerId: Long): Flow<Optional<AesKeyPair>>

    @CheckResult
    fun findKeyPairFor(accountId: Long, sessionId: Long): Flow<AesKeyPair?>

    @CheckResult
    fun deleteAll(accountId: Long): Flow<Boolean>
}