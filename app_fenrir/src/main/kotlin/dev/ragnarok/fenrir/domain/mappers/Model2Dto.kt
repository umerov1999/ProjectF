package dev.ragnarok.fenrir.domain.mappers

import dev.ragnarok.fenrir.api.model.AttachmentsTokenCreator
import dev.ragnarok.fenrir.api.model.IAttachmentToken
import dev.ragnarok.fenrir.model.*

object Model2Dto {
    fun createTokens(models: Collection<AbsModel>?): List<IAttachmentToken> {
        return MapUtil.mapAll(models) { createToken(it) }
    }

    private fun createToken(model: AbsModel): IAttachmentToken {
        when (model) {
            is Document -> {
                return AttachmentsTokenCreator.ofDocument(
                    model.id,
                    model.ownerId,
                    model.accessKey
                )
            }
            is Audio -> {
                return AttachmentsTokenCreator.ofAudio(model.id, model.ownerId, model.accessKey)
            }
            is Link -> {
                return AttachmentsTokenCreator.ofLink(model.url)
            }
            is Photo -> {
                return AttachmentsTokenCreator.ofPhoto(
                    model.getObjectId(),
                    model.ownerId,
                    model.accessKey
                )
            }
            is Poll -> {
                return AttachmentsTokenCreator.ofPoll(model.id, model.ownerId)
            }
            is Post -> {
                return AttachmentsTokenCreator.ofPost(model.vkid, model.ownerId)
            }
            is Video -> {
                return AttachmentsTokenCreator.ofVideo(model.id, model.ownerId, model.accessKey)
            }
            is Article -> {
                return AttachmentsTokenCreator.ofArticle(model.id, model.ownerId, model.accessKey)
            }
            is Story -> {
                return AttachmentsTokenCreator.ofStory(model.id, model.ownerId, model.accessKey)
            }
            is Graffiti -> {
                return AttachmentsTokenCreator.ofGraffity(
                    model.id,
                    model.owner_id,
                    model.access_key
                )
            }
            is PhotoAlbum -> {
                return AttachmentsTokenCreator.ofPhotoAlbum(model.getObjectId(), model.ownerId)
            }
            is Call -> {
                return AttachmentsTokenCreator.ofCall(
                    model.initiator_id,
                    model.receiver_id,
                    model.state,
                    model.time
                )
            }
            is Geo -> {
                return AttachmentsTokenCreator.ofGeo(
                    model.latitude,
                    model.longitude
                )
            }
            is AudioArtist -> {
                return AttachmentsTokenCreator.ofArtist(model.getId())
            }
            is WallReply -> {
                return AttachmentsTokenCreator.ofWallReply(model.getObjectId(), model.ownerId)
            }
            is NotSupported -> {
                return AttachmentsTokenCreator.ofError(model.getType(), model.getBody())
            }
            is Event -> {
                return AttachmentsTokenCreator.ofEvent(model.id, model.button_text)
            }
            is Market -> {
                return AttachmentsTokenCreator.ofMarket(model.id, model.owner_id, model.access_key)
            }
            is MarketAlbum -> {
                return AttachmentsTokenCreator.ofMarketAlbum(
                    model.getId(),
                    model.getOwner_id(),
                    model.getAccess_key()
                )
            }
            is AudioPlaylist -> {
                return if (model.getOriginal_access_key()
                        .isNullOrEmpty() || model.getOriginal_id() == 0 || model.getOriginal_owner_id() == 0
                ) {
                    AttachmentsTokenCreator.ofAudioPlaylist(
                        model.getId(),
                        model.getOwnerId(),
                        model.getAccess_key()
                    )
                } else {
                    AttachmentsTokenCreator.ofAudioPlaylist(
                        model.getOriginal_id(),
                        model.getOriginal_owner_id(),
                        model.getOriginal_access_key()
                    )
                }
            }
            else -> throw UnsupportedOperationException("Token for class " + model.javaClass + " not supported")
        }
    }
}