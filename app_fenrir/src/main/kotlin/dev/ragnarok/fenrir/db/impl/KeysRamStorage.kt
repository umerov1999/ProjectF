package dev.ragnarok.fenrir.db.impl

import dev.ragnarok.fenrir.crypt.AesKeyPair
import dev.ragnarok.fenrir.db.interfaces.IKeysStorage
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import io.reactivex.rxjava3.core.*
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

    override fun saveKeyPair(pair: AesKeyPair): Completable {
        return Completable.create { e: CompletableEmitter ->
            prepareKeysFor(pair.accountId).add(pair)
            e.onComplete()
        }
    }

    override fun getAll(accountId: Long): Single<List<AesKeyPair>> {
        return Single.create { e: SingleEmitter<List<AesKeyPair>> ->
            val list: List<AesKeyPair>? = mData[accountId]
            val result: MutableList<AesKeyPair> = ArrayList(if (list == null) 0 else 1)
            if (list != null) {
                result.addAll(list)
            }
            e.onSuccess(result)
        }
    }

    override fun getKeys(accountId: Long, peerId: Long): Single<List<AesKeyPair>> {
        return Single.create { e: SingleEmitter<List<AesKeyPair>> ->
            val list: List<AesKeyPair>? = mData[accountId]
            val result: MutableList<AesKeyPair> = ArrayList(if (list == null) 0 else 1)
            if (list != null) {
                for (pair in list) {
                    if (pair.peerId == peerId) {
                        result.add(pair)
                    }
                }
            }
            e.onSuccess(result)
        }
    }

    override fun findLastKeyPair(accountId: Long, peerId: Long): Single<Optional<AesKeyPair>> {
        return Single.create { e: SingleEmitter<Optional<AesKeyPair>> ->
            val list: List<AesKeyPair>? = mData[accountId]
            var result: AesKeyPair? = null
            if (list != null) {
                for (pair in list) {
                    if (pair.peerId == peerId) {
                        result = pair
                    }
                }
            }
            e.onSuccess(wrap(result))
        }
    }

    override fun findKeyPairFor(accountId: Long, sessionId: Long): Maybe<AesKeyPair> {
        return Maybe.create { e: MaybeEmitter<AesKeyPair> ->
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
            if (result != null) {
                e.onSuccess(result)
            }
            e.onComplete()
        }
    }

    override fun deleteAll(accountId: Long): Completable {
        return Completable.create { e: CompletableEmitter ->
            mData.remove(accountId)
            e.onComplete()
        }
    }

    override val stores: IStorages
        get() = throw UnsupportedOperationException()
}