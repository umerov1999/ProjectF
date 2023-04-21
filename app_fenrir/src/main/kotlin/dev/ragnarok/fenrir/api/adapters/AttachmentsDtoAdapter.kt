package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.JsonObject
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class AttachmentsDtoAdapter : AbsDtoAdapter<VKApiAttachments>("VKApiAttachments") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiAttachments {
        if (!checkArray(json)) {
            return VKApiAttachments()
        }
        val array = json.jsonArray
        val dto = VKApiAttachments(array.size)
        for (i in 0 until array.size) {
            if (!checkObject(array[i])) {
                continue
            }
            val o = array[i].jsonObject
            val type = optString(o, "type")
            val attachment: VKApiAttachment? = try {
                parse(type, o)
            } catch (e: Exception) {
                e.printStackTrace()
                continue
            }
            if (attachment != null && type.nonNullNoEmpty()) {
                dto.entries.add(VKApiAttachments.Entry(type, attachment))
            }
        }
        return dto
    }

    companion object {
        fun parse(
            type: String?,
            root: JsonObject
        ): VKApiAttachment? {
            type ?: return null
            val o = root[type]

            //{"type":"photos_list","photos_list":["406536042_456239026"]}
            return when {
                VKApiAttachment.TYPE_PHOTO == type -> {
                    kJson.decodeFromJsonElement(VKApiPhoto.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_VIDEO == type -> {
                    kJson.decodeFromJsonElement(VKApiVideo.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_AUDIO == type -> {
                    kJson.decodeFromJsonElement(VKApiAudio.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_DOC == type -> {
                    val doc: VKApiDoc =
                        kJson.decodeFromJsonElement(VKApiDoc.serializer(), o ?: return null)
                    if ("lottie" == doc.ext) {
                        val sticker = VKApiSticker()
                        sticker.sticker_id = doc.id
                        sticker.animation_url = doc.url
                        return sticker
                    }
                    doc
                }

                VKApiAttachment.TYPE_POST == type || VKApiAttachment.TYPE_FAVE_POST == type -> {
                    kJson.decodeFromJsonElement(VKApiPost.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_LINK == type -> {
                    kJson.decodeFromJsonElement(VKApiLink.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_ARTICLE == type -> {
                    kJson.decodeFromJsonElement(VKApiArticle.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_POLL == type -> {
                    kJson.decodeFromJsonElement(VKApiPoll.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_WIKI_PAGE == type -> {
                    kJson.decodeFromJsonElement(VKApiWikiPage.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_ALBUM == type -> {
                    kJson.decodeFromJsonElement(VKApiPhotoAlbum.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_STICKER == type -> {
                    kJson.decodeFromJsonElement(VKApiSticker.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_AUDIO_MESSAGE == type -> {
                    kJson.decodeFromJsonElement(VKApiAudioMessage.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_GIFT == type -> {
                    kJson.decodeFromJsonElement(VKApiGiftItem.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_GRAFFITI == type -> {
                    kJson.decodeFromJsonElement(VKApiGraffiti.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_STORY == type -> {
                    kJson.decodeFromJsonElement(VKApiStory.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_CALL == type -> {
                    kJson.decodeFromJsonElement(VKApiCall.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_GEO == type -> {
                    kJson.decodeFromJsonElement(VKApiGeo.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_AUDIO_PLAYLIST == type -> {
                    kJson.decodeFromJsonElement(VKApiAudioPlaylist.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_WALL_REPLY == type -> {
                    kJson.decodeFromJsonElement(VKApiWallReply.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_EVENT == type -> {
                    kJson.decodeFromJsonElement(VKApiEvent.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_MARKET_ALBUM == type -> {
                    kJson.decodeFromJsonElement(VKApiMarketAlbum.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_ARTIST == type -> {
                    kJson.decodeFromJsonElement(VKApiAudioArtist.serializer(), o ?: return null)
                }

                VKApiAttachment.TYPE_MARKET == type || VKApiAttachment.TYPE_PRODUCT == type -> {
                    kJson.decodeFromJsonElement(VKApiMarket.serializer(), o ?: return null)
                }

                !Utils.isValueAssigned(
                    type,
                    VKApiAttachment.IGNORE_ATTACHMENTS
                ) -> {
                    VKApiNotSupported(type, o.toString())
                }

                else -> {
                    null
                }
            }
        }
    }
}