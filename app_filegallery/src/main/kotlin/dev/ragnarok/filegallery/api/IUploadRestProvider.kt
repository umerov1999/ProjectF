package dev.ragnarok.filegallery.api

import dev.ragnarok.filegallery.api.rest.SimplePostHttp
import kotlinx.coroutines.flow.Flow

interface IUploadRestProvider {
    fun provideUploadRest(): Flow<SimplePostHttp>
}