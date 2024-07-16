package dev.ragnarok.filegallery.api

import dev.ragnarok.filegallery.api.rest.SimplePostHttp
import kotlinx.coroutines.flow.Flow

interface ILocalServerRestProvider {
    fun provideLocalServerRest(): Flow<SimplePostHttp>
}