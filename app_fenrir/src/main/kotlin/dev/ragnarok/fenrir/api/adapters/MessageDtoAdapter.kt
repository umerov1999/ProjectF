package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import dev.ragnarok.fenrir.api.util.VKStringUtils
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class MessageDtoAdapter : AbsDtoAdapter<VKApiMessage>("VKApiMessage") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiMessage {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiMessage()
        val root = json.jsonObject
        dto.id = optInt(root, "id")
        dto.out = optBoolean(root, "out")
        dto.peer_id = optLong(root, "peer_id")
        dto.from_id =
            if (root.has("from_id")) optLong(root, "from_id") else optLong(root, "user_id")
        dto.date = optLong(root, "date")
        //dto.read_state = optBoolean(root, "read_state");
        //dto.title = VKStringUtils.unescape(optString(root, "title"));
        dto.body = VKStringUtils.unescape(
            if (root.has("text")) optString(
                root,
                "text"
            ) else optString(root, "body")
        )
        if (hasObject(root, "keyboard")) {
            dto.keyboard = root["keyboard"]?.let {
                kJson.decodeFromJsonElement(VKApiConversation.CurrentKeyboard.serializer(), it)
            }
        }
        if (hasArray(root, "attachments")) {
            dto.attachments =
                root["attachments"]?.let {
                    kJson.decodeFromJsonElement(VKApiAttachments.serializer(), it)
                }
        }
        if (hasArray(root, "fwd_messages")) {
            val fwdArray = root["fwd_messages"]?.jsonArray
            dto.fwd_messages = ArrayList(fwdArray?.size.orZero())
            for (i in 0 until fwdArray?.size.orZero()) {
                if (!checkObject(fwdArray?.get(i))) {
                    continue
                }
                dto.fwd_messages?.add(deserialize(fwdArray?.get(i) ?: continue))
            }
        }
        if (hasObject(root, "reply_message")) {
            if (dto.fwd_messages == null) {
                dto.fwd_messages = ArrayList(1)
            }
            root["reply_message"]?.let { deserialize(it) }?.let {
                dto.fwd_messages?.add(
                    it
                )
            }
        }
        dto.deleted = optBoolean(root, "deleted")
        dto.important = optBoolean(root, "important")
        dto.was_listened = optBoolean(root, "was_listened")

        if (dto.was_listened) {
            for (i in dto.attachments?.entries.orEmpty()) {
                if (i.type == VKApiAttachment.TYPE_AUDIO_MESSAGE && i.attachment is VKApiAudioMessage) {
                    i.attachment.was_listened = true
                }
            }
        }

        dto.random_id = optString(root, "random_id")
        dto.payload = optString(root, "payload")
        dto.update_time = optLong(root, "update_time")
        dto.conversation_message_id = optInt(root, "conversation_message_id")
        val actionJson = root["action"]
        if (checkObject(actionJson)) {
            dto.action = optString(actionJson.jsonObject, "type")
            dto.action_mid = optLong(actionJson.jsonObject, "member_id")
            dto.action_text = optString(actionJson.jsonObject, "text")
            dto.action_email = optString(actionJson.jsonObject, "email")
            if (hasObject(actionJson.jsonObject, "photo")) {
                val photoJson = actionJson.jsonObject["photo"]?.jsonObject
                dto.action_photo_50 = optString(photoJson, "photo_50")
                dto.action_photo_100 = optString(photoJson, "photo_100")
                dto.action_photo_200 = optString(photoJson, "photo_200")
            }
        }
        return dto
    }

    companion object {
        private val TAG = MessageDtoAdapter::class.java.simpleName
    }
}