package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.DocsEntryDtoAdapter
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiDoc : VKApiAttachment {
    @SerialName("id")
    var id = 0

    @SerialName("owner_id")
    var ownerId = 0L

    @SerialName("title")
    var title: String? = null

    @SerialName("size")
    var size: Long = 0

    @SerialName("ext")
    var ext: String? = null

    @SerialName("url")
    var url: String? = null

    @SerialName("date")
    var date: Long = 0

    @SerialName("type")
    var type = 0

    @SerialName("preview")
    var preview: Preview? = null

    @SerialName("access_key")
    var accessKey: String? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_DOC
    }

    @Serializable(with = DocsEntryDtoAdapter::class)
    class Entry(val type: String, val doc: VKApiDoc)

    @Serializable
    class Preview {
        @SerialName("photo")
        var photo: Photo? = null

        @SerialName("video")
        var video: Video? = null

        @SerialName("graffiti")
        var graffiti: Graffiti? = null
    }

    @Serializable
    class Graffiti {
        @SerialName("src")
        var src: String? = null

        @SerialName("width")
        var width = 0

        @SerialName("height")
        var height = 0
    }

    @Serializable
    class Photo {
        @SerialName("sizes")
        var sizes: List<PhotoSizeDto>? = null
    }

    @Serializable
    class Video {
        @SerialName("src")
        var src: String? = null

        @SerialName("width")
        var width = 0

        @SerialName("height")
        var height = 0

        @SerialName("file_size")
        var fileSize: Long = 0
    }
}