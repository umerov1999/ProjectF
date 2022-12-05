package dev.ragnarok.fenrir.db.impl

import android.util.SparseArray
import dev.ragnarok.fenrir.crypt.AesKeyPair
import dev.ragnarok.fenrir.db.interfaces.IKeysStorage
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import io.reactivex.rxjava3.core.*
import java.util.concurrent.CopyOnWriteArrayList

internal class KeysRamStorage : IKeysStorage {
    private val mData = SparseArray<MutableList<AesKeyPair>>()
    private fun prepareKeysFor(accountId: Int): MutableList<AesKeyPair> {
        var list = mData[accountId]
        if (list == null) {
            list = CopyOnWriteArrayList()
            mData.put(accountId, list)
        }
        return list
    }

    override fun saveKeyPair(pair: AesKeyPair): Completable {
        return Completable.create { e: CompletableEmitter ->
            prepareKeysFor(pair.accountId).add(pair)
            e.onComplete()
        }
    }

    override fun getAll(accountId: Int): Single<List<AesKeyPair>> {
        return Single.create { e: SingleEmitter<List<AesKeyPair>> ->
            val list: List<AesKeyPair>? = mData[accountId]
            val result: MutableList<AesKeyPair> = ArrayList(if (list == null) 0 else 1)
            if (list != null) {
                result.addAll(list)
            }
            e.onSuccess(result)
        }
    }

    override fun getKeys(accountId: Int, peerId: Int): Single<List<AesKeyPair>> {
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

    override fun findLastKeyPair(accountId: Int, peerId: Int): Single<Optional<AesKeyPair>> {
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

    override fun findKeyPairFor(accountId: Int, sessionId: Long): Maybe<AesKeyPair> {
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

    override fun deleteAll(accountId: Int): Completable {
        return Completable.create { e: CompletableEmitter ->
            mData.remove(accountId)
            e.onComplete()
        }
    }

    override val stores: IStorages
        get() = throw UnsupportedOperationException()
}