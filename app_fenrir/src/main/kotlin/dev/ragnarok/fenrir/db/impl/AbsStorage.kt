package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderResult
import android.content.ContentResolver
import android.content.Context
import dev.ragnarok.fenrir.db.DBHelper
import dev.ragnarok.fenrir.db.interfaces.IStorage
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.orZero

open class AbsStorage(private val mRepositoryContext: AppStorages) : IStorage {
    override val stores: IStorages
        get() = mRepositoryContext

    val context: Context
        get() = mRepositoryContext.applicationContext

    fun helper(accountId: Long): DBHelper {
        return DBHelper.getInstance(context, accountId)
    }

    protected val contentResolver: ContentResolver
        get() = mRepositoryContext.contentResolver

    companion object {
        fun extractId(result: ContentProviderResult): Int {
            return result.uri?.pathSegments?.get(1)?.toInt().orZero()
        }


        fun <T> addToListAndReturnIndex(target: MutableList<T>, item: T): Int {
            target.add(item)
            return target.size - 1
        }
    }
}