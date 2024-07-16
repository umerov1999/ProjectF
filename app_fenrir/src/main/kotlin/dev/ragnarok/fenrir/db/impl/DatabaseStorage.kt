package dev.ragnarok.fenrir.db.impl

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.provider.BaseColumns
import dev.ragnarok.fenrir.db.FenrirContentProvider
import dev.ragnarok.fenrir.db.FenrirContentProvider.Companion.getCountriesContentUriFor
import dev.ragnarok.fenrir.db.column.CountriesColumns
import dev.ragnarok.fenrir.db.interfaces.IDatabaseStore
import dev.ragnarok.fenrir.db.model.entity.CountryDboEntity
import dev.ragnarok.fenrir.getInt
import dev.ragnarok.fenrir.getString
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DatabaseStorage internal constructor(base: AppStorages) : AbsStorage(base), IDatabaseStore {
    override fun storeCountries(accountId: Long, dbos: List<CountryDboEntity>): Flow<Boolean> {
        return flow {
            val uri = getCountriesContentUriFor(accountId)
            val operations = ArrayList<ContentProviderOperation>(dbos.size + 1)
            operations.add(ContentProviderOperation.newDelete(uri).build())
            for (dbo in dbos) {
                val cv = ContentValues()
                cv.put(BaseColumns._ID, dbo.id)
                cv.put(CountriesColumns.NAME, dbo.title)
                operations.add(
                    ContentProviderOperation.newInsert(uri)
                        .withValues(cv)
                        .build()
                )
            }
            contentResolver.applyBatch(FenrirContentProvider.AUTHORITY, operations)
            emit(true)
        }
    }

    override fun getCountries(accountId: Long): Flow<List<CountryDboEntity>> {
        return flow {
            val uri = getCountriesContentUriFor(accountId)
            val cursor = contentResolver.query(uri, null, null, null, null)
            val dbos: MutableList<CountryDboEntity> = ArrayList(safeCountOf(cursor))
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (!isActive()) {
                        break
                    }
                    val id = cursor.getInt(BaseColumns._ID)
                    val title =
                        cursor.getString(CountriesColumns.NAME)
                    dbos.add(CountryDboEntity().set(id, title))
                }
                cursor.close()
            }
            emit(dbos)
        }
    }
}