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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class StoriesShortVideosApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IStoriesShortVideosApi {

    override fun getStories(
        owner_id: Long?,
        extended: Int?,
        fields: String?
    ): Flow<StoriesResponse> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getStories(owner_id, extended, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getNarratives(
        owner_id: Long,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiNarratives>> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getNarratives(owner_id, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getStoryById(
        stories: List<AccessIdPair>,
        extended: Int?,
        fields: String?
    ): Flow<StoryGetResponse> {
        val storyString = join(stories, ",") { AccessIdPair.format(it) }
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getStoryById(storyString, extended, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getStoriesViewers(
        ownerId: Long?,
        storyId: Int?,
        offset: Int?, count: Int?
    ): Flow<ViewersListResponse> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getStoriesViewers(
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
    ): Flow<StoriesResponse> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.searchStories(q, mentioned_id, count, extended, fields)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun stories_delete(owner_id: Long, story_id: Int): Flow<Int> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.stories_delete(owner_id, story_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun stories_getPhotoUploadServer(
        group_id: Long?,
        reply_to_story: String?
    ): Flow<VKApiStoryUploadServer> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.stories_getPhotoUploadServer(1, group_id, reply_to_story)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun stories_getVideoUploadServer(
        group_id: Long?,
        reply_to_story: String?
    ): Flow<VKApiStoryUploadServer> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.stories_getVideoUploadServer(1, group_id, reply_to_story)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun stories_save(upload_results: String?): Flow<Items<VKApiStory>> {
        return provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.stories_save(upload_results)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getShortVideos(
        ownerId: Long?,
        startFrom: String?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Flow<ShortVideosResponse> {
        return if (ownerId != null) {
            provideService(IStoriesShortVideosService(), TokenType.USER, TokenType.COMMUNITY)
                .flatMapConcat {
                    it.getOwnerShortVideos(
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
                .flatMapConcat {
                    it.getTopShortVideos(
                        startFrom,
                        count,
                        extended,
                        fields
                    )
                        .map(extractResponseWithErrorHandling())
                }
        }
    }

    override fun subscribe(owner_id: Long?): Flow<Int> {
        return provideService(IStoriesShortVideosService(), TokenType.USER)
            .flatMapConcat {
                it.subscribe(owner_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun unsubscribe(owner_id: Long?): Flow<Int> {
        return provideService(IStoriesShortVideosService(), TokenType.USER)
            .flatMapConcat {
                it.unsubscribe(owner_id)
                    .map(extractResponseWithErrorHandling())
            }
    }
}
