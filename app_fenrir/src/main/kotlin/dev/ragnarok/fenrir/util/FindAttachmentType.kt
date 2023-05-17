package dev.ragnarok.fenrir.util

interface FindAttachmentType {
    companion object {
        const val TYPE_PHOTO = "photo"
        const val TYPE_VIDEO = "video"
        const val TYPE_AUDIO = "audio"
        const val TYPE_DOC = "doc"
        const val TYPE_POST_WITH_QUERY = "posts_query"
        const val TYPE_LINK = "link"
        const val TYPE_POST = "wall"
        const val TYPE_MULTI = "multi"
    }
}