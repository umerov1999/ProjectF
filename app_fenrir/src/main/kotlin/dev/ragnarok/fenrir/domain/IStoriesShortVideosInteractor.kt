package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.model.Narratives
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.Story
import dev.ragnarok.fenrir.model.Video
import kotlinx.coroutines.flow.Flow

interface IStoriesShortVideosInteractor {
    fun getStoriesViewers(
        accountId: Long,
        ownerId: Long,
        storyId: Int,
        count: Int,
        offset: Int
    ): Flow<List<Pair<Owner, Boolean>>>

    fun searchStories(accountId: Long, q: String?, mentioned_id: Long?): Flow<List<Story>>
    fun getStories(accountId: Long, owner_id: Long?): Flow<List<Story>>
    fun stories_delete(accountId: Long, owner_id: Long, story_id: Int): Flow<Int>
    fun getNarratives(
        accountId: Long,
        owner_id: Long,
        offset: Int?,
        count: Int?
    ): Flow<List<Narratives>>

    fun getStoryById(accountId: Long, stories: List<AccessIdPair>): Flow<List<Story>>

    fun getShortVideos(
        accountId: Long,
        ownerId: Long?,
        startFrom: String?,
        count: Int?
    ): Flow<Pair<List<Video>, String?>>

    fun subscribe(accountId: Long, owner_id: Long): Flow<Int>
    fun unsubscribe(accountId: Long, owner_id: Long): Flow<Int>
}
