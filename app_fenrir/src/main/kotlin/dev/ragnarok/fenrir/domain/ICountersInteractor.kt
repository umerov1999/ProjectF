package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.SectionCounters
import io.reactivex.rxjava3.core.Single

interface ICountersInteractor {
    fun getApiCounters(accountId: Int): Single<SectionCounters>
}