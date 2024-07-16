package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.domain.ICountersInteractor
import dev.ragnarok.fenrir.model.SectionCounters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CountersInteractor(private val networker: INetworker) : ICountersInteractor {
    override fun getApiCounters(accountId: Long): Flow<SectionCounters> {
        return networker.vkDefault(accountId)
            .account()
            .getCounters("friends,messages,photos,videos,gifts,events,groups,notifications")
            .map { dto ->
                SectionCounters()
                    .setFriends(dto.friends)
                    .setMessages(dto.messages)
                    .setPhotos(dto.photos)
                    .setVideos(dto.videos)
                    .setGifts(dto.gifts)
                    .setEvents(dto.events)
                    .setNotes(dto.notes)
                    .setGroups(dto.groups)
                    .setNotifications(dto.notifications)
            }
    }
}