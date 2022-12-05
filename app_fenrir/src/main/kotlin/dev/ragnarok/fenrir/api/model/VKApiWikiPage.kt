package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

/**
 * Describes a Wiki page.
 */
@Serializable
class VKApiWikiPage : VKApiAttachment {
    /**
     * Wiki page ID;
     */
    var id = 0

    /**
     * ID of the group the wiki page belongs to;
     */
    var owner_id = 0

    /**
     * ID of the page creator.
     */
    var creator_id = 0

    /**
     * Wiki page name.
     */
    var title: String? = null

    /**
     * Text of the wiki page.
     */
    var source: String? = null

    /**
     * Whether a user can edit page text (false — cannot, true — can).
     */
    var current_user_can_edit = false

    /**
     * Whether a user can edit page access permissions (false — cannot, true — can).
     */
    var current_user_can_edit_access = false

    /**
     * Who can view the wiki page(0 — only community managers, 1 — only community members, 2 — all users).
     */
    var who_can_view = 0

    /**
     * Who can edit the wiki page (0 — only community managers, 1 — only community members, 2 — all users).
     */
    var who_can_edit = 0

    /**
     * ID of the last user who edited the page.
     */
    var editor_id = 0

    /**
     * Date of the last change.
     */
    var edited: Long = 0

    /**
     * Page creation date.
     */
    var created: Long = 0

    /**
     * Title of the parent page for navigation, if any.
     */
    var parent: String? = null

    /**
     * Title of the second parent page for navigation, if any.
     */
    var parent2: String? = null
    var views = 0
    var html: String? = null
    var view_url: String? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_WIKI_PAGE
    }
}