package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.PhotoSizeDto
import dev.ragnarok.fenrir.api.model.VKApiPhotoAlbum
import dev.ragnarok.fenrir.api.model.VKApiPrivacy
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

class PhotoAlbumDtoAdapter : AbsAdapter<VKApiPhotoAlbum>("VKApiPhotoAlbum") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiPhotoAlbum {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val album = VKApiPhotoAlbum()
        val root = json.asJsonObject
        album.id = optInt(root, "id")
        album.thumb_id = optInt(root, "thumb_id")
        album.owner_id = optInt(root, "owner_id")
        album.title = optString(root, "title")
        album.description = optString(root, "description")
        album.created = optLong(root, "created")
        album.updated = optLong(root, "updated")
        album.size = optInt(root, "size")
        album.can_upload = optInt(root, "can_upload") == 1
        album.thumb_src = optString(root, "thumb_src")
        if (hasObject(root, "privacy_view")) {
            album.privacy_view =
                root["privacy_view"]?.let {
                    kJson.decodeFromJsonElement(VKApiPrivacy.serializer(), it)
                }
        }
        if (hasObject(root, "privacy_comment")) {
            album.privacy_comment =
                root["privacy_comment"]?.let {
                    kJson.decodeFromJsonElement(VKApiPrivacy.serializer(), it)
                }
        }
        if (hasArray(root, "sizes")) {
            val sizesArray = root.getAsJsonArray("sizes")
            album.photo = ArrayList(sizesArray?.size.orZero())
            for (i in 0 until sizesArray?.size.orZero()) {
                album.photo?.add(
                    kJson.decodeFromJsonElement(
                        PhotoSizeDto.serializer(),
                        sizesArray?.get(i) ?: continue
                    )
                )
            }
        } else if (hasObject(root, "thumb")) {
            val thumb = root.getAsJsonObject("thumb")
            if (hasArray(thumb, "sizes")) {
                val sizesArray = thumb.getAsJsonArray("sizes")
                album.photo = ArrayList(sizesArray?.size.orZero())
                for (i in 0 until sizesArray?.size.orZero()) {
                    album.photo?.add(
                        kJson.decodeFromJsonElement(
                            PhotoSizeDto.serializer(),
                            sizesArray?.get(i) ?: continue
                        )
                    )
                }
            } else {
                album.photo = ArrayList(3)
                album.photo?.add(
                    PhotoSizeDto.create(
                        PhotoSizeDto.Type.S,
                        "http://vk.com/images/s_noalbum.png"
                    )
                )
                album.photo?.add(
                    PhotoSizeDto.create(
                        PhotoSizeDto.Type.M,
                        "http://vk.com/images/m_noalbum.png"
                    )
                )
                album.photo?.add(
                    PhotoSizeDto.create(
                        PhotoSizeDto.Type.X,
                        "http://vk.com/images/x_noalbum.png"
                    )
                )
            }
        } else {
            album.photo = ArrayList(3)
            album.photo?.add(
                PhotoSizeDto.create(
                    PhotoSizeDto.Type.S,
                    "http://vk.com/images/s_noalbum.png"
                )
            )
            album.photo?.add(
                PhotoSizeDto.create(
                    PhotoSizeDto.Type.M,
                    "http://vk.com/images/m_noalbum.png"
                )
            )
            album.photo?.add(
                PhotoSizeDto.create(
                    PhotoSizeDto.Type.X,
                    "http://vk.com/images/x_noalbum.png"
                )
            )
        }
        album.upload_by_admins_only = optBoolean(root, "upload_by_admins_only")
        album.comments_disabled = optBoolean(root, "comments_disabled")
        return album
    }

    companion object {
        private val TAG = PhotoAlbumDtoAdapter::class.java.simpleName
    }
}