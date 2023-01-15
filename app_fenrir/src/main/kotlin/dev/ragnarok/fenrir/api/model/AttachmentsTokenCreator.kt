package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken

object AttachmentsTokenCreator {
    fun ofDocument(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("doc", id, ownerId, accessKey)
    }

    fun ofAudio(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("audio", id, ownerId, accessKey)
    }

    fun ofLink(url: String?): IAttachmentToken {
        return AttachmentTokens.LinkAttachmentToken(url)
    }

    fun ofArticle(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("article", id, ownerId, accessKey)
    }

    fun ofStory(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("story", id, ownerId, accessKey)
    }

    fun ofPhotoAlbum(id: Int, ownerId: Int): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("album", id, ownerId)
    }

    fun ofAudioPlaylist(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("audio_playlist", id, ownerId, accessKey)
    }

    fun ofGraffiti(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("graffiti", id, ownerId, accessKey)
    }

    fun ofCall(
        initiator_id: Int,
        receiver_id: Int,
        state: String?,
        time: Long
    ): IAttachmentToken {
        return AttachmentTokens.AttachmentTokenStringSpecial(
            "call",
            initiator_id.toString(),
            receiver_id.toString() + "_{$state}_$time"
        )
    }

    fun ofGeo(latitude: String?, longitude: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("geo", latitude.hashCode(), longitude.hashCode())
    }

    fun ofPhoto(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("photo", id, ownerId, accessKey)
    }

    fun ofPoll(id: Int, ownerId: Int): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("poll", id, ownerId)
    }

    fun ofWallReply(id: Int, ownerId: Int): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("wall_reply", id, ownerId)
    }

    fun ofPost(id: Int, ownerId: Int): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("wall", id, ownerId)
    }

    fun ofError(type: String?, data: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("error", type.hashCode(), data.hashCode())
    }

    fun ofEvent(id: Int, button_text: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("event", id, button_text.hashCode())
    }

    fun ofMarket(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("market", id, ownerId, accessKey)
    }

    fun ofMarketAlbum(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("market_album", id, ownerId, accessKey)
    }

    fun ofVideo(id: Int, ownerId: Int, accessKey: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentToken("video", id, ownerId, accessKey)
    }

    fun ofArtist(id: String?): IAttachmentToken {
        return AttachmentTokens.AttachmentTokenForArtist("artist", id)
    }
}
