package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.Fields
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IStoriesShortVideosApi
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiNarratives
import dev.ragnarok.fenrir.api.model.VKApiStory
import dev.ragnarok.fenrir.api.model.response.ShortVideosResponse
import dev.ragnarok.fenrir.api.model.response.StoriesResponse
import dev.ragnarok.fenrir.api.model.response.StoryGetResponse
import dev.ragnarok.fenrir.api.model.response.ViewersListResponse
import dev.ragnarok.fenrir.api.model.server.VKApiStoryUploadServer
import dev.ragnarok.fenrir.api.services.IStoriesShortVideosService
import io.reactivex.rxjava3.core.Single

internal class StoriesShortVideosApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IStoriesShortVideosApi {

    override fun getStories(
        owner_id: Long?,
        extended: Int?,
        fields: String?
    ): Single<StoriesResponse> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getStories(owner_id, extended, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getNarratives(
        owner_id: Long,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiNarratives>> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getNarratives(owner_id, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getStoryById(
        stories: List<AccessIdPair>,
        extended: Int?,
        fields: String?
    ): Single<StoryGetResponse> {
        val storyString = join(stories, ",") { AccessIdPair.format(it) }
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getStoryById(storyString, extended, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getStoriesViewers(
        ownerId: Long?,
        storyId: Int?,
        offset: Int?, count: Int?
    ): Single<ViewersListResponse> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.SERVICE)
            .flatMap { service ->
                service
                    .getStoriesViewers(
                        ownerId, storyId, offset, count, 1, Fields.FIELDS_BASE_USER
                    )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchStories(
        q: String?,
        mentioned_id: Long?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<StoriesResponse> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.searchStories(q, mentioned_id, count, extended, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun stories_delete(owner_id: Long, story_id: Int): Single<Int> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.stories_delete(owner_id, story_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun stories_getPhotoUploadServer(
        group_id: Long?,
        reply_to_story: String?
    ): Single<VKApiStoryUploadServer> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.stories_getPhotoUploadServer(1, group_id, reply_to_story)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun stories_getVideoUploadServer(
        group_id: Long?,
        reply_to_story: String?
    ): Single<VKApiStoryUploadServer> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.stories_getVideoUploadServer(1, group_id, reply_to_story)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun stories_save(upload_results: String?): Single<Items<VKApiStory>> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.stories_save(upload_results)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getShortVideos(
        ownerId: Long?,
        startFrom: String?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<ShortVideosResponse> {
        return if (ownerId != null) {
            provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
                .flatMap { service ->
                    service.getOwnerShortVideos(
                        ownerId,
                        startFrom,
                        count,
                        extended,
                        fields
                    )
                        .map(extractResponseWithErrorHandling())
                }
        } else {
            provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
                .flatMap { service ->
                    service.getTopShortVideos(
                        startFrom,
                        count,
                        extended,
                        fields
                    )
                        .map(extractResponseWithErrorHandling())
                }
        }
    }
}
