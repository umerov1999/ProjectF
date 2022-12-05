package dev.ragnarok.fenrir.api.model

object AttachmentsTokenCreator {
    fun ofDocument(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentToken("doc", id, ownerId, accessKey)
    }

    fun ofAudio(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentToken("audio", id, ownerId, accessKey)
    }

    fun ofLink(url: String?): IAttachmentToken {
        return LinkAttachmentToken(url)
    }

    fun ofArticle(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentToken("article", id, ownerId, accessKey)
    }

    fun ofStory(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentToken("story", id, ownerId, accessKey)
    }

    fun ofPhotoAlbum(id: Int, ownerId: Int): IAttachmentToken {
        return AttachmentToken("album", id, ownerId)
    }

    fun ofAudioPlaylist(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentToken("audio_playlist", id, ownerId, accessKey)
    }

    fun ofGraffity(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentToken("graffiti", id, ownerId, accessKey)
    }

    fun ofCall(initiator_id: Int, receiver_id: Int, state: String?, time: Long): IAttachmentToken {
        return AttachmentToken("call", initiator_id, receiver_id, state + "_" + time)
    }

    fun ofGeo(latitude: String?, longitude: String?): IAttachmentToken {
        return AttachmentToken("geo", latitude.hashCode(), longitude.hashCode())
    }

    fun ofPhoto(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentToken("photo", id, ownerId, accessKey)
    }

    fun ofPoll(id: Int, ownerId: Int): IAttachmentToken {
        return AttachmentToken("poll", id, ownerId)
    }

    fun ofWallReply(id: Int, ownerId: Int): IAttachmentToken {
        return AttachmentToken("wall_reply", id, ownerId)
    }

    fun ofPost(id: Int, ownerId: Int): IAttachmentToken {
        return AttachmentToken("wall", id, ownerId)
    }

    fun ofError(type: String?, data: String?): IAttachmentToken {
        return AttachmentToken("error", type.hashCode(), data.hashCode())
    }

    fun ofEvent(id: Int, button_text: String?): IAttachmentToken {
        return AttachmentToken("event", id, button_text.hashCode())
    }

    fun ofMarket(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentToken("market", id, ownerId, accessKey)
    }

    fun ofMarketAlbum(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentToken("market_album", id, ownerId, accessKey)
    }

    fun ofVideo(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentToken("video", id, ownerId, accessKey)
    }

    fun ofArtist(id: String?): IAttachmentToken {
        return AttachmentTokenString("artist", id)
    }
}