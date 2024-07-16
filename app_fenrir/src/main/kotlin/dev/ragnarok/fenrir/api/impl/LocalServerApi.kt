package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.UserAgentTool
import dev.ragnarok.fenrir.api.ILocalServerServiceProvider
import dev.ragnarok.fenrir.api.interfaces.ILocalServerApi
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiAudio
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.model.FileRemote
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils.firstNonEmptyString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class LocalServerApi(private val service: ILocalServerServiceProvider) : ILocalServerApi {
    override fun getVideos(offset: Int?, count: Int?, reverse: Boolean): Flow<Items<VKApiVideo>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.getVideos(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAudios(offset: Int?, count: Int?, reverse: Boolean): Flow<Items<VKApiAudio>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.getAudios(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getPhotos(offset: Int?, count: Int?, reverse: Boolean): Flow<Items<VKApiPhoto>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.getPhotos(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getDiscography(
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Flow<Items<VKApiAudio>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.getDiscography(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchVideos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Flow<Items<VKApiVideo>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.searchVideos(query, offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchPhotos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Flow<Items<VKApiPhoto>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.searchPhotos(query, offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchAudios(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Flow<Items<VKApiAudio>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.searchAudios(query, offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchDiscography(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Flow<Items<VKApiAudio>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.searchDiscography(query, offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun update_time(hash: String?): Flow<Int> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.update_time(hash)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun delete_media(hash: String?): Flow<Int> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.delete_media(hash)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get_file_name(hash: String?): Flow<String> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.get_file_name(hash)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun update_file_name(hash: String?, name: String?): Flow<Int> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.update_file_name(hash, name)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun rebootPC(type: String?): Flow<Int> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.rebootPC(type)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun fsGet(dir: String?): Flow<Items<FileRemote>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.fsGet(dir)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun uploadAudio(hash: String?): Flow<Int> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.uploadAudio(
                    hash,
                    Settings.get().accounts().currentAccessToken,
                    UserAgentTool.USER_AGENT_CURRENT_ACCOUNT
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    companion object {
        inline fun <reified T : Any> extractResponseWithErrorHandling(): (BaseResponse<T>) -> T =
            { err ->
                err.error?.let {
                    throw Exception(
                        firstNonEmptyString(
                            err.error?.errorMsg,
                            "Error"
                        )
                    )
                } ?: (err.response
                    ?: throw NullPointerException("Local Server return null response"))
            }
    }
}