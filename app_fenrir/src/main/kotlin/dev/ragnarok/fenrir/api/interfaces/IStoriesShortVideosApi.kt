package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiNarratives
import dev.ragnarok.fenrir.api.model.VKApiStory
import dev.ragnarok.fenrir.api.model.response.ShortVideosResponse
import dev.ragnarok.fenrir.api.model.response.StoriesResponse
import dev.ragnarok.fenrir.api.model.response.StoryGetResponse
import dev.ragnarok.fenrir.api.model.response.ViewersListResponse
import dev.ragnarok.fenrir.api.model.server.VKApiStoryUploadServer
import kotlinx.coroutines.flow.Flow

interface IStoriesShortVideosApi {
    @CheckResult
    fun stories_getPhotoUploadServer(
        group_id: Long?,
        reply_to_story: String?
    ): Flow<VKApiStoryUploadServer>

    @CheckResult
    fun stories_getVideoUploadServer(
        group_id: Long?,
        reply_to_story: String?
    ): Flow<VKApiStoryUploadServer>

    @CheckResult
    fun getStoriesViewers(
        ownerId: Long?,
        storyId: Int?,
        offset: Int?,
        count: Int?
    ): Flow<ViewersListResponse>

    @CheckResult
    fun stories_delete(owner_id: Long, story_id: Int): Flow<Int>

    @CheckResult
    fun stories_save(upload_results: String?): Flow<Items<VKApiStory>>

    @CheckResult
    fun getStories(owner_id: Long?, extended: Int?, fields: String?): Flow<StoriesResponse>

    @CheckResult
    fun getNarratives(owner_id: Long, offset: Int?, count: Int?): Flow<Items<VKApiNarratives>>

    @CheckResult
    fun getStoryById(
        stories: List<AccessIdPair>,
        extended: Int?,
        fields: String?
    ): Flow<StoryGetResponse>

    @CheckResult
    fun searchStories(
        q: String?,
        mentioned_id: Long?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Flow<StoriesResponse>

    @CheckResult
    fun getShortVideos(
        ownerId: Long?,
        startFrom: String?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Flow<ShortVideosResponse>

    @CheckResult
    fun subscribe(owner_id: Long?): Flow<Int>

    @CheckResult
    fun unsubscribe(owner_id: Long?): Flow<Int>
}
