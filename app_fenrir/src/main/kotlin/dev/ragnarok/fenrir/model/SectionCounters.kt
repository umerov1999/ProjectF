package dev.ragnarok.fenrir.model

class SectionCounters {
    var friends = 0
        private set
    var messages = 0
    var photos = 0
        private set
    var videos = 0
        private set
    var gifts = 0
        private set
    var events = 0
        private set
    var notes = 0
        private set
    var groups = 0
        private set
    var notifications = 0
        private set

    fun setFriends(friends: Int): SectionCounters {
        this.friends = friends
        return this
    }

    fun setMessages(messages: Int): SectionCounters {
        this.messages = messages
        return this
    }

    fun setPhotos(photos: Int): SectionCounters {
        this.photos = photos
        return this
    }

    fun setVideos(videos: Int): SectionCounters {
        this.videos = videos
        return this
    }

    fun setGifts(gifts: Int): SectionCounters {
        this.gifts = gifts
        return this
    }

    fun setEvents(events: Int): SectionCounters {
        this.events = events
        return this
    }

    fun setNotes(notes: Int): SectionCounters {
        this.notes = notes
        return this
    }

    fun setGroups(groups: Int): SectionCounters {
        this.groups = groups
        return this
    }

    fun setNotifications(notifications: Int): SectionCounters {
        this.notifications = notifications
        return this
    }
}