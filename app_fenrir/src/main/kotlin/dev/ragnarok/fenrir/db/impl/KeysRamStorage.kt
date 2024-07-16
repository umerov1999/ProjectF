package dev.ragnarok.fenrir.db.impl

import dev.ragnarok.fenrir.crypt.AesKeyPair
import dev.ragnarok.fenrir.db.interfaces.IKeysStorage
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.CopyOnWriteArrayList

internal class KeysRamStorage : IKeysStorage {
    private val mData = HashMap<Long, MutableList<AesKeyPair>>()
    private fun prepareKeysFor(accountId: Long): MutableList<AesKeyPair> {
        var list = mData[accountId]
        if (list == null) {
            list = CopyOnWriteArrayList()
            mData[accountId] = list
        }
        return list
    }

    override fun saveKeyPair(pair: AesKeyPair): Flow<Boolean> {
        return flow {
            prepareKeysFor(pair.accountId).add(pair)
            emit(true)
        }
    }

    override fun getAll(accountId: Long): Flow<List<AesKeyPair>> {
        return flow {
            val list: List<AesKeyPair>? = mData[accountId]
            val result: MutableList<AesKeyPair> = ArrayList(if (list == null) 0 else 1)
            if (list != null) {
                result.addAll(list)
            }
            emit(result)
        }
    }

    override fun getKeys(accountId: Long, peerId: Long): Flow<List<AesKeyPair>> {
        return flow {
            val list: List<AesKeyPair>? = mData[accountId]
            val result: MutableList<AesKeyPair> = ArrayList(if (list == null) 0 else 1)
            if (list != null) {
                for (pair in list) {
                    if (pair.peerId == peerId) {
                        result.add(pair)
                    }
                }
            }
            emit(result)
        }
    }

    override fun findLastKeyPair(accountId: Long, peerId: Long): Flow<Optional<AesKeyPair>> {
        return flow {
            val list: List<AesKeyPair>? = mData[accountId]
            var result: AesKeyPair? = null
            if (list != null) {
                for (pair in list) {
                    if (pair.peerId == peerId) {
                        result = pair
                    }
                }
            }
            emit(wrap(result))
        }
    }

    override fun findKeyPairFor(accountId: Long, sessionId: Long): Flow<AesKeyPair?> {
        return flow {
            val pairs: List<AesKeyPair>? = mData[accountId]
            var result: AesKeyPair? = null
            if (pairs != null) {
                for (pair in pairs) {
                    if (pair.sessionId == sessionId) {
                        result = pair
                        break
                    }
                }
            }
            emit(result)
        }
    }

    override fun deleteAll(accountId: Long): Flow<Boolean> {
        return flow {
            mData.remove(accountId)
            emit(true)
        }
    }

    override val stores: IStorages
        get() = throw UnsupportedOperationException()
}