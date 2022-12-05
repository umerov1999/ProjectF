package dev.ragnarok.fenrir.db.impl

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import dev.ragnarok.fenrir.crypt.AesKeyPair
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getKeysContentUriFor
import dev.ragnarok.fenrir.db.column.KeyColumns
import dev.ragnarok.fenrir.db.interfaces.IKeysStorage
import dev.ragnarok.fenrir.exception.DatabaseException
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getLong
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.util.Optional
import dev.ragnarok.fenrir.util.Optional.Companion.wrap
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import io.reactivex.rxjava3.core.*

internal class KeysPersistStorage(context: AppStorages) : AbsStorage(context), IKeysStorage {
    private fun map(cursor: Cursor): AesKeyPair {
        return AesKeyPair()
            .setVersion(cursor.getInt(KeyColumns.VERSION))
            .setPeerId(cursor.getInt(KeyColumns.PEER_ID))
            .setSessionId(cursor.getLong(KeyColumns.SESSION_ID))
            .setDate(cursor.getLong(KeyColumns.DATE))
            .setStartMessageId(cursor.getInt(KeyColumns.START_SESSION_MESSAGE_ID))
            .setEndMessageId(cursor.getInt(KeyColumns.END_SESSION_MESSAGE_ID))
            .setHisAesKey(cursor.getString(KeyColumns.IN_KEY))
            .setMyAesKey(cursor.getString(KeyColumns.OUT_KEY))
    }

    override fun saveKeyPair(pair: AesKeyPair): Completable {
        return Completable.create { e: CompletableEmitter ->
            val alreaadyExist = findKeyPairFor(pair.accountId, pair.sessionId)
                .blockingGet()
            if (alreaadyExist != null) {
                e.onError(DatabaseException("Key pair with the session ID is already in the database"))
                return@create
            }
            val cv = ContentValues()
            cv.put(KeyColumns.VERSION, pair.version)
            cv.put(KeyColumns.PEER_ID, pair.peerId)
            cv.put(KeyColumns.SESSION_ID, pair.sessionId)
            cv.put(KeyColumns.DATE, pair.date)
            cv.put(KeyColumns.START_SESSION_MESSAGE_ID, pair.startMessageId)
            cv.put(KeyColumns.END_SESSION_MESSAGE_ID, pair.endMessageId)
            cv.put(KeyColumns.OUT_KEY, pair.myAesKey)
            cv.put(KeyColumns.IN_KEY, pair.hisAesKey)
            val uri = getKeysContentUriFor(pair.accountId)
            context.contentResolver.insert(uri, cv)
            e.onComplete()
        }
    }

    override fun getAll(accountId: Int): Single<List<AesKeyPair>> {
        return Single.create { e: SingleEmitter<List<AesKeyPair>> ->
            val uri = getKeysContentUriFor(accountId)
            val cursor = context.contentResolver.query(uri, null, null, null, BaseColumns._ID)
            val pairs: MutableList<AesKeyPair> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    pairs.add(map(cursor).setAccountId(accountId))
                }
                cursor.close()
            }
            e.onSuccess(pairs)
        }
    }

    override fun getKeys(accountId: Int, peerId: Int): Single<List<AesKeyPair>> {
        return Single.create { e: SingleEmitter<List<AesKeyPair>> ->
            val uri = getKeysContentUriFor(accountId)
            val cursor = context.contentResolver
                .query(
                    uri,
                    null,
                    KeyColumns.PEER_ID + " = ?",
                    arrayOf(peerId.toString()),
                    BaseColumns._ID
                )
            val pairs: MutableList<AesKeyPair> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed) {
                        break
                    }
                    pairs.add(map(cursor).setAccountId(accountId))
                }
                cursor.close()
            }
            e.onSuccess(pairs)
        }
    }

    override fun findLastKeyPair(accountId: Int, peerId: Int): Single<Optional<AesKeyPair>> {
        return Single.create { e: SingleEmitter<Optional<AesKeyPair>> ->
            val uri = getKeysContentUriFor(accountId)
            val cursor = context.contentResolver
                .query(
                    uri,
                    null,
                    KeyColumns.PEER_ID + " = ?",
                    arrayOf(peerId.toString()),
                    BaseColumns._ID + " DESC LIMIT 1"
                )
            var pair: AesKeyPair? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    pair = map(cursor).setAccountId(accountId)
                }
                cursor.close()
            }
            e.onSuccess(wrap(pair))
        }
    }

    override fun findKeyPairFor(accountId: Int, sessionId: Long): Maybe<AesKeyPair> {
        return Maybe.create { e: MaybeEmitter<AesKeyPair> ->
            val uri = getKeysContentUriFor(accountId)
            val cursor = context.contentResolver
                .query(
                    uri,
                    null,
                    KeyColumns.SESSION_ID + " = ?",
                    arrayOf(sessionId.toString()),
                    null
                )
            var pair: AesKeyPair? = null
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    pair = map(cursor).setAccountId(accountId)
                }
                cursor.close()
            }
            if (pair != null) {
                e.onSuccess(pair)
            }
            e.onComplete()
        }
    }

    override fun deleteAll(accountId: Int): Completable {
        return Completable.create { e: CompletableEmitter ->
            val uri = getKeysContentUriFor(accountId)
            context.contentResolver.delete(uri, null, null)
            e.onComplete()
        }
    }
}