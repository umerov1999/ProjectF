package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiNarratives
import dev.ragnarok.fenrir.api.model.VKApiStory
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.ShortVideosResponse
import dev.ragnarok.fenrir.api.model.response.StoriesResponse
import dev.ragnarok.fenrir.api.model.response.StoryGetResponse
import dev.ragnarok.fenrir.api.model.response.ViewersListResponse
import dev.ragnarok.fenrir.api.model.server.VKApiStoryUploadServer
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IStoriesShortVideosService : IServiceRest() {
    fun stories_getPhotoUploadServer(
        add_to_news: Int?,
        group_id: Long?,
        reply_to_story: String?
    ): Single<BaseResponse<VKApiStoryUploadServer>> {
        return rest.request(
            "stories.getPhotoUploadServer",
            form(
                "add_to_news" to add_to_news,
                "reply_to_story" to reply_to_story,
                "group_id" to group_id
            ),
            base(VKApiStoryUploadServer.serializer())
        )
    }

    fun stories_getVideoUploadServer(
        add_to_news: Int?,
        group_id: Long?,
        reply_to_story: String?
    ): Single<BaseResponse<VKApiStoryUploadServer>> {
        return rest.request(
            "stories.getVideoUploadServer",
            form(
                "add_to_news" to add_to_news,
                "reply_to_story" to reply_to_story,
                "group_id" to group_id
            ),
            base(VKApiStoryUploadServer.serializer())
        )
    }

    fun getStoriesViewers(
        ownerId: Long?,
        storyId: Int?,
        offset: Int?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<ViewersListResponse>> {
        return rest.request(
            "stories.getViewers", form(
                "owner_id" to ownerId,
                "story_id" to storyId,
                "offset" to offset,
                "count" to count,
                "extended" to extended,
                "fields" to fields
            ), base(ViewersListResponse.serializer())
        )
    }

    fun stories_save(upload_results: String?): Single<BaseResponse<Items<VKApiStory>>> {
        return rest.request(
            "stories.save",
            form("upload_results" to upload_results),
            items(VKApiStory.serializer())
        )
    }

    fun stories_delete(owner_id: Long, story_id: Int): Single<BaseResponse<Int>> {
        return rest.request(
            "stories.delete", form(
                "owner_id" to owner_id,
                "story_id" to story_id
            ), baseInt
        )
    }

    fun getStories(
        owner_id: Long?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<StoriesResponse>> {
        return rest.request(
            "stories.get", form(
                "owner_id" to owner_id,
                "extended" to extended,
                "fields" to fields
            ), base(StoriesResponse.serializer())
        )
    }

    fun getNarratives(
        owner_id: Long,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiNarratives>>> {
        return rest.request(
            "narratives.getFromOwner", form(
                "owner_id" to owner_id,
                "offset" to offset,
                "count" to count
            ), items(VKApiNarratives.serializer())
        )
    }

    fun getStoryById(
        stories: String?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<StoryGetResponse>> {
        return rest.request(
            "stories.getById", form(
                "stories" to stories,
                "extended" to extended,
                "fields" to fields
            ), base(StoryGetResponse.serializer())
        )
    }

    fun searchStories(
        q: String?,
        mentioned_id: Long?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<StoriesResponse>> {
        return rest.request(
            "stories.search", form(
                "q" to q,
                "mentioned_id" to mentioned_id,
                "count" to count,
                "extended" to extended,
                "fields" to fields
            ), base(StoriesResponse.serializer())
        )
    }

    fun getTopShortVideos(
        startFrom: String?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<ShortVideosResponse>> {
        return rest.request(
            "shortVideo.getTopVideos", form(
                "start_from" to startFrom,
                "count" to count,
                "extended" to extended,
                "fields" to fields
            ), base(ShortVideosResponse.serializer())
        )
    }

    fun getOwnerShortVideos(
        ownerId: Long?,
        startFrom: String?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<ShortVideosResponse>> {
        return rest.request(
            "shortVideo.getOwnerVideos", form(
                "owner_id" to ownerId,
                "start_from" to startFrom,
                "count" to count,
                "extended" to extended,
                "fields" to fields
            ), base(ShortVideosResponse.serializer())
        )
    }
}