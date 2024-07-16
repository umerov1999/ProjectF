package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.CountryDboEntity
import kotlinx.coroutines.flow.Flow

interface IDatabaseStore {
    fun storeCountries(accountId: Long, dbos: List<CountryDboEntity>): Flow<Boolean>
    fun getCountries(accountId: Long): Flow<List<CountryDboEntity>>
}