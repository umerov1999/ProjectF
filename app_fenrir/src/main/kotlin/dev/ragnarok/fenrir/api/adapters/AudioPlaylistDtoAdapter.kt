package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiAudioPlaylist
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class AudioPlaylistDtoAdapter : AbsDtoAdapter<VKApiAudioPlaylist>("VKApiAudioPlaylist") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiAudioPlaylist {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val album = VKApiAudioPlaylist()
        val root = json.jsonObject
        album.id = optInt(root, "id")
        album.count = optInt(root, "count")
        album.owner_id = optLong(root, "owner_id")
        album.title = optString(root, "title")
        album.access_key = optString(root, "access_key")
        album.description = optString(root, "description")
        album.update_time = optLong(root, "update_time")
        album.Year = optInt(root, "year")
        if (hasArray(root, "genres")) {
            val build = StringBuilder()
            val gnr = root["genres"]?.jsonArray
            var isFirst = true
            for (i in gnr.orEmpty()) {
                if (!checkObject(i)) {
                    continue
                }
                if (isFirst) isFirst = false else build.append(", ")
                val value = optString(i.jsonObject, "name")
                if (value != null) build.append(value)
            }
            album.genre = build.toString()
        }
        if (hasObject(root, "original")) {
            val orig = root["original"]?.jsonObject
            album.original_id = optInt(orig, "playlist_id")
            album.original_owner_id = optLong(orig, "owner_id")
            album.original_access_key = optString(orig, "access_key")
        }
        if (hasArray(root, "main_artists")) {
            val artist = root["main_artists"]?.jsonArray?.get(0)
            if (checkObject(artist)) {
                album.artist_name = optString(artist.jsonObject, "name")
            }
        }
        if (hasObject(root, "photo")) {
            val thmb = root["photo"]?.jsonObject
            if (thmb.has("photo_600")) album.thumb_image =
                optString(thmb, "photo_600") else if (thmb.has("photo_300")) album.thumb_image =
                optString(thmb, "photo_300")
        } else if (hasArray(root, "thumbs")) {
            val thmbc = root["thumbs"]?.jsonArray?.get(0)
            if (checkObject(thmbc)) {
                if (thmbc.jsonObject.has("photo_600")) album.thumb_image = optString(
                    thmbc.jsonObject,
                    "photo_600"
                ) else if (thmbc.jsonObject.has("photo_300")) album.thumb_image =
                    optString(thmbc.jsonObject, "photo_300")
            }
        }
        return album
    }

    companion object {
        private val TAG = AudioPlaylistDtoAdapter::class.java.simpleName
    }
}