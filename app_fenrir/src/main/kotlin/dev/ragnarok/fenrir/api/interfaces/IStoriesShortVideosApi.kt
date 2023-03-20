package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiNarratives
import dev.ragnarok.fenrir.api.model.VKApiStory
import dev.ragnarok.fenrir.api.model.response.ShortVideosResponse
import dev.ragnarok.fenrir.api.model.response.StoryGetResponse
import dev.ragnarok.fenrir.api.model.response.StoryResponse
import dev.ragnarok.fenrir.api.model.response.ViewersListResponse
import dev.ragnarok.fenrir.api.model.server.VKApiStoryUploadServer
import io.reactivex.rxjava3.core.Single

interface IStoriesShortVideosApi {
    @CheckResult
    fun stories_getPhotoUploadServer(
        group_id: Long?,
        reply_to_story: String?
    ): Single<VKApiStoryUploadServer>

    @CheckResult
    fun stories_getVideoUploadServer(
        group_id: Long?,
        reply_to_story: String?
    ): Single<VKApiStoryUploadServer>

    @CheckResult
    fun getStoriesViewers(
        ownerId: Long?,
        storyId: Int?,
        offset: Int?,
        count: Int?
    ): Single<ViewersListResponse>

    @CheckResult
    fun stories_delete(owner_id: Long, story_id: Int): Single<Int>

    @CheckResult
    fun stories_save(upload_results: String?): Single<Items<VKApiStory>>

    @CheckResult
    fun getStory(owner_id: Long?, extended: Int?, fields: String?): Single<StoryResponse>

    @CheckResult
    fun getNarratives(owner_id: Long, offset: Int?, count: Int?): Single<Items<VKApiNarratives>>

    @CheckResult
    fun getStoryById(
        stories: List<AccessIdPair>,
        extended: Int?,
        fields: String?
    ): Single<StoryGetResponse>

    @CheckResult
    fun searchStory(
        q: String?,
        mentioned_id: Long?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<StoryResponse>

    @CheckResult
    fun getShortVideos(
        ownerId: Long?,
        startFrom: String?,
        count: Int?,
        extended: Int?,
        fields: String?
    ): Single<ShortVideosResponse>
}
