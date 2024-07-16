package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.api.services.ILocalServerService
import kotlinx.coroutines.flow.Flow

interface ILocalServerServiceProvider {
    fun provideLocalServerService(): Flow<ILocalServerService>
}