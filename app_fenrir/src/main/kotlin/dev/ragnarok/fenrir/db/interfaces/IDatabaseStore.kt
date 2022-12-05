package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.CountryDboEntity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IDatabaseStore {
    fun storeCountries(accountId: Int, dbos: List<CountryDboEntity>): Completable
    fun getCountries(accountId: Int): Single<List<CountryDboEntity>>
}