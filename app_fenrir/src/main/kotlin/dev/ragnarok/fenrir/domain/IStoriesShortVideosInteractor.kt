package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.model.Narratives
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.Video
import io.reactivex.rxjava3.core.Single

interface IStoriesShortVideosInteractor {
    fun getStoriesViewers(
        accountId: Long,
        ownerId: Long,
        storyId: Int,
        count: Int,
        offset: Int
    ): Single<List<Pair<Owner, Boolean>>>

    fun searchStories(accountId: Long, q: String?, mentioned_id: Long?): Single<List<Story>>
    fun getStories(accountId: Long, owner_id: Long?): Single<List<Story>>
    fun stories_delete(accountId: Long, owner_id: Long, story_id: Int): Single<Int>
    fun getNarratives(
        accountId: Long,
        owner_id: Long,
        offset: Int?,
        count: Int?
    ): Single<List<Narratives>>

    fun getStoryById(accountId: Long, stories: List<AccessIdPair>): Single<List<Story>>

    fun getShortVideos(
        accountId: Long,
        ownerId: Long?,
        startFrom: String?,
        count: Int?
    ): Single<Pair<List<Video>, String?>>
}
