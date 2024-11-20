package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.Flow

interface IStatusApi {
    @CheckResult
    fun set(text: String?, groupId: Long?): Flow<Boolean>
}