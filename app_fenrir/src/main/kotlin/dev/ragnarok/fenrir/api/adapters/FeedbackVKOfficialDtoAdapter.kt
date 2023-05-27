package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.domain.mappers.Dto2Model
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.link.LinkParser
import dev.ragnarok.fenrir.model.FeedbackVKOfficial
import dev.ragnarok.fenrir.model.FeedbackVKOfficial.*
import dev.ragnarok.fenrir.model.FeedbackVKOfficialList
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.serializeble.json.*
import kotlinx.serialization.builtins.ListSerializer

class FeedbackVKOfficialDtoAdapter :
    AbsDtoAdapter<FeedbackVKOfficialList>("FeedbackVKOfficialList") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): FeedbackVKOfficialList {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dtolist = FeedbackVKOfficialList()
        val root = json.jsonObject
        dtolist.items = ArrayList()

        val profiles: List<VKApiUser> =
            if (hasArray(root, "profiles")) kJson.decodeFromJsonElementOrNull(
                ListSerializer(VKApiUser.serializer()), root["profiles"]
            ).orEmpty() else emptyList()
        val groups: List<VKApiCommunity> =
            if (hasArray(root, "groups")) kJson.decodeFromJsonElementOrNull(
                ListSerializer(VKApiCommunity.serializer()), root["groups"]
            ).orEmpty() else emptyList()

        val photos: MutableList<VKApiPhoto> = ArrayList()
        if (hasArray(root, "photos")) {
            val temp = root["photos"]?.jsonArray
            for (i in temp.orEmpty()) {
                if (!checkObject(i)) {
                    continue
                }
                photos.add(kJson.decodeFromJsonElement(VKApiPhoto.serializer(), i))
            }
        }
        if (!hasArray(root, "items")) return dtolist
        for (i in root["items"]?.jsonArray.orEmpty()) {
            if (!checkObject(i)) {
                continue
            }
            val root_item = i.jsonObject
            val dto = FeedbackVKOfficial()
            if (hasObject(root_item, "action")) {
                val action_item = root_item["action"]?.jsonObject
                if ("authorize" == optString(action_item, "type")) {
                    dto.action = ActionBrowserURL(optString(action_item, "url"))
                } else if ("custom" == optString(
                        action_item,
                        "type"
                    ) && optString(
                        root_item,
                        "icon_type"
                    ) == "friend_found"
                ) {
                    dto.action = ActionURL(optString(action_item, "url"))
                } else if ("message_open" == optString(
                        action_item,
                        "type"
                    ) && hasObject(action_item, "context")
                ) {
                    val context_item = action_item["context"]?.jsonObject
                    dto.action = ActionMessage(
                        optLong(context_item, "peer_id", 0),
                        optInt(context_item, "id", 0)
                    )
                }
            }
            try {
                if (hasObject(root_item, "action_buttons")) {
                    val action_buttons = root_item["action_buttons"]?.jsonObject
                    for (ss1 in action_buttons.orEmpty().keys) {
                        if (checkArray(action_buttons?.get(ss1))) {
                            for (ss2 in action_buttons?.get(ss1)?.jsonArray.orEmpty()) {
                                if (checkObject(ss2)) {
                                    val act = ss2.jsonObject
                                    if (hasObject(act, "action")) {
                                        val actu = act["action"]?.jsonObject
                                        if ("hide_item" == optString(
                                                actu,
                                                "type"
                                            ) && hasObject(actu, "context")
                                        ) {
                                            val actctx = actu["context"]?.jsonObject
                                            dto.hide_query = optString(actctx, "query")
                                            if (dto.hide_query.nonNullNoEmpty()) {
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
            dto.iconType = optString(root_item, "icon_type")
            dto.header = optString(root_item, "header")
            dto.header.nonNullNoEmpty {
                dto.header = it.replace("{date}", "")
                    .replace("'''(((?!''').)*)'''".toRegex(), "<b>$1</b>")
                    .replace("\\[vk(ontakte)?://[A-Za-z\\d/?=]+\\|([^]]+)]".toRegex(), "$2")

                val matcher = LinkParser.MENTIONS_AVATAR_PATTERN.matcher(it)
                if (matcher.find()) {
                    val Type = matcher.group(1)
                    matcher.group(2)?.toLong()?.let { lit ->
                        dto.header_owner_id =
                            if (Type == "event" || Type == "club" || Type == "public") -lit else lit
                        if (dto.header_owner_id.orZero() >= 0) {
                            for (n in profiles) {
                                if (n.id == dto.header_owner_id) {
                                    dto.header_owner_avatar_url =
                                        Utils.firstNonEmptyString(n.photo_200, n.photo_100)
                                    break
                                }
                            }
                        } else {
                            for (n in groups) {
                                if (-n.id == dto.header_owner_id) {
                                    dto.header_owner_avatar_url =
                                        Utils.firstNonEmptyString(n.photo_200, n.photo_100)
                                    break
                                }
                            }
                        }
                    }
                }
            }

            dto.text = optString(root_item, "text")
            dto.text.nonNullNoEmpty {
                dto.text =
                    it.replace("{date}", "")
                        .replace("'''(((?!''').)*)'''".toRegex(), "<b>$1</b>")
                        .replace("\\[vk(ontakte)?://[A-Za-z\\d/?=]+\\|([^]]+)]".toRegex(), "$2")
            }
            dto.footer = optString(root_item, "footer")
            dto.footer.nonNullNoEmpty {
                dto.footer = it.replace("{date}", "")
                    .replace("'''(((?!''').)*)'''".toRegex(), "<b>$1</b>")
                    .replace("\\[vk(ontakte)?://[A-Za-z\\d/?=]+\\|([^]]+)]".toRegex(), "$2")
            }
            dto.time = optLong(root_item, "date")
            dto.iconURL = optString(root_item, "icon_url")
            val attachments: MutableList<Attachment> = ArrayList()
            if (hasObject(root_item, "main_item")) {
                val main_item = root_item["main_item"]?.jsonObject
                if (hasArray(main_item, "image_object")) {
                    val jsonPhotos2 = main_item["image_object"]?.jsonArray
                    dto.iconURL =
                        jsonPhotos2?.get(jsonPhotos2.size - 1)?.asJsonObjectSafe?.get("url")?.asPrimitiveSafe?.content
                }
                if ("photo" == optString(main_item, "type")) {
                    attachments.add(
                        kJson.decodeFromJsonElement(
                            Attachment.serializer(),
                            main_item!!
                        )
                    )
                }
            }
            if (hasObject(root_item, "additional_item")) {
                val additional_item = root_item["additional_item"]?.jsonObject
                if (hasArray(additional_item, "image_object")) {
                    val arrt = additional_item["image_object"]?.jsonArray
                    dto.images = ArrayList()
                    for (s in arrt.orEmpty()) {
                        if (!checkObject(s)) {
                            continue
                        }
                        val imgh: ImageAdditional =
                            kJson.decodeFromJsonElement(ImageAdditional.serializer(), s)
                        dto.images?.add(imgh)
                    }

                    if (hasObject(additional_item, "action")) {
                        val action_item = additional_item["action"]?.jsonObject
                        if ("custom" == optString(action_item, "type")) {
                            dto.images_action = ActionURL(optString(action_item, "url"))
                        }
                    }
                }
                if ("photo" == optString(additional_item, "type")) {
                    attachments.add(
                        kJson.decodeFromJsonElement(
                            Attachment.serializer(),
                            additional_item!!
                        )
                    )
                }
            }
            if (hasArray(root_item, "attachments")) {
                val temp = root_item["attachments"]?.jsonArray
                for (a in temp.orEmpty()) {
                    if (!checkObject(a)) {
                        continue
                    }
                    attachments.add(kJson.decodeFromJsonElement(Attachment.serializer(), a))
                }
            }
            for (s in attachments) {
                if (s.type.isNullOrEmpty() || s.object_id.isNullOrEmpty() || s.type != "photo") {
                    continue
                }
                for (v in photos) {
                    if (v.owner_id.toString() + "_" + v.id == s.object_id) {
                        if (dto.attachments == null) {
                            dto.attachments = ArrayList()
                        }
                        dto.attachments?.add(Dto2Model.transform(v))
                        break
                    }
                }
            }
            dtolist.items?.add(dto)
        }
        return dtolist
    }

    companion object {
        private val TAG = FeedbackVKOfficialDtoAdapter::class.java.simpleName
    }
}