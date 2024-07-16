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
import dev.ragnarok.filegallery.nonNullNoEmptyOr
import dev.ragnarok.filegallery.util.Utils.firstNonEmptyString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class LocalServerApi(private val service: ILocalServerServiceProvider) : ILocalServerApi {
    override fun getVideos(offset: Int?, count: Int?, reverse: Boolean): Flow<List<Video>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.getVideos(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAudios(offset: Int?, count: Int?, reverse: Boolean): Flow<List<Audio>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.getAudios(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getPhotos(
        offset: Int?,
        count: Int?,
        reverse: Boolean
    ): Flow<MutableList<Photo>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.getPhotos(offset, count, if (reverse) 1 else 0)
                    .map(extractResponseWithErrorHandlingMutable())
            }
    }

    override fun getDiscography(offset: Int?, count: Int?, reverse: Boolean): Flow<List<Audio>> {
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
    ): Flow<List<Video>> {
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
    ): Flow<List<Photo>> {
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
    ): Flow<List<Audio>> {
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
    ): Flow<List<Audio>> {
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
                    .map(extractResponseWithErrorHandlingSimple())
            }
    }

    override fun delete_media(hash: String?): Flow<Int> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.delete_media(hash)
                    .map(extractResponseWithErrorHandlingSimple())
            }
    }

    override fun get_file_name(hash: String?): Flow<String> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.get_file_name(hash)
                    .map(extractResponseWithErrorHandlingSimple())
            }
    }

    override fun update_file_name(hash: String?, name: String?): Flow<Int> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.update_file_name(hash, name)
                    .map(extractResponseWithErrorHandlingSimple())
            }
    }

    override fun rebootPC(type: String?): Flow<Int> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.rebootPC(type)
                    .map(extractResponseWithErrorHandlingSimple())
            }
    }

    override fun fsGet(dir: String?): Flow<List<FileRemote>> {
        return service.provideLocalServerService()
            .flatMapConcat {
                it.fsGet(dir)
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

        inline fun <reified T> extractResponseWithErrorHandling(): (BaseResponse<Items<T>>) -> List<T> =
            {
                if (it.error != null) {
                    throw Throwable(
                        firstNonEmptyString(
                            it.error?.errorMsg,
                            "Error"
                        )
                    )
                } else {
                    it.response?.items.nonNullNoEmptyOr({ items -> items }, { ArrayList() })
                }
            }

        inline fun <reified T> extractResponseWithErrorHandlingMutable(): (BaseResponse<Items<T>>) -> MutableList<T> =
            {
                if (it.error != null) {
                    throw Throwable(
                        firstNonEmptyString(
                            it.error?.errorMsg,
                            "Error"
                        )
                    )
                } else {
                    it.response?.items.nonNullNoEmptyOr({ items -> items }, { ArrayList() })
                }
            }

        inline fun <reified T> extractResponseWithErrorHandlingSimple(): (BaseResponse<T>) -> T =
            {
                if (it.error != null) {
                    throw Throwable(
                        firstNonEmptyString(
                            it.error?.errorMsg,
                            "Error"
                        )
                    )
                } else {
                    it.response ?: throw NullPointerException("response")
                }
            }
    }
}