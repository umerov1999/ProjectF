package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
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
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Function

internal class LocalServerApi(private val service: ILocalServerServiceProvider) : ILocalServerApi {
    override fun getVideos(offset: Int?, count: Int?, reverse: Boolean): Single<Items<VKApiVideo>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.getVideos(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAudios(offset: Int?, count: Int?, reverse: Boolean): Single<Items<VKApiAudio>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.getAudios(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getPhotos(offset: Int?, count: Int?, reverse: Boolean): Single<Items<VKApiPhoto>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.getPhotos(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getDiscography(
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<Items<VKApiAudio>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.getDiscography(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchVideos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<Items<VKApiVideo>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.searchVideos(query, offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchPhotos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<Items<VKApiPhoto>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.searchPhotos(query, offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchAudios(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<Items<VKApiAudio>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.searchAudios(query, offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchDiscography(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<Items<VKApiAudio>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.searchDiscography(query, offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun update_time(hash: String?): Single<Int> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.update_time(hash)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun delete_media(hash: String?): Single<Int> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.delete_media(hash)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get_file_name(hash: String?): Single<String> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.get_file_name(hash)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun update_file_name(hash: String?, name: String?): Single<Int> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.update_file_name(hash, name)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun rebootPC(type: String?): Single<Int> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.rebootPC(type)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun fsGet(dir: String?): Single<Items<FileRemote>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.fsGet(dir)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun uploadAudio(hash: String?): Single<Int> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.uploadAudio(
                    hash, Settings.get().accounts().currentAccessToken, Constants.USER_AGENT(
                        AccountType.BY_TYPE
                    )
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    companion object {
        inline fun <reified T : Any> extractResponseWithErrorHandling(): Function<BaseResponse<T>, T> {
            return Function { response: BaseResponse<T> ->
                response.error?.let {
                    throw Exception(
                        firstNonEmptyString(
                            response.error?.errorMsg,
                            "Error"
                        )
                    )
                } ?: (response.response
                    ?: throw NullPointerException("Local Server return null response"))
            }
        }
    }
}