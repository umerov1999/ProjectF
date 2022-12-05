package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiAudioPlaylist
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class AudioPlaylistDtoAdapter : AbsAdapter<VKApiAudioPlaylist>("VKApiAudioPlaylist") {
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
        album.owner_id = optInt(root, "owner_id")
        album.title = optString(root, "title")
        album.access_key = optString(root, "access_key")
        album.description = optString(root, "description")
        album.update_time = optInt(root, "update_time").toLong()
        album.Year = optInt(root, "year")
        if (hasArray(root, "genres")) {
            val build = StringBuilder()
            val gnr = root.getAsJsonArray("genres")
            var isFirst = true
            for (i in gnr.orEmpty()) {
                if (!checkObject(i)) {
                    continue
                }
                if (isFirst) isFirst = false else build.append(", ")
                val `val` = optString(i.asJsonObject, "name")
                if (`val` != null) build.append(`val`)
            }
            album.genre = build.toString()
        }
        if (hasObject(root, "original")) {
            val orig = root.getAsJsonObject("original")
            album.original_id = optInt(orig, "playlist_id")
            album.original_owner_id = optInt(orig, "owner_id")
            album.original_access_key = optString(orig, "access_key")
        }
        if (hasArray(root, "main_artists")) {
            val artist = root.getAsJsonArray("main_artists")?.get(0)
            if (checkObject(artist)) {
                album.artist_name = optString(artist.asJsonObject, "name")
            }
        }
        if (hasObject(root, "photo")) {
            val thmb = root.getAsJsonObject("photo")
            if (thmb.has("photo_600")) album.thumb_image =
                optString(thmb, "photo_600") else if (thmb.has("photo_300")) album.thumb_image =
                optString(thmb, "photo_300")
        } else if (hasArray(root, "thumbs")) {
            val thmbc = root.getAsJsonArray("thumbs")?.get(0)
            if (checkObject(thmbc)) {
                if (thmbc.asJsonObject.has("photo_600")) album.thumb_image = optString(
                    thmbc.asJsonObject,
                    "photo_600"
                ) else if (thmbc.asJsonObject.has("photo_300")) album.thumb_image =
                    optString(thmbc.asJsonObject, "photo_300")
            }
        }
        return album
    }

    companion object {
        private val TAG = AudioPlaylistDtoAdapter::class.java.simpleName
    }
}