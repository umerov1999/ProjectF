package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import io.reactivex.rxjava3.core.Single

interface IStatusApi {
    @CheckResult
    operator fun set(text: String?, groupId: Int?): Single<Boolean>
}