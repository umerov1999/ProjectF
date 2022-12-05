package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.NewsAdapter
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import kotlinx.serialization.Serializable

@Serializable(with = NewsAdapter::class)
class VKApiNews {
    var type //friends_recomm //post
            : String? = null
    var source_id = 0
    var date: Long = 0
    var post_id = 0
    var post_type: String? = null
    var final_post = false
    var copy_owner_id = 0
    var copy_post_id = 0
    var copy_history: List<VKApiPost>? = null
    var copy_post_date: Long = 0
    var text: String? = null
    var can_edit = false
    var can_delete = false
    var comment_count = 0
    var comment_can_post = false
    var like_count = 0
    var user_like = false
    var can_like = false
    var can_publish = false
    var reposts_count = 0
    var mark_as_ads = 0
    var user_reposted = false
    var copyright: Copyright? = null

    /**
     * Information about attachments to the post (photos, links, etc.), if any;
     */
    var attachments: VKApiAttachments? = null
    var friends: ArrayList<Int>? = null
    var views = 0
    val attachmentsCount: Int
        get() = attachments?.size().orZero()

    fun hasAttachments(): Boolean {
        return attachmentsCount > 0
    }

    fun hasCopyHistory(): Boolean {
        return copy_history.nonNullNoEmpty()
    }

    val isOnlyRepost: Boolean
        get() = copy_history.nonNullNoEmpty() && attachmentsCount == 0 && text.isNullOrEmpty()

    fun stripRepost() {
        copy_history = null
    }

    class Copyright(val name: String, val link: String?)
}