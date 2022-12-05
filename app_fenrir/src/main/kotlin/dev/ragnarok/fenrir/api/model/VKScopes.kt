package dev.ragnarok.fenrir.api.model

/**
 * Application Access Permissions
 *
 * @see [http://vk.com/dev/permissions](http://vk.com/dev/permissions)
 */
object VKScopes {
    /**
     * User allowed to send notifications to him/her.
     */
    const val NOTIFY = "notify"

    /**
     * Access to friends.
     */
    const val FRIENDS = "friends"

    /**
     * Access to photos.
     */
    const val PHOTOS = "photos"

    /**
     * Access to audios.
     */
    const val AUDIO = "audio"

    /**
     * Access to videos.
     */
    const val VIDEO = "video"

    /**
     * Access to documents.
     */
    const val DOCS = "docs"

    /**
     * Access to user notes.
     */
    const val NOTES = "notes"

    /**
     * Access to wiki pages.
     */
    const val PAGES = "pages"

    /**
     * Access to user status.
     */
    const val STATUS = "status"

    /**
     * Access to offers (obsolete methods).
     */
    @Deprecated("")
    val OFFERS = "offers"

    /**
     * Access to questions (obsolete methods).
     */
    @Deprecated("")
    val QUESTIONS = "questions"

    /**
     * Access to standard and advanced methods for the wall.
     */
    const val WALL = "wall"

    /**
     * Access to user groups.
     */
    const val GROUPS = "groups"

    /**
     * Access to advanced methods for messaging.
     */
    const val MESSAGES = "messages"

    /**
     * Access to notifications about answers to the user.
     */
    const val NOTIFICATIONS = "notifications"

    /**
     * Access to statistics of user groups and applications where he/she is an administrator.
     */
    const val STATS = "stats"

    /**
     * Access to advanced methods for [Ads API](http://vk.com/dev/ads).
     */
    const val ADS = "ads"

    /**
     * Access to API at any time from a third party server.
     */
    const val OFFLINE = "offline"

    /**
     * Possibility to make API requests without HTTPS. <br></br>
     * **Note that this functionality is under testing and can be changed.**
     */
    const val NOHTTPS = "nohttps"

    /**
     * Converts integer value of permissions into arraylist of constants
     *
     * @param permissions integer permissions value
     * @return ArrayList contains string constants of permissions (scope)
     */
    fun parse(permissions: Int): ArrayList<String> {
        val result = ArrayList<String>()
        if (permissions and 1 > 0) result.add(NOTIFY)
        if (permissions and 2 > 0) result.add(FRIENDS)
        if (permissions and 4 > 0) result.add(PHOTOS)
        if (permissions and 8 > 0) result.add(AUDIO)
        if (permissions and 16 > 0) result.add(VIDEO)
        if (permissions and 128 > 0) result.add(PAGES)
        if (permissions and 1024 > 0) result.add(STATUS)
        if (permissions and 2048 > 0) result.add(NOTES)
        if (permissions and 4096 > 0) result.add(MESSAGES)
        if (permissions and 8192 > 0) result.add(WALL)
        if (permissions and 32768 > 0) result.add(ADS)
        if (permissions and 65536 > 0) result.add(OFFLINE)
        if (permissions and 131072 > 0) result.add(DOCS)
        if (permissions and 262144 > 0) result.add(GROUPS)
        if (permissions and 524288 > 0) result.add(NOTIFICATIONS)
        if (permissions and 1048576 > 0) result.add(STATS)
        return result
    }
}