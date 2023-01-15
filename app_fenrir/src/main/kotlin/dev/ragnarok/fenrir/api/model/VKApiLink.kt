package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.Serializable

/**
 * A link object describes a link attachment
 */
@Serializable
class VKApiLink : VKApiAttachment {
    /**
     * Link URL
     */
    var url: String? = null

    /**
     * Link title
     */
    var title: String? = null

    /**
     * Link description;
     */
    var description: String? = null

    /**
     * Image preview URL for the link (if any).
     */
    var photo: VKApiPhoto? = null

    /**
     * ID wiki page with content for the preview of the page contents
     * ID is returned as "ownerid_pageid".
     */
    var preview_page: String? = null

    /* адрес страницы для предпросмотра содержимого страницы. */
    var preview_url: String? = null
    var preview_photo: String? = null
    var caption: String? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_LINK
    }
}