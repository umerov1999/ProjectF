package dev.ragnarok.filegallery.api.impl

import dev.ragnarok.filegallery.api.ILocalServerServiceProvider
import dev.ragnarok.filegallery.api.PercentagePublisher
import dev.ragnarok.filegallery.api.interfaces.ILocalServerApi
import dev.ragnarok.filegallery.api.model.Items
import dev.ragnarok.filegallery.api.model.response.BaseResponse
import dev.ragnarok.filegallery.api.util.ProgressRequestBody
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.FileRemote
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.util.Utils.firstNonEmptyString
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.functions.Function

internal class LocalServerApi(private val service: ILocalServerServiceProvider) : ILocalServerApi {
    override fun getVideos(offset: Int?, count: Int?, reverse: Boolean): Single<List<Video>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.getVideos(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAudios(offset: Int?, count: Int?, reverse: Boolean): Single<List<Audio>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.getAudios(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getPhotos(
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Single<MutableList<Photo>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.getPhotos(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandlingMutable())
            }
    }

    override fun getDiscography(offset: Int?, count: Int?, reverse: Boolean): Single<List<Audio>> {
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
    ): Single<List<Video>> {
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
    ): Single<List<Photo>> {
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
    ): Single<List<Audio>> {
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
    ): Single<List<Audio>> {
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
                    .map(extractResponseWithErrorHandlingSimple())
            }
    }

    override fun delete_media(hash: String?): Single<Int> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.delete_media(hash)
                    .map(extractResponseWithErrorHandlingSimple())
            }
    }

    override fun get_file_name(hash: String?): Single<String> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.get_file_name(hash)
                    .map(extractResponseWithErrorHandlingSimple())
            }
    }

    override fun update_file_name(hash: String?, name: String?): Single<Int> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.update_file_name(hash, name)
                    .map(extractResponseWithErrorHandlingSimple())
            }
    }

    override fun rebootPC(type: String?): Single<Int> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.rebootPC(type)
                    .map(extractResponseWithErrorHandlingSimple())
            }
    }

    override fun fsGet(dir: String?): Single<List<FileRemote>> {
        return service.provideLocalServerService()
            .flatMap { service ->
                service.fsGet(dir)
                    .map(extractResponseWithErrorHandling())
            }
    }

    companion object {
        internal fun wrapPercentageListener(listener: PercentagePublisher?): ProgressRequestBody.UploadCallbacks {
            return object : ProgressRequestBody.UploadCallbacks {
                override fun onProgressUpdate(percentage: Int) {
                    listener?.onProgressChanged(percentage)
                }
            }
        }

        inline fun <reified T> extractResponseWithErrorHandling(): Function<BaseResponse<Items<T>>, List<T>> {
            return Function { response ->
                if (response.error != null) {
                    throw Exceptions.propagate(
                        Exception(
                            firstNonEmptyString(
                                response.error?.errorMsg,
                                "Error"
                            )
                        )
                    )
                }
                response.response?.items.nonNullNoEmpty {
                    return@Function it
                }
                return@Function ArrayList<T>()
            }
        }

        inline fun <reified T> extractResponseWithErrorHandlingMutable(): Function<BaseResponse<Items<T>>, MutableList<T>> {
            return Function { response ->
                if (response.error != null) {
                    throw Exceptions.propagate(
                        Exception(
                            firstNonEmptyString(
                                response.error?.errorMsg,
                                "Error"
                            )
                        )
                    )
                }
                response.response?.items.nonNullNoEmpty {
                    return@Function it
                }
                return@Function ArrayList<T>()
            }
        }

        inline fun <reified T : Any> extractResponseWithErrorHandlingSimple(): Function<BaseResponse<T>, T> {
            return Function { response ->
                if (response.error != null) {
                    throw Exceptions.propagate(
                        Exception(
                            firstNonEmptyString(
                                response.error?.errorMsg,
                                "Error"
                            )
                        )
                    )
                }
                response.response ?: throw NullPointerException("response")
            }
        }
    }
}