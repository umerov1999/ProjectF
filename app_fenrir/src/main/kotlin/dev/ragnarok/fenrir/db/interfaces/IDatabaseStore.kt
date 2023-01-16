package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.CountryDboEntity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IDatabaseStore {
    fun storeCountries(accountId: Long, dbos: List<CountryDboEntity>): Completable
    fun getCountries(accountId: Long): Single<List<CountryDboEntity>>
}