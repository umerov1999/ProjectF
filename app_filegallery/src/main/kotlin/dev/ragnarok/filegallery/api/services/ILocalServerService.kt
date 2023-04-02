package dev.ragnarok.filegallery.api.services

import dev.ragnarok.filegallery.api.model.Items
import dev.ragnarok.filegallery.api.model.response.BaseResponse
import dev.ragnarok.filegallery.api.rest.IServiceRest
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.model.FileRemote
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.model.Video
import io.reactivex.rxjava3.core.Single

class ILocalServerService : IServiceRest() {
    fun getAudios(
        offset: Int?,
        count: Int?,
        reverse: Int?
    ): Single<BaseResponse<Items<Audio>>> {
        return rest.request(
            "audio.get", form(
                "offset" to offset,
                "count" to count,
                "reverse" to reverse
            ), items(Audio.serializer())
        )
    }

    fun getDiscography(
        offset: Int?,
        count: Int?,
        reverse: Int?
    ): Single<BaseResponse<Items<Audio>>> {
        return rest.request(
            "discography.get", form(
                "offset" to offset,
                "count" to count,
                "reverse" to reverse
            ), items(Audio.serializer())
        )
    }

    fun getPhotos(
        offset: Int?,
        count: Int?,
        reverse: Int?
    ): Single<BaseResponse<Items<Photo>>> {
        return rest.request(
            "photos.get", form(
                "offset" to offset,
                "count" to count,
                "reverse" to reverse
            ), items(Photo.serializer())
        )
    }

    fun getVideos(
        offset: Int?,
        count: Int?,
        reverse: Int?
    ): Single<BaseResponse<Items<Video>>> {
        return rest.request(
            "video.get", form(
                "offset" to offset,
                "count" to count,
                "reverse" to reverse
            ), items(Video.serializer())
        )
    }

    fun searchAudios(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Int?
    ): Single<BaseResponse<Items<Audio>>> {
        return rest.request(
            "audio.search", form(
                "q" to query,
                "offset" to offset,
                "count" to count,
                "reverse" to reverse
            ), items(Audio.serializer())
        )
    }

    fun searchDiscography(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Int?
    ): Single<BaseResponse<Items<Audio>>> {
        return rest.request(
            "discography.search", form(
                "q" to query,
                "offset" to offset,
                "count" to count,
                "reverse" to reverse
            ), items(Audio.serializer())
        )
    }

    fun searchVideos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Int?
    ): Single<BaseResponse<Items<Video>>> {
        return rest.request(
            "video.search", form(
                "q" to query,
                "offset" to offset,
                "count" to count,
                "reverse" to reverse
            ), items(Video.serializer())
        )
    }

    fun searchPhotos(
        query: String?,
        offset: Int?,
        count: Int?,
        reverse: Int?
    ): Single<BaseResponse<Items<Photo>>> {
        return rest.request(
            "photos.search", form(
                "q" to query,
                "offset" to offset,
                "count" to count,
                "reverse" to reverse
            ), items(Photo.serializer())
        )
    }

    fun update_time(hash: String?): Single<BaseResponse<Int>> {
        return rest.request("update_time", form("hash" to hash), baseInt)
    }

    fun delete_media(hash: String?): Single<BaseResponse<Int>> {
        return rest.request("delete_media", form("hash" to hash), baseInt)
    }

    fun get_file_name(hash: String?): Single<BaseResponse<String>> {
        return rest.request("get_file_name", form("hash" to hash), baseString)
    }

    fun update_file_name(
        hash: String?,
        name: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "update_file_name", form(
                "hash" to hash,
                "name" to name
            ), baseInt
        )
    }

    fun fsGet(
        dir: String?
    ): Single<BaseResponse<Items<FileRemote>>> {
        return rest.request("fs.get", form("dir" to dir), items(FileRemote.serializer()))
    }

    fun rebootPC(
        type: String?
    ): Single<BaseResponse<Int>> {
        return rest.request("rebootPC", form("type" to type), baseInt)
    }
}