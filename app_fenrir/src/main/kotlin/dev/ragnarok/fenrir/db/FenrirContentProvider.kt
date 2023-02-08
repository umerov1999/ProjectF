package dev.ragnarok.fenrir.db

import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import dev.ragnarok.fenrir.BuildConfig
import dev.ragnarok.fenrir.db.column.*
import dev.ragnarok.fenrir.db.column.attachments.CommentsAttachmentsColumns
import dev.ragnarok.fenrir.db.column.attachments.MessagesAttachmentsColumns
import dev.ragnarok.fenrir.db.column.attachments.WallAttachmentsColumns
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Logger

class FenrirContentProvider : ContentProvider() {
    companion object {
        // Uri authority
        val AUTHORITY: String = BuildConfig.APPLICATION_ID + ".providers.FenrirMessages"
        const val URI_USERS = 1
        const val URI_USERS_ID = 2
        const val URI_MESSAGES = 3
        const val URI_MESSAGES_ID = 4
        const val URI_MESSAGES_ATTACHMENTS = 5
        const val URI_MESSAGES_ATTACHMENTS_ID = 6
        const val URI_PHOTOS = 7
        const val URI_PHOTOS_EXTENDED = 8
        const val URI_PHOTOS_ID = 9
        const val URI_DIALOGS = 10
        const val URI_DOCS = 11
        const val URI_DOCS_ID = 12
        const val URI_VIDEOS = 13
        const val URI_VIDEOS_ID = 14
        const val URI_POSTS = 15
        const val URI_POSTS_ID = 16
        const val URI_POST_ATTACHMENTS = 17
        const val URI_POST_ATTACHMENTS_ID = 18
        const val URI_GROUPS = 19
        const val URI_GROUPS_ID = 20
        const val URI_RELATIVESHIP = 21
        const val URI_COMMENTS = 22
        const val URI_COMMENTS_ID = 23
        const val URI_COMMENTS_ATTACHMENTS = 24
        const val URI_COMMENTS_ATTACHMENTS_ID = 25
        const val URI_PHOTO_ALBUMS = 26
        const val URI_NEWS = 27
        const val URI_GROUPS_DET = 28
        const val URI_GROUPS_DET_ID = 29
        const val URI_VIDEO_ALBUMS = 30
        const val URI_TOPICS = 31
        const val URI_NOTIFICATIONS = 32
        const val URI_USER_DET = 33
        const val URI_USER_DET_ID = 34
        const val URI_FAVE_PHOTOS = 35
        const val URI_FAVE_VIDEOS = 36
        const val URI_FAVE_PAGES = 37
        const val URI_FAVE_GROUPS = 38
        const val URI_FAVE_LINKS = 39
        const val URI_FAVE_POSTS = 40
        const val URI_FAVE_ARTICLES = 41
        const val URI_FAVE_PRODUCTS = 42
        const val URI_COUNTRIES = 43
        const val URI_FEED_LISTS = 44
        const val URI_FRIEND_LISTS = 45
        const val URI_KEYS = 46
        const val URI_PEERS = 47

        // path
        private const val USER_PATH = "users"
        private const val MESSAGES_PATH = "messages"
        private const val MESSAGES_ATTACHMENTS_PATH = "messages_attachments"
        private const val PHOTOS_PATH = "photos"
        private const val PHOTOS_EXTENDED_PATH = "extended_photos"
        private const val DIALOGS_PATH = "dialogs"
        private const val PEERS_PATH = "peers"
        private const val DOCS_PATH = "docs"
        private const val VIDEOS_PATH = "videos"
        private const val POSTS_PATH = "posts"
        private const val POSTS_ATTACHMENTS_PATH = "wall_attachments"
        private const val GROUPS_PATH = "groups"
        private const val RELATIVESHIP_PATH = "relativeship"
        private const val COMMENTS_PATH = "comments"
        private const val COMMENTS_ATTACHMENTS_PATH = "comments_attachments"
        private const val PHOTO_ALBUMS_PATH = "photo_albums"
        private const val NEWS_PATH = "news"
        private const val GROUPS_DET_PATH = "groups_det"
        private const val VIDEO_ALBUMS_PATH = "video_albums"
        private const val TOPICS_PATH = "topics"
        private const val NOTIFICATIONS_PATH = "notifications"
        private const val USER_DET_PATH = "user_det"
        private const val FAVE_PHOTOS_PATH = "fave_photos"
        private const val FAVE_VIDEOS_PATH = "fave_videos"
        private const val FAVE_PAGES_PATH = "fave_pages"
        private const val FAVE_GROUPS_PATH = "fave_groups"
        private const val FAVE_ARTICLES_PATH = "fave_articles"
        private const val FAVE_PRODUCTS_PATH = "fave_products"
        private const val FAVE_LINKS_PATH = "fave_links"
        private const val FAVE_POSTS_PATH = "fave_posts"
        private const val COUNTRIES_PATH = "countries"
        private const val FEED_LISTS_PATH = "feed_lists"
        private const val FRIEND_LISTS_PATH = "friends_lists"
        private const val KEYS_PATH = "keys"

        // Типы данных
        val USER_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$USER_PATH"
        val USER_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$USER_PATH"
        val MESSAGE_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$MESSAGES_PATH"
        val MESSAGE_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.$AUTHORITY.$MESSAGES_PATH"
        val MESSAGES_ATTACHMENTS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$MESSAGES_ATTACHMENTS_PATH"
        val MESSAGES_ATTACHMENTS_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.$AUTHORITY.$MESSAGES_ATTACHMENTS_PATH"
        val PHOTOS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$PHOTOS_PATH"
        val PHOTOS_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.$AUTHORITY.$PHOTOS_PATH"
        val PHOTOS_EXTENDED_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$PHOTOS_EXTENDED_PATH"
        val DIALOGS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$DIALOGS_PATH"
        val PEERS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$PEERS_PATH"
        val DOCS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$DOCS_PATH"
        val DOCS_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$DOCS_PATH"
        val VIDEOS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$VIDEOS_PATH"
        val VIDEOS_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.$AUTHORITY.$VIDEOS_PATH"
        val POSTS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$POSTS_PATH"
        val POSTS_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$AUTHORITY.$POSTS_PATH"
        val POSTS_ATTACHMENTS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$POSTS_ATTACHMENTS_PATH"
        val POSTS_ATTACHMENTS_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.$AUTHORITY.$POSTS_ATTACHMENTS_PATH"
        val GROUPS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$GROUPS_PATH"
        val GROUPS_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.$AUTHORITY.$GROUPS_PATH"
        val RELATIVESHIP_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$RELATIVESHIP_PATH"
        val COMMENTS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$COMMENTS_PATH"
        val COMMENTS_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.$AUTHORITY.$COMMENTS_PATH"
        val COMMENTS_ATTACHMENTS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$COMMENTS_ATTACHMENTS_PATH"
        val COMMENTS_ATTACHMENTS_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.$AUTHORITY.$COMMENTS_ATTACHMENTS_PATH"
        val PHOTO_ALBUMS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$PHOTO_ALBUMS_PATH"
        val NEWS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$NEWS_PATH"
        val GROUPS_DET_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$GROUPS_DET_PATH"
        val GROUPS_DET_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.$AUTHORITY.$GROUPS_DET_PATH"
        val VIDEO_ALBUMS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$VIDEO_ALBUMS_PATH"
        val TOPICS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$TOPICS_PATH"
        val NOTIFICATIONS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$NOTIFICATIONS_PATH"
        val USER_DET_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$USER_DET_PATH"
        val USER_DET_CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.$AUTHORITY.$USER_DET_PATH"
        val FAVE_PHOTOS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$FAVE_PHOTOS_PATH"
        val FAVE_VIDEOS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$FAVE_VIDEOS_PATH"
        val FAVE_ARTICLES_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$FAVE_ARTICLES_PATH"
        val FAVE_PRODUCTS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$FAVE_PRODUCTS_PATH"
        val FAVE_PAGES_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$FAVE_PAGES_PATH"
        val FAVE_GROUPS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$FAVE_GROUPS_PATH"
        val FAVE_LINKS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$FAVE_LINKS_PATH"
        val FAVE_POSTS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$FAVE_POSTS_PATH"
        val COUNTRIES_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$COUNTRIES_PATH"
        val FEED_LISTS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$FEED_LISTS_PATH"
        val FRIEND_LISTS_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.$AUTHORITY.$FRIEND_LISTS_PATH"
        val KEYS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$AUTHORITY.$KEYS_PATH"

        // описание и создание UriMatcher
        private var sUriMatcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        // Общий Uri
        private val USER_CONTENT_URI = Uri.parse("content://$AUTHORITY/$USER_PATH")
        private val MESSAGE_CONTENT_URI = Uri.parse("content://$AUTHORITY/$MESSAGES_PATH")
        private val MESSAGES_ATTACHMENTS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$MESSAGES_ATTACHMENTS_PATH")
        private val PHOTOS_CONTENT_URI = Uri.parse("content://$AUTHORITY/$PHOTOS_PATH")
        private val PHOTOS_EXTENDED_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$PHOTOS_EXTENDED_PATH")
        private val DIALOGS_CONTENT_URI = Uri.parse("content://$AUTHORITY/$DIALOGS_PATH")
        private val PEERS_CONTENT_URI = Uri.parse("content://$AUTHORITY/$PEERS_PATH")
        private val DOCS_CONTENT_URI = Uri.parse("content://$AUTHORITY/$DOCS_PATH")
        private val VIDEOS_CONTENT_URI = Uri.parse("content://$AUTHORITY/$VIDEOS_PATH")
        private val POSTS_CONTENT_URI = Uri.parse("content://$AUTHORITY/$POSTS_PATH")
        private val POSTS_ATTACHMENTS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$POSTS_ATTACHMENTS_PATH")
        private val GROUPS_CONTENT_URI = Uri.parse("content://$AUTHORITY/$GROUPS_PATH")
        private val RELATIVESHIP_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$RELATIVESHIP_PATH")
        private val COMMENTS_CONTENT_URI = Uri.parse("content://$AUTHORITY/$COMMENTS_PATH")
        private val COMMENTS_ATTACHMENTS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$COMMENTS_ATTACHMENTS_PATH")
        private val PHOTO_ALBUMS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$PHOTO_ALBUMS_PATH")
        private val NEWS_CONTENT_URI = Uri.parse("content://$AUTHORITY/$NEWS_PATH")
        private val GROUPS_DET_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$GROUPS_DET_PATH")
        private val VIDEO_ALBUMS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$VIDEO_ALBUMS_PATH")
        private val TOPICS_CONTENT_URI = Uri.parse("content://$AUTHORITY/$TOPICS_PATH")
        private val NOTIFICATIONS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$NOTIFICATIONS_PATH")
        private val USER_DET_CONTENT_URI = Uri.parse("content://$AUTHORITY/$USER_DET_PATH")
        private val FAVE_PHOTOS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$FAVE_PHOTOS_PATH")
        private val FAVE_VIDEOS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$FAVE_VIDEOS_PATH")
        private val FAVE_PAGES_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$FAVE_PAGES_PATH")
        private val FAVE_GROUPS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$FAVE_GROUPS_PATH")
        private val FAVE_ARTICLES_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$FAVE_ARTICLES_PATH")
        private val FAVE_PRODUCTS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$FAVE_PRODUCTS_PATH")
        private val FAVE_LINKS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$FAVE_LINKS_PATH")
        private val FAVE_POSTS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$FAVE_POSTS_PATH")
        private val COUNTRIES_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$COUNTRIES_PATH")
        private val FEED_LISTS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$FEED_LISTS_PATH")
        private val FRIEND_LISTS_CONTENT_URI =
            Uri.parse("content://$AUTHORITY/$FRIEND_LISTS_PATH")
        private val KEYS_CONTENT_URI = Uri.parse("content://$AUTHORITY/$KEYS_PATH")
        private const val AID = "aid"

        private val sUsersProjectionMap: MutableMap<String, String>
        private val sMessagesProjectionMap: MutableMap<String, String>
        private val sMessagesAttachmentsProjectionMap: MutableMap<String, String>
        private val sPhotosProjectionMap: MutableMap<String, String>
        private val sPhotosExtendedProjectionMap: MutableMap<String, String>
        private val sDialogsProjectionMap: MutableMap<String, String>
        private val sPeersProjectionMap: MutableMap<String, String>
        private val sDocsProjectionMap: MutableMap<String, String>
        private val sVideosProjectionMap: MutableMap<String, String>
        private val sPostsProjectionMap: MutableMap<String, String>
        private val sPostsMessagesAttachmentsProjectionMap: MutableMap<String, String>
        private val sGroupsProjectionMap: MutableMap<String, String>
        private val sGroupsDetProjectionMap: MutableMap<String, String>
        private val sRelativeshipProjectionMap: MutableMap<String, String>
        private val sCommentsProjectionMap: MutableMap<String, String>
        private val sCommentsMessagesAttachmentsProjectionMap: MutableMap<String, String>
        private val sPhotoAlbumsProjectionMap: MutableMap<String, String>
        private val sNewsProjectionMap: MutableMap<String, String>
        private val sVideoAlbumsProjectionMap: MutableMap<String, String>
        private val sTopicsProjectionMap: MutableMap<String, String>
        private val sNoticationsProjectionMap: MutableMap<String, String>
        private val sUserDetProjectionMap: MutableMap<String, String>
        private val sFavePhotosProjectionMap: MutableMap<String, String>
        private val sFaveVideosProjectionMap: MutableMap<String, String>
        private val sFaveUsersProjectionMap: MutableMap<String, String>
        private val sFaveGroupsProjectionMap: MutableMap<String, String>
        private val sFaveLinksProjectionMap: MutableMap<String, String>
        private val sFavePostsProjectionMap: MutableMap<String, String>
        private val sFaveArticlesProjectionMap: MutableMap<String, String>
        private val sFaveProductsProjectionMap: MutableMap<String, String>
        private val sCountriesProjectionMap: MutableMap<String, String>
        private val sFeedListsProjectionMap: MutableMap<String, String>
        private val sFriendListsProjectionMap: MutableMap<String, String>
        private val sKeysProjectionMap: MutableMap<String, String>

        /*
        private fun testProjectionMapsArgs(vararg maps: MutableMap<String, String>) {
            for (i in maps) {
                for (s in i) {
                    if (!s.value.contains(s.key)) {
                        throw Exception("${s.key} = ${s.value}")
                    }
                }
            }
        }

        fun testProjectionMaps() {
            testProjectionMapsArgs(
                sUsersProjectionMap,
                sMessagesProjectionMap,
                sMessagesAttachmentsProjectionMap,
                sPhotosProjectionMap,
                sPhotosExtendedProjectionMap,
                sDialogsProjectionMap,
                sPeersProjectionMap,
                sDocsProjectionMap,
                sVideosProjectionMap,
                sPostsProjectionMap,
                sPostsMessagesAttachmentsProjectionMap,
                sGroupsProjectionMap,
                sGroupsDetProjectionMap,
                sCommentsProjectionMap,
                sCommentsMessagesAttachmentsProjectionMap,
                sPhotoAlbumsProjectionMap,
                sNewsProjectionMap,
                sVideoAlbumsProjectionMap,
                sTopicsProjectionMap,
                sNoticationsProjectionMap,
                sUserDetProjectionMap,
                sFavePhotosProjectionMap,
                sFaveVideosProjectionMap,
                sFaveUsersProjectionMap,
                sFaveGroupsProjectionMap,
                sFaveLinksProjectionMap,
                sFavePostsProjectionMap,
                sFaveArticlesProjectionMap,
                sFaveProductsProjectionMap,
                sCountriesProjectionMap,
                sFeedListsProjectionMap,
                sFriendListsProjectionMap,
                sKeysProjectionMap,
                sRelativeshipProjectionMap
            )
        }
         */

        fun getKeysContentUriFor(aid: Long): Uri {
            return appendAccountId(KEYS_CONTENT_URI, aid)
        }


        fun getGroupsDetContentUriFor(aid: Long): Uri {
            return appendAccountId(GROUPS_DET_CONTENT_URI, aid)
        }


        fun getFavePostsContentUriFor(aid: Long): Uri {
            return appendAccountId(FAVE_POSTS_CONTENT_URI, aid)
        }


        fun getFaveLinksContentUriFor(aid: Long): Uri {
            return appendAccountId(FAVE_LINKS_CONTENT_URI, aid)
        }


        fun getFavePhotosContentUriFor(aid: Long): Uri {
            return appendAccountId(FAVE_PHOTOS_CONTENT_URI, aid)
        }


        fun getFaveUsersContentUriFor(aid: Long): Uri {
            return appendAccountId(FAVE_PAGES_CONTENT_URI, aid)
        }


        fun getFaveGroupsContentUriFor(aid: Long): Uri {
            return appendAccountId(FAVE_GROUPS_CONTENT_URI, aid)
        }


        fun getFaveVideosContentUriFor(aid: Long): Uri {
            return appendAccountId(FAVE_VIDEOS_CONTENT_URI, aid)
        }


        fun getFaveArticlesContentUriFor(aid: Long): Uri {
            return appendAccountId(FAVE_ARTICLES_CONTENT_URI, aid)
        }


        fun getFaveProductsContentUriFor(aid: Long): Uri {
            return appendAccountId(FAVE_PRODUCTS_CONTENT_URI, aid)
        }


        fun getTopicsContentUriFor(aid: Long): Uri {
            return appendAccountId(TOPICS_CONTENT_URI, aid)
        }

        //public static Uri getPollContentUriFor(int aid){
        //    return appendAccountId(POLL_CONTENT_URI, aid);
        //}

        fun getMessagesAttachmentsContentUriFor(aid: Long): Uri {
            return appendAccountId(MESSAGES_ATTACHMENTS_CONTENT_URI, aid)
        }


        fun getPostsAttachmentsContentUriFor(aid: Long): Uri {
            return appendAccountId(POSTS_ATTACHMENTS_CONTENT_URI, aid)
        }


        fun getPostsContentUriFor(aid: Long): Uri {
            return appendAccountId(POSTS_CONTENT_URI, aid)
        }


        fun getVideosContentUriFor(aid: Long): Uri {
            return appendAccountId(VIDEOS_CONTENT_URI, aid)
        }


        fun getVideoAlbumsContentUriFor(aid: Long): Uri {
            return appendAccountId(VIDEO_ALBUMS_CONTENT_URI, aid)
        }


        fun getDocsContentUriFor(aid: Long): Uri {
            return appendAccountId(DOCS_CONTENT_URI, aid)
        }


        fun getPhotosContentUriFor(aid: Long): Uri {
            return appendAccountId(PHOTOS_CONTENT_URI, aid)
        }

        fun getPhotosExtendedContentUriFor(aid: Long): Uri {
            return appendAccountId(PHOTOS_EXTENDED_CONTENT_URI, aid)
        }


        fun getCommentsContentUriFor(aid: Long): Uri {
            return appendAccountId(COMMENTS_CONTENT_URI, aid)
        }


        fun getCommentsAttachmentsContentUriFor(aid: Long): Uri {
            return appendAccountId(COMMENTS_ATTACHMENTS_CONTENT_URI, aid)
        }


        fun getDialogsContentUriFor(aid: Long): Uri {
            return appendAccountId(DIALOGS_CONTENT_URI, aid)
        }


        fun getPeersContentUriFor(aid: Long): Uri {
            return appendAccountId(PEERS_CONTENT_URI, aid)
        }


        fun getRelativeshipContentUriFor(aid: Long): Uri {
            return appendAccountId(RELATIVESHIP_CONTENT_URI, aid)
        }


        fun getUserContentUriFor(aid: Long): Uri {
            return appendAccountId(USER_CONTENT_URI, aid)
        }


        fun getUserDetContentUriFor(aid: Long): Uri {
            return appendAccountId(USER_DET_CONTENT_URI, aid)
        }


        fun getGroupsContentUriFor(aid: Long): Uri {
            return appendAccountId(GROUPS_CONTENT_URI, aid)
        }


        fun getNewsContentUriFor(aid: Long): Uri {
            return appendAccountId(NEWS_CONTENT_URI, aid)
        }


        fun getMessageContentUriFor(aid: Long): Uri {
            return appendAccountId(MESSAGE_CONTENT_URI, aid)
        }


        fun getCountriesContentUriFor(aid: Long): Uri {
            return appendAccountId(COUNTRIES_CONTENT_URI, aid)
        }


        fun getNotificationsContentUriFor(aid: Long): Uri {
            return appendAccountId(NOTIFICATIONS_CONTENT_URI, aid)
        }


        fun getFeedListsContentUriFor(aid: Long): Uri {
            return appendAccountId(FEED_LISTS_CONTENT_URI, aid)
        }


        fun getPhotoAlbumsContentUriFor(aid: Long): Uri {
            return appendAccountId(PHOTO_ALBUMS_CONTENT_URI, aid)
        }


        fun getFriendListsContentUriFor(aid: Long): Uri {
            return appendAccountId(FRIEND_LISTS_CONTENT_URI, aid)
        }

        private fun appendAccountId(uri: Uri, aid: Long): Uri {
            return Uri.Builder()
                .scheme(uri.scheme)
                .authority(uri.authority)
                .path(uri.path)
                .appendQueryParameter(AID, aid.toString())
                .build()
        }

        init {
            sUriMatcher.addURI(AUTHORITY, USER_PATH, URI_USERS)
            sUriMatcher.addURI(AUTHORITY, "$USER_PATH/#", URI_USERS_ID)
            sUriMatcher.addURI(AUTHORITY, MESSAGES_PATH, URI_MESSAGES)
            sUriMatcher.addURI(AUTHORITY, "$MESSAGES_PATH/#", URI_MESSAGES_ID)
            sUriMatcher.addURI(AUTHORITY, MESSAGES_ATTACHMENTS_PATH, URI_MESSAGES_ATTACHMENTS)
            sUriMatcher.addURI(
                AUTHORITY,
                "$MESSAGES_ATTACHMENTS_PATH/#",
                URI_MESSAGES_ATTACHMENTS_ID
            )
            sUriMatcher.addURI(AUTHORITY, PHOTOS_PATH, URI_PHOTOS)
            sUriMatcher.addURI(AUTHORITY, PHOTOS_EXTENDED_PATH, URI_PHOTOS_EXTENDED)
            sUriMatcher.addURI(AUTHORITY, "$PHOTOS_PATH/#", URI_PHOTOS_ID)
            sUriMatcher.addURI(AUTHORITY, DIALOGS_PATH, URI_DIALOGS)
            sUriMatcher.addURI(AUTHORITY, PEERS_PATH, URI_PEERS)
            sUriMatcher.addURI(AUTHORITY, DOCS_PATH, URI_DOCS)
            sUriMatcher.addURI(AUTHORITY, "$DOCS_PATH/#", URI_DOCS_ID)
            sUriMatcher.addURI(AUTHORITY, VIDEOS_PATH, URI_VIDEOS)
            sUriMatcher.addURI(AUTHORITY, "$VIDEOS_PATH/#", URI_VIDEOS_ID)
            sUriMatcher.addURI(AUTHORITY, POSTS_PATH, URI_POSTS)
            sUriMatcher.addURI(AUTHORITY, "$POSTS_PATH/#", URI_POSTS_ID)
            sUriMatcher.addURI(AUTHORITY, POSTS_ATTACHMENTS_PATH, URI_POST_ATTACHMENTS)
            sUriMatcher.addURI(AUTHORITY, "$POSTS_ATTACHMENTS_PATH/#", URI_POST_ATTACHMENTS_ID)
            sUriMatcher.addURI(AUTHORITY, GROUPS_PATH, URI_GROUPS)
            sUriMatcher.addURI(AUTHORITY, "$GROUPS_PATH/#", URI_GROUPS_ID)
            sUriMatcher.addURI(AUTHORITY, RELATIVESHIP_PATH, URI_RELATIVESHIP)
            sUriMatcher.addURI(AUTHORITY, COMMENTS_PATH, URI_COMMENTS)
            sUriMatcher.addURI(AUTHORITY, "$COMMENTS_PATH/#", URI_COMMENTS_ID)
            sUriMatcher.addURI(AUTHORITY, COMMENTS_ATTACHMENTS_PATH, URI_COMMENTS_ATTACHMENTS)
            sUriMatcher.addURI(
                AUTHORITY,
                "$COMMENTS_ATTACHMENTS_PATH/#",
                URI_COMMENTS_ATTACHMENTS_ID
            )
            sUriMatcher.addURI(AUTHORITY, PHOTO_ALBUMS_PATH, URI_PHOTO_ALBUMS)
            sUriMatcher.addURI(AUTHORITY, NEWS_PATH, URI_NEWS)
            sUriMatcher.addURI(AUTHORITY, GROUPS_DET_PATH, URI_GROUPS_DET)
            sUriMatcher.addURI(AUTHORITY, "$GROUPS_DET_PATH/#", URI_GROUPS_DET_ID)
            sUriMatcher.addURI(AUTHORITY, VIDEO_ALBUMS_PATH, URI_VIDEO_ALBUMS)
            sUriMatcher.addURI(AUTHORITY, TOPICS_PATH, URI_TOPICS)
            sUriMatcher.addURI(AUTHORITY, NOTIFICATIONS_PATH, URI_NOTIFICATIONS)
            sUriMatcher.addURI(AUTHORITY, USER_DET_PATH, URI_USER_DET)
            sUriMatcher.addURI(AUTHORITY, "$USER_DET_PATH/#", URI_USER_DET_ID)
            sUriMatcher.addURI(AUTHORITY, FAVE_PHOTOS_PATH, URI_FAVE_PHOTOS)
            sUriMatcher.addURI(AUTHORITY, FAVE_VIDEOS_PATH, URI_FAVE_VIDEOS)
            sUriMatcher.addURI(AUTHORITY, FAVE_PAGES_PATH, URI_FAVE_PAGES)
            sUriMatcher.addURI(AUTHORITY, FAVE_GROUPS_PATH, URI_FAVE_GROUPS)
            sUriMatcher.addURI(AUTHORITY, FAVE_LINKS_PATH, URI_FAVE_LINKS)
            sUriMatcher.addURI(AUTHORITY, FAVE_ARTICLES_PATH, URI_FAVE_ARTICLES)
            sUriMatcher.addURI(AUTHORITY, FAVE_PRODUCTS_PATH, URI_FAVE_PRODUCTS)
            sUriMatcher.addURI(AUTHORITY, FAVE_POSTS_PATH, URI_FAVE_POSTS)
            sUriMatcher.addURI(AUTHORITY, COUNTRIES_PATH, URI_COUNTRIES)
            sUriMatcher.addURI(AUTHORITY, FEED_LISTS_PATH, URI_FEED_LISTS)
            sUriMatcher.addURI(AUTHORITY, FRIEND_LISTS_PATH, URI_FRIEND_LISTS)
            sUriMatcher.addURI(AUTHORITY, KEYS_PATH, URI_KEYS)
        }

        init {
            //Setup projection maps
            sUsersProjectionMap = HashMap()
            sUsersProjectionMap[BaseColumns._ID] = UserColumns.FULL_ID
            sUsersProjectionMap[UserColumns.FIRST_NAME] = UserColumns.FULL_FIRST_NAME
            sUsersProjectionMap[UserColumns.LAST_NAME] =
                UserColumns.FULL_LAST_NAME
            sUsersProjectionMap[UserColumns.ONLINE] = UserColumns.FULL_ONLINE
            sUsersProjectionMap[UserColumns.ONLINE_MOBILE] = UserColumns.FULL_ONLINE_MOBILE
            sUsersProjectionMap[UserColumns.ONLINE_APP] = UserColumns.FULL_ONLINE_APP
            sUsersProjectionMap[UserColumns.PHOTO_50] = UserColumns.FULL_PHOTO_50
            sUsersProjectionMap[UserColumns.PHOTO_100] =
                UserColumns.FULL_PHOTO_100
            sUsersProjectionMap[UserColumns.PHOTO_200] = UserColumns.FULL_PHOTO_200
            sUsersProjectionMap[UserColumns.PHOTO_MAX] = UserColumns.FULL_PHOTO_MAX
            sUsersProjectionMap[UserColumns.LAST_SEEN] = UserColumns.FULL_LAST_SEEN
            sUsersProjectionMap[UserColumns.HAS_UNSEEN_STORIES] =
                UserColumns.FULL_HAS_UNSEEN_STORIES
            sUsersProjectionMap[UserColumns.PLATFORM] =
                UserColumns.FULL_PLATFORM
            sUsersProjectionMap[UserColumns.USER_STATUS] = UserColumns.FULL_USER_STATUS
            sUsersProjectionMap[UserColumns.SEX] = UserColumns.FULL_SEX
            sUsersProjectionMap[UserColumns.DOMAIN] =
                UserColumns.FULL_DOMAIN
            sUsersProjectionMap[UserColumns.IS_FRIEND] = UserColumns.FULL_IS_FRIEND
            sUsersProjectionMap[UserColumns.FRIEND_STATUS] = UserColumns.FULL_FRIEND_STATUS
            sUsersProjectionMap[UserColumns.WRITE_MESSAGE_STATUS] =
                UserColumns.FULL_WRITE_MESSAGE_STATUS
            sUsersProjectionMap[UserColumns.IS_USER_BLACK_LIST] =
                UserColumns.FULL_IS_USER_BLACK_LIST
            sUsersProjectionMap[UserColumns.IS_BLACK_LISTED] = UserColumns.FULL_IS_BLACK_LISTED
            sUsersProjectionMap[UserColumns.IS_CAN_ACCESS_CLOSED] =
                UserColumns.FULL_IS_CAN_ACCESS_CLOSED
            sUsersProjectionMap[UserColumns.IS_VERIFIED] = UserColumns.FULL_IS_VERIFIED
            sUsersProjectionMap[UserColumns.MAIDEN_NAME] = UserColumns.FULL_MAIDEN_NAME
            sUsersProjectionMap[UserColumns.BDATE] = UserColumns.FULL_BDATE

            sRelativeshipProjectionMap = HashMap()
            sRelativeshipProjectionMap[BaseColumns._ID] = RelationshipColumns.FULL_ID
            sRelativeshipProjectionMap[RelationshipColumns.OBJECT_ID] =
                RelationshipColumns.FULL_OBJECT_ID
            sRelativeshipProjectionMap[RelationshipColumns.SUBJECT_ID] =
                RelationshipColumns.FULL_SUBJECT_ID
            sRelativeshipProjectionMap[RelationshipColumns.TYPE] = RelationshipColumns.FULL_TYPE
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_FIRST_NAME] =
                UserColumns.FULL_FIRST_NAME + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_FIRST_NAME
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_LAST_NAME] =
                UserColumns.FULL_LAST_NAME + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_LAST_NAME
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_ONLINE] =
                UserColumns.FULL_ONLINE + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_ONLINE
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_ONLINE_MOBILE] =
                UserColumns.FULL_ONLINE_MOBILE + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_ONLINE_MOBILE
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_ONLINE_APP] =
                UserColumns.FULL_ONLINE_APP + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_ONLINE_APP
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_PHOTO_50] =
                UserColumns.FULL_PHOTO_50 + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_PHOTO_50
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_PHOTO_100] =
                UserColumns.FULL_PHOTO_100 + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_PHOTO_100
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_PHOTO_200] =
                UserColumns.FULL_PHOTO_200 + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_PHOTO_200
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_PHOTO_MAX] =
                UserColumns.FULL_PHOTO_MAX + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_PHOTO_MAX
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_HAS_UNSEEN_STORIES] =
                UserColumns.FULL_HAS_UNSEEN_STORIES + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_HAS_UNSEEN_STORIES
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_LAST_SEEN] =
                UserColumns.FULL_LAST_SEEN + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_LAST_SEEN
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_PLATFORM] =
                UserColumns.FULL_PLATFORM + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_PLATFORM
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_STATUS] =
                UserColumns.FULL_USER_STATUS + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_STATUS
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_SEX] =
                UserColumns.FULL_SEX + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_SEX
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_IS_FRIEND] =
                UserColumns.FULL_IS_FRIEND + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_IS_FRIEND
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_FRIEND_STATUS] =
                UserColumns.FULL_FRIEND_STATUS + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_FRIEND_STATUS
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_WRITE_MESSAGE_STATUS] =
                UserColumns.FULL_WRITE_MESSAGE_STATUS + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_WRITE_MESSAGE_STATUS
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_IS_USER_BLACK_LIST] =
                UserColumns.FULL_IS_USER_BLACK_LIST + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_IS_USER_BLACK_LIST
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_IS_BLACK_LISTED] =
                UserColumns.FULL_IS_BLACK_LISTED + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_IS_BLACK_LISTED
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_IS_CAN_ACCESS_CLOSED] =
                UserColumns.FULL_IS_CAN_ACCESS_CLOSED + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_IS_CAN_ACCESS_CLOSED
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_IS_VERIFIED] =
                UserColumns.FULL_IS_VERIFIED + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_IS_VERIFIED
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_MAIDEN_NAME] =
                UserColumns.FULL_MAIDEN_NAME + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_MAIDEN_NAME
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_USER_BDATE] =
                UserColumns.FULL_BDATE + " AS " + RelationshipColumns.FOREIGN_SUBJECT_USER_BDATE

            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_NAME] =
                GroupColumns.FULL_NAME + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_NAME
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_SCREEN_NAME] =
                GroupColumns.FULL_SCREEN_NAME + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_SCREEN_NAME
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_PHOTO_50] =
                GroupColumns.FULL_PHOTO_50 + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_PHOTO_50
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_PHOTO_100] =
                GroupColumns.FULL_PHOTO_100 + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_PHOTO_100
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_PHOTO_200] =
                GroupColumns.FULL_PHOTO_200 + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_PHOTO_200
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_IS_CLOSED] =
                GroupColumns.FULL_IS_CLOSED + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_IS_CLOSED
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_IS_BLACK_LISTED] =
                GroupColumns.FULL_IS_BLACK_LISTED + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_IS_BLACK_LISTED
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_IS_VERIFIED] =
                GroupColumns.FULL_IS_VERIFIED + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_IS_VERIFIED
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_HAS_UNSEEN_STORIES] =
                GroupColumns.FULL_HAS_UNSEEN_STORIES + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_HAS_UNSEEN_STORIES
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_IS_ADMIN] =
                GroupColumns.FULL_IS_ADMIN + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_IS_ADMIN
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_ADMIN_LEVEL] =
                GroupColumns.FULL_ADMIN_LEVEL + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_ADMIN_LEVEL
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_IS_MEMBER] =
                GroupColumns.FULL_IS_MEMBER + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_IS_MEMBER
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_MEMBERS_COUNT] =
                GroupColumns.FULL_MEMBERS_COUNT + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_MEMBERS_COUNT
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_MEMBER_STATUS] =
                GroupColumns.FULL_MEMBER_STATUS + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_MEMBER_STATUS
            sRelativeshipProjectionMap[RelationshipColumns.FOREIGN_SUBJECT_GROUP_TYPE] =
                GroupColumns.FULL_TYPE + " AS " + RelationshipColumns.FOREIGN_SUBJECT_GROUP_TYPE

            sMessagesProjectionMap = HashMap()
            sMessagesProjectionMap[MessageColumns._ID] = MessageColumns.FULL_ID
            sMessagesProjectionMap[MessageColumns.PEER_ID] = MessageColumns.FULL_PEER_ID
            sMessagesProjectionMap[MessageColumns.FROM_ID] = MessageColumns.FULL_FROM_ID
            sMessagesProjectionMap[MessageColumns.DATE] = MessageColumns.FULL_DATE
            //sMessagesProjectionMap.put(MessageColumns.READ_STATE, MessageColumns.FULL_READ_STATE);
            sMessagesProjectionMap[MessageColumns.OUT] = MessageColumns.FULL_OUT
            //sMessagesProjectionMap.put(MessageColumns.TITLE, MessageColumns.FULL_TITLE);
            sMessagesProjectionMap[MessageColumns.BODY] =
                MessageColumns.FULL_BODY
            sMessagesProjectionMap[MessageColumns.ENCRYPTED] =
                MessageColumns.FULL_ENCRYPTED
            sMessagesProjectionMap[MessageColumns.DELETED] = MessageColumns.FULL_DELETED
            sMessagesProjectionMap[MessageColumns.DELETED_FOR_ALL] =
                MessageColumns.FULL_DELETED_FOR_ALL
            sMessagesProjectionMap[MessageColumns.IMPORTANT] = MessageColumns.FULL_IMPORTANT
            sMessagesProjectionMap[MessageColumns.FORWARD_COUNT] =
                MessageColumns.FULL_FORWARD_COUNT
            sMessagesProjectionMap[MessageColumns.HAS_ATTACHMENTS] =
                MessageColumns.FULL_HAS_ATTACHMENTS
            sMessagesProjectionMap[MessageColumns.STATUS] =
                MessageColumns.FULL_STATUS
            sMessagesProjectionMap[MessageColumns.ATTACH_TO] = MessageColumns.FULL_ATTACH_TO
            sMessagesProjectionMap[MessageColumns.ORIGINAL_ID] =
                MessageColumns.FULL_ORIGINAL_ID
            sMessagesProjectionMap[MessageColumns.UPDATE_TIME] = MessageColumns.FULL_UPDATE_TIME
            sMessagesProjectionMap[MessageColumns.ACTION] =
                MessageColumns.FULL_ACTION
            sMessagesProjectionMap[MessageColumns.ACTION_MID] =
                MessageColumns.FULL_ACTION_MID
            sMessagesProjectionMap[MessageColumns.ACTION_EMAIL] = MessageColumns.FULL_ACTION_EMAIL
            sMessagesProjectionMap[MessageColumns.ACTION_TEXT] = MessageColumns.FULL_ACTION_TEXT
            sMessagesProjectionMap[MessageColumns.PHOTO_50] =
                MessageColumns.FULL_PHOTO_50
            sMessagesProjectionMap[MessageColumns.PHOTO_100] = MessageColumns.FULL_PHOTO_100
            sMessagesProjectionMap[MessageColumns.PHOTO_200] = MessageColumns.FULL_PHOTO_200
            sMessagesProjectionMap[MessageColumns.RANDOM_ID] =
                MessageColumns.FULL_RANDOM_ID
            sMessagesProjectionMap[MessageColumns.EXTRAS] = MessageColumns.FULL_EXTRAS
            sMessagesProjectionMap[MessageColumns.PAYLOAD] =
                MessageColumns.FULL_PAYLOAD
            sMessagesProjectionMap[MessageColumns.KEYBOARD] = MessageColumns.FULL_KEYBOARD

            sMessagesAttachmentsProjectionMap = HashMap()
            sMessagesAttachmentsProjectionMap[BaseColumns._ID] = MessagesAttachmentsColumns.FULL_ID
            sMessagesAttachmentsProjectionMap[MessagesAttachmentsColumns.M_ID] =
                MessagesAttachmentsColumns.FULL_M_ID
            sMessagesAttachmentsProjectionMap[MessagesAttachmentsColumns.DATA] =
                MessagesAttachmentsColumns.FULL_DATA

            sPhotosProjectionMap = HashMap()
            sPhotosProjectionMap[BaseColumns._ID] =
                PhotosColumns.FULL_ID
            sPhotosProjectionMap[PhotosColumns.PHOTO_ID] =
                PhotosColumns.FULL_PHOTO_ID
            sPhotosProjectionMap[PhotosColumns.ALBUM_ID] = PhotosColumns.FULL_ALBUM_ID
            sPhotosProjectionMap[PhotosColumns.OWNER_ID] =
                PhotosColumns.FULL_OWNER_ID
            sPhotosProjectionMap[PhotosColumns.WIDTH] =
                PhotosColumns.FULL_WIDTH
            sPhotosProjectionMap[PhotosColumns.HEIGHT] = PhotosColumns.FULL_HEIGHT
            sPhotosProjectionMap[PhotosColumns.TEXT] =
                PhotosColumns.FULL_TEXT
            sPhotosProjectionMap[PhotosColumns.DATE] =
                PhotosColumns.FULL_DATE
            sPhotosProjectionMap[PhotosColumns.SIZES] =
                PhotosColumns.FULL_SIZES
            sPhotosProjectionMap[PhotosColumns.USER_LIKES] = PhotosColumns.FULL_USER_LIKES
            sPhotosProjectionMap[PhotosColumns.CAN_COMMENT] =
                PhotosColumns.FULL_CAN_COMMENT
            sPhotosProjectionMap[PhotosColumns.LIKES] = PhotosColumns.FULL_LIKES
            sPhotosProjectionMap[PhotosColumns.REPOSTS] = PhotosColumns.FULL_REPOSTS
            sPhotosProjectionMap[PhotosColumns.COMMENTS] =
                PhotosColumns.FULL_COMMENTS
            sPhotosProjectionMap[PhotosColumns.TAGS] =
                PhotosColumns.FULL_TAGS
            sPhotosProjectionMap[PhotosColumns.ACCESS_KEY] =
                PhotosColumns.FULL_ACCESS_KEY
            sPhotosProjectionMap[PhotosColumns.DELETED] =
                PhotosColumns.FULL_DELETED

            sPhotosExtendedProjectionMap = HashMap()
            sPhotosExtendedProjectionMap[BaseColumns._ID] =
                PhotosExtendedColumns.FULL_ID
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.DB_ALBUM_ID] =
                PhotosExtendedColumns.FULL_DB_ALBUM_ID
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.DB_OWNER_ID] =
                PhotosExtendedColumns.FULL_DB_OWNER_ID
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.PHOTO_ID] =
                PhotosExtendedColumns.FULL_PHOTO_ID
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.ALBUM_ID] =
                PhotosExtendedColumns.FULL_ALBUM_ID
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.OWNER_ID] =
                PhotosExtendedColumns.FULL_OWNER_ID
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.WIDTH] =
                PhotosExtendedColumns.FULL_WIDTH
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.HEIGHT] =
                PhotosExtendedColumns.FULL_HEIGHT
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.TEXT] =
                PhotosExtendedColumns.FULL_TEXT
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.DATE] =
                PhotosExtendedColumns.FULL_DATE
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.SIZES] =
                PhotosExtendedColumns.FULL_SIZES
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.USER_LIKES] =
                PhotosExtendedColumns.FULL_USER_LIKES
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.CAN_COMMENT] =
                PhotosExtendedColumns.FULL_CAN_COMMENT
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.LIKES] =
                PhotosExtendedColumns.FULL_LIKES
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.REPOSTS] =
                PhotosExtendedColumns.FULL_REPOSTS
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.COMMENTS] =
                PhotosExtendedColumns.FULL_COMMENTS
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.TAGS] =
                PhotosExtendedColumns.FULL_TAGS
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.ACCESS_KEY] =
                PhotosExtendedColumns.FULL_ACCESS_KEY
            sPhotosExtendedProjectionMap[PhotosExtendedColumns.DELETED] =
                PhotosExtendedColumns.FULL_DELETED

            sDialogsProjectionMap = HashMap()
            sDialogsProjectionMap[BaseColumns._ID] =
                DialogsColumns.FULL_ID
            sDialogsProjectionMap[DialogsColumns.UNREAD] =
                DialogsColumns.FULL_UNREAD
            sDialogsProjectionMap[DialogsColumns.TITLE] = DialogsColumns.FULL_TITLE
            sDialogsProjectionMap[DialogsColumns.IN_READ] =
                DialogsColumns.FULL_IN_READ
            sDialogsProjectionMap[DialogsColumns.OUT_READ] = DialogsColumns.FULL_OUT_READ
            sDialogsProjectionMap[DialogsColumns.PHOTO_50] = DialogsColumns.FULL_PHOTO_50
            sDialogsProjectionMap[DialogsColumns.PHOTO_100] = DialogsColumns.FULL_PHOTO_100
            sDialogsProjectionMap[DialogsColumns.PHOTO_200] =
                DialogsColumns.FULL_PHOTO_200
            sDialogsProjectionMap[DialogsColumns.ACL] = DialogsColumns.FULL_ACL
            sDialogsProjectionMap[DialogsColumns.LAST_MESSAGE_ID] =
                DialogsColumns.FULL_LAST_MESSAGE_ID
            sDialogsProjectionMap[DialogsColumns.IS_GROUP_CHANNEL] =
                DialogsColumns.FULL_IS_GROUP_CHANNEL
            sDialogsProjectionMap[DialogsColumns.MAJOR_ID] = DialogsColumns.FULL_MAJOR_ID
            sDialogsProjectionMap[DialogsColumns.MINOR_ID] =
                DialogsColumns.FULL_MINOR_ID
            sDialogsProjectionMap[DialogsColumns.FOREIGN_MESSAGE_FROM_ID] =
                MessageColumns.FULL_FROM_ID + " AS " + DialogsColumns.FOREIGN_MESSAGE_FROM_ID
            sDialogsProjectionMap[DialogsColumns.FOREIGN_MESSAGE_BODY] =
                MessageColumns.FULL_BODY + " AS " + DialogsColumns.FOREIGN_MESSAGE_BODY
            sDialogsProjectionMap[DialogsColumns.FOREIGN_MESSAGE_DATE] =
                MessageColumns.FULL_DATE + " AS " + DialogsColumns.FOREIGN_MESSAGE_DATE
            sDialogsProjectionMap[DialogsColumns.FOREIGN_MESSAGE_OUT] =
                MessageColumns.FULL_OUT + " AS " + DialogsColumns.FOREIGN_MESSAGE_OUT
            //sDialogsProjectionMap.put(DialogsColumns.FOREIGN_MESSAGE_READ_STATE, MessageColumns.FULL_READ_STATE + " AS " + DialogsColumns.FOREIGN_MESSAGE_READ_STATE);
            sDialogsProjectionMap[DialogsColumns.FOREIGN_MESSAGE_HAS_ATTACHMENTS] =
                MessageColumns.FULL_HAS_ATTACHMENTS + " AS " + DialogsColumns.FOREIGN_MESSAGE_HAS_ATTACHMENTS
            sDialogsProjectionMap[DialogsColumns.FOREIGN_MESSAGE_FWD_COUNT] =
                MessageColumns.FULL_FORWARD_COUNT + " AS " + DialogsColumns.FOREIGN_MESSAGE_FWD_COUNT
            sDialogsProjectionMap[DialogsColumns.FOREIGN_MESSAGE_ACTION] =
                MessageColumns.FULL_ACTION + " AS " + DialogsColumns.FOREIGN_MESSAGE_ACTION
            sDialogsProjectionMap[DialogsColumns.FOREIGN_MESSAGE_ENCRYPTED] =
                MessageColumns.FULL_ENCRYPTED + " AS " + DialogsColumns.FOREIGN_MESSAGE_ENCRYPTED

            sPeersProjectionMap = HashMap()
            sPeersProjectionMap[BaseColumns._ID] = PeersColumns.FULL_ID
            sPeersProjectionMap[PeersColumns.UNREAD] =
                PeersColumns.FULL_UNREAD
            sPeersProjectionMap[PeersColumns.TITLE] = PeersColumns.FULL_TITLE
            sPeersProjectionMap[PeersColumns.IN_READ] = PeersColumns.FULL_IN_READ
            sPeersProjectionMap[PeersColumns.OUT_READ] =
                PeersColumns.FULL_OUT_READ
            sPeersProjectionMap[PeersColumns.PHOTO_50] = PeersColumns.FULL_PHOTO_50
            sPeersProjectionMap[PeersColumns.PHOTO_100] = PeersColumns.FULL_PHOTO_100
            sPeersProjectionMap[PeersColumns.PHOTO_200] =
                PeersColumns.FULL_PHOTO_200
            sPeersProjectionMap[PeersColumns.PINNED] = PeersColumns.FULL_PINNED
            sPeersProjectionMap[PeersColumns.LAST_MESSAGE_ID] =
                PeersColumns.FULL_LAST_MESSAGE_ID
            sPeersProjectionMap[PeersColumns.ACL] = PeersColumns.FULL_ACL
            sPeersProjectionMap[PeersColumns.IS_GROUP_CHANNEL] =
                PeersColumns.FULL_IS_GROUP_CHANNEL
            sPeersProjectionMap[PeersColumns.KEYBOARD] = PeersColumns.FULL_KEYBOARD
            sPeersProjectionMap[PeersColumns.MAJOR_ID] = PeersColumns.FULL_MAJOR_ID
            sPeersProjectionMap[PeersColumns.MINOR_ID] = PeersColumns.FULL_MINOR_ID

            sDocsProjectionMap = HashMap()
            sDocsProjectionMap[BaseColumns._ID] = DocColumns.FULL_ID
            sDocsProjectionMap[DocColumns.DOC_ID] = DocColumns.FULL_DOC_ID
            sDocsProjectionMap[DocColumns.OWNER_ID] = DocColumns.FULL_OWNER_ID
            sDocsProjectionMap[DocColumns.TITLE] = DocColumns.FULL_TITLE
            sDocsProjectionMap[DocColumns.SIZE] = DocColumns.FULL_SIZE
            sDocsProjectionMap[DocColumns.EXT] = DocColumns.FULL_EXT
            sDocsProjectionMap[DocColumns.URL] = DocColumns.FULL_URL
            sDocsProjectionMap[DocColumns.PHOTO] = DocColumns.FULL_PHOTO
            sDocsProjectionMap[DocColumns.GRAFFITI] =
                DocColumns.FULL_GRAFFITI
            sDocsProjectionMap[DocColumns.VIDEO] = DocColumns.FULL_VIDEO
            sDocsProjectionMap[DocColumns.DATE] = DocColumns.FULL_DATE
            sDocsProjectionMap[DocColumns.TYPE] = DocColumns.FULL_TYPE
            sDocsProjectionMap[DocColumns.ACCESS_KEY] = DocColumns.FULL_ACCESS_KEY

            sVideosProjectionMap = HashMap()
            sVideosProjectionMap[BaseColumns._ID] =
                VideoColumns.FULL_ID
            sVideosProjectionMap[VideoColumns.VIDEO_ID] =
                VideoColumns.FULL_VIDEO_ID
            sVideosProjectionMap[VideoColumns.OWNER_ID] =
                VideoColumns.FULL_OWNER_ID
            sVideosProjectionMap[VideoColumns.ORIGINAL_OWNER_ID] =
                VideoColumns.FULL_ORIGINAL_OWNER_ID
            sVideosProjectionMap[VideoColumns.ALBUM_ID] = VideoColumns.FULL_ALBUM_ID
            sVideosProjectionMap[VideoColumns.TITLE] = VideoColumns.FULL_TITLE
            sVideosProjectionMap[VideoColumns.DESCRIPTION] =
                VideoColumns.FULL_DESCRIPTION
            sVideosProjectionMap[VideoColumns.DURATION] =
                VideoColumns.FULL_DURATION
            sVideosProjectionMap[VideoColumns.LINK] = VideoColumns.FULL_LINK
            sVideosProjectionMap[VideoColumns.DATE] =
                VideoColumns.FULL_DATE
            sVideosProjectionMap[VideoColumns.ADDING_DATE] = VideoColumns.FULL_ADDING_DATE
            sVideosProjectionMap[VideoColumns.VIEWS] = VideoColumns.FULL_VIEWS
            sVideosProjectionMap[VideoColumns.PLAYER] = VideoColumns.FULL_PLAYER
            sVideosProjectionMap[VideoColumns.IMAGE] = VideoColumns.FULL_IMAGE
            sVideosProjectionMap[VideoColumns.ACCESS_KEY] =
                VideoColumns.FULL_ACCESS_KEY
            sVideosProjectionMap[VideoColumns.COMMENTS] =
                VideoColumns.FULL_COMMENTS
            sVideosProjectionMap[VideoColumns.CAN_COMMENT] = VideoColumns.FULL_CAN_COMMENT
            sVideosProjectionMap[VideoColumns.IS_PRIVATE] = VideoColumns.FULL_IS_PRIVATE
            sVideosProjectionMap[VideoColumns.IS_FAVORITE] = VideoColumns.FULL_IS_FAVORITE
            sVideosProjectionMap[VideoColumns.CAN_REPOST] =
                VideoColumns.FULL_CAN_REPOST
            sVideosProjectionMap[VideoColumns.USER_LIKES] =
                VideoColumns.FULL_USER_LIKES
            sVideosProjectionMap[VideoColumns.REPEAT] =
                VideoColumns.FULL_REPEAT
            sVideosProjectionMap[VideoColumns.LIKES] = VideoColumns.FULL_LIKES
            sVideosProjectionMap[VideoColumns.PRIVACY_VIEW] = VideoColumns.FULL_PRIVACY_VIEW
            sVideosProjectionMap[VideoColumns.PRIVACY_COMMENT] =
                VideoColumns.FULL_PRIVACY_COMMENT
            sVideosProjectionMap[VideoColumns.MP4_240] =
                VideoColumns.FULL_MP4_240
            sVideosProjectionMap[VideoColumns.MP4_360] =
                VideoColumns.FULL_MP4_360
            sVideosProjectionMap[VideoColumns.MP4_480] =
                VideoColumns.FULL_MP4_480
            sVideosProjectionMap[VideoColumns.MP4_720] =
                VideoColumns.FULL_MP4_720
            sVideosProjectionMap[VideoColumns.MP4_1080] =
                VideoColumns.FULL_MP4_1080
            sVideosProjectionMap[VideoColumns.MP4_1440] =
                VideoColumns.FULL_MP4_1440
            sVideosProjectionMap[VideoColumns.MP4_2160] =
                VideoColumns.FULL_MP4_2160
            sVideosProjectionMap[VideoColumns.EXTERNAL] = VideoColumns.FULL_EXTERNAL
            sVideosProjectionMap[VideoColumns.HLS] = VideoColumns.FULL_HLS
            sVideosProjectionMap[VideoColumns.LIVE] =
                VideoColumns.FULL_LIVE
            sVideosProjectionMap[VideoColumns.PLATFORM] =
                VideoColumns.FULL_PLATFORM
            sVideosProjectionMap[VideoColumns.CAN_EDIT] =
                VideoColumns.FULL_CAN_EDIT
            sVideosProjectionMap[VideoColumns.CAN_ADD] = VideoColumns.FULL_CAN_ADD

            sPostsProjectionMap = HashMap()
            sPostsProjectionMap[BaseColumns._ID] = PostsColumns.FULL_ID
            sPostsProjectionMap[PostsColumns.POST_ID] =
                PostsColumns.FULL_POST_ID
            sPostsProjectionMap[PostsColumns.OWNER_ID] =
                PostsColumns.FULL_OWNER_ID
            sPostsProjectionMap[PostsColumns.FROM_ID] = PostsColumns.FULL_FROM_ID
            sPostsProjectionMap[PostsColumns.DATE] = PostsColumns.FULL_DATE
            sPostsProjectionMap[PostsColumns.TEXT] = PostsColumns.FULL_TEXT
            sPostsProjectionMap[PostsColumns.REPLY_OWNER_ID] = PostsColumns.FULL_REPLY_OWNER_ID
            sPostsProjectionMap[PostsColumns.REPLY_POST_ID] = PostsColumns.FULL_REPLY_POST_ID
            sPostsProjectionMap[PostsColumns.FRIENDS_ONLY] = PostsColumns.FULL_FRIENDS_ONLY
            sPostsProjectionMap[PostsColumns.COMMENTS_COUNT] =
                PostsColumns.FULL_COMMENTS_COUNT
            sPostsProjectionMap[PostsColumns.CAN_POST_COMMENT] = PostsColumns.FULL_CAN_POST_COMMENT
            sPostsProjectionMap[PostsColumns.COPYRIGHT_BLOB] = PostsColumns.FULL_COPYRIGHT_BLOB
            sPostsProjectionMap[PostsColumns.LIKES_COUNT] = PostsColumns.FULL_LIKES_COUNT
            sPostsProjectionMap[PostsColumns.USER_LIKES] =
                PostsColumns.FULL_USER_LIKES
            sPostsProjectionMap[PostsColumns.CAN_LIKE] = PostsColumns.FULL_CAN_LIKE
            sPostsProjectionMap[PostsColumns.CAN_PUBLISH] = PostsColumns.FULL_CAN_PUBLISH
            sPostsProjectionMap[PostsColumns.CAN_EDIT] =
                PostsColumns.FULL_CAN_EDIT
            sPostsProjectionMap[PostsColumns.IS_FAVORITE] =
                PostsColumns.FULL_IS_FAVORITE
            sPostsProjectionMap[PostsColumns.IS_DONUT] =
                PostsColumns.FULL_IS_DONUT
            sPostsProjectionMap[PostsColumns.REPOSTS_COUNT] =
                PostsColumns.FULL_REPOSTS_COUNT
            sPostsProjectionMap[PostsColumns.USER_REPOSTED] =
                PostsColumns.FULL_USER_REPOSTED
            sPostsProjectionMap[PostsColumns.POST_TYPE] = PostsColumns.FULL_POST_TYPE
            sPostsProjectionMap[PostsColumns.ATTACHMENTS_COUNT] =
                PostsColumns.FULL_ATTACHMENTS_COUNT
            sPostsProjectionMap[PostsColumns.SIGNED_ID] = PostsColumns.FULL_SIGNED_ID
            sPostsProjectionMap[PostsColumns.CREATED_BY] = PostsColumns.FULL_CREATED_BY
            sPostsProjectionMap[PostsColumns.CAN_PIN] = PostsColumns.FULL_CAN_PIN
            sPostsProjectionMap[PostsColumns.IS_PINNED] = PostsColumns.FULL_IS_PINNED
            sPostsProjectionMap[PostsColumns.DELETED] =
                PostsColumns.FULL_DELETED
            sPostsProjectionMap[PostsColumns.POST_SOURCE] = PostsColumns.FULL_POST_SOURCE
            sPostsProjectionMap[PostsColumns.VIEWS] = PostsColumns.FULL_VIEWS

            sPostsMessagesAttachmentsProjectionMap = HashMap()
            sPostsMessagesAttachmentsProjectionMap[BaseColumns._ID] =
                WallAttachmentsColumns.FULL_ID
            sPostsMessagesAttachmentsProjectionMap[WallAttachmentsColumns.P_ID] =
                WallAttachmentsColumns.FULL_P_ID
            sPostsMessagesAttachmentsProjectionMap[WallAttachmentsColumns.DATA] =
                WallAttachmentsColumns.FULL_DATA

            sGroupsProjectionMap = HashMap()
            sGroupsProjectionMap[BaseColumns._ID] = GroupColumns.FULL_ID
            sGroupsProjectionMap[GroupColumns.NAME] = GroupColumns.FULL_NAME
            sGroupsProjectionMap[GroupColumns.SCREEN_NAME] = GroupColumns.FULL_SCREEN_NAME
            sGroupsProjectionMap[GroupColumns.HAS_UNSEEN_STORIES] =
                GroupColumns.FULL_HAS_UNSEEN_STORIES
            sGroupsProjectionMap[GroupColumns.IS_CLOSED] =
                GroupColumns.FULL_IS_CLOSED
            sGroupsProjectionMap[GroupColumns.IS_VERIFIED] = GroupColumns.FULL_IS_VERIFIED
            sGroupsProjectionMap[GroupColumns.IS_ADMIN] = GroupColumns.FULL_IS_ADMIN
            sGroupsProjectionMap[GroupColumns.ADMIN_LEVEL] = GroupColumns.FULL_ADMIN_LEVEL
            sGroupsProjectionMap[GroupColumns.IS_MEMBER] = GroupColumns.FULL_IS_MEMBER
            sGroupsProjectionMap[GroupColumns.MEMBERS_COUNT] = GroupColumns.FULL_MEMBERS_COUNT
            sGroupsProjectionMap[GroupColumns.MEMBER_STATUS] = GroupColumns.FULL_MEMBER_STATUS
            sGroupsProjectionMap[GroupColumns.TYPE] =
                GroupColumns.FULL_TYPE
            sGroupsProjectionMap[GroupColumns.PHOTO_50] =
                GroupColumns.FULL_PHOTO_50
            sGroupsProjectionMap[GroupColumns.PHOTO_100] = GroupColumns.FULL_PHOTO_100
            sGroupsProjectionMap[GroupColumns.PHOTO_200] =
                GroupColumns.FULL_PHOTO_200
            sGroupsProjectionMap[GroupColumns.CAN_ADD_TOPICS] =
                GroupColumns.FULL_CAN_ADD_TOPICS
            sGroupsProjectionMap[GroupColumns.TOPICS_ORDER] = GroupColumns.FULL_TOPICS_ORDER
            sGroupsProjectionMap[GroupColumns.IS_BLACK_LISTED] = GroupColumns.FULL_IS_BLACK_LISTED

            sCommentsProjectionMap = HashMap()
            sCommentsProjectionMap[BaseColumns._ID] = CommentsColumns.FULL_ID
            sCommentsProjectionMap[CommentsColumns.COMMENT_ID] = CommentsColumns.FULL_COMMENT_ID
            sCommentsProjectionMap[CommentsColumns.FROM_ID] = CommentsColumns.FULL_FROM_ID
            sCommentsProjectionMap[CommentsColumns.DATE] =
                CommentsColumns.FULL_DATE
            sCommentsProjectionMap[CommentsColumns.TEXT] = CommentsColumns.FULL_TEXT
            sCommentsProjectionMap[CommentsColumns.REPLY_TO_USER] =
                CommentsColumns.FULL_REPLY_TO_USER
            sCommentsProjectionMap[CommentsColumns.REPLY_TO_COMMENT] =
                CommentsColumns.FULL_REPLY_TO_COMMENT
            sCommentsProjectionMap[CommentsColumns.THREADS_COUNT] =
                CommentsColumns.FULL_THREADS_COUNT
            sCommentsProjectionMap[CommentsColumns.THREADS] = CommentsColumns.FULL_THREADS
            sCommentsProjectionMap[CommentsColumns.LIKES] =
                CommentsColumns.FULL_LIKES
            sCommentsProjectionMap[CommentsColumns.USER_LIKES] = CommentsColumns.FULL_USER_LIKES
            sCommentsProjectionMap[CommentsColumns.CAN_LIKE] = CommentsColumns.FULL_CAN_LIKE
            sCommentsProjectionMap[CommentsColumns.CAN_EDIT] =
                CommentsColumns.FULL_CAN_EDIT
            sCommentsProjectionMap[CommentsColumns.ATTACHMENTS_COUNT] =
                CommentsColumns.FULL_ATTACHMENTS_COUNT
            sCommentsProjectionMap[CommentsColumns.DELETED] =
                CommentsColumns.FULL_DELETED
            sCommentsProjectionMap[CommentsColumns.SOURCE_ID] = CommentsColumns.FULL_SOURCE_ID
            sCommentsProjectionMap[CommentsColumns.SOURCE_OWNER_ID] =
                CommentsColumns.FULL_SOURCE_OWNER_ID
            sCommentsProjectionMap[CommentsColumns.SOURCE_TYPE] =
                CommentsColumns.FULL_SOURCE_TYPE
            sCommentsProjectionMap[CommentsColumns.SOURCE_ACCESS_KEY] =
                CommentsColumns.FULL_SOURCE_ACCESS_KEY

            sCommentsMessagesAttachmentsProjectionMap = HashMap()
            sCommentsMessagesAttachmentsProjectionMap[BaseColumns._ID] =
                CommentsAttachmentsColumns.FULL_ID
            sCommentsMessagesAttachmentsProjectionMap[CommentsAttachmentsColumns.C_ID] =
                CommentsAttachmentsColumns.FULL_C_ID
            sCommentsMessagesAttachmentsProjectionMap[CommentsAttachmentsColumns.DATA] =
                CommentsAttachmentsColumns.FULL_DATA

            sPhotoAlbumsProjectionMap = HashMap()
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.ALBUM_ID] =
                PhotoAlbumsColumns.FULL_ALBUM_ID
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.OWNER_ID] =
                PhotoAlbumsColumns.FULL_OWNER_ID
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.TITLE] =
                PhotoAlbumsColumns.FULL_TITLE
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.SIZE] =
                PhotoAlbumsColumns.FULL_SIZE
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.PRIVACY_VIEW] =
                PhotoAlbumsColumns.FULL_PRIVACY_VIEW
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.PRIVACY_COMMENT] =
                PhotoAlbumsColumns.FULL_PRIVACY_COMMENT
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.DESCRIPTION] =
                PhotoAlbumsColumns.FULL_DESCRIPTION
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.CAN_UPLOAD] =
                PhotoAlbumsColumns.FULL_CAN_UPLOAD
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.UPDATED] =
                PhotoAlbumsColumns.FULL_UPDATED
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.CREATED] =
                PhotoAlbumsColumns.FULL_CREATED
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.SIZES] = PhotoAlbumsColumns.FULL_SIZES
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.UPLOAD_BY_ADMINS] =
                PhotoAlbumsColumns.FULL_UPLOAD_BY_ADMINS
            sPhotoAlbumsProjectionMap[PhotoAlbumsColumns.COMMENTS_DISABLED] =
                PhotoAlbumsColumns.FULL_COMMENTS_DISABLED

            //sPollProjectionMap = new HashMap<>();
            //sPollProjectionMap.put(PollColumns._ID, PollColumns.FULL_ID);
            //sPollProjectionMap.put(PollColumns.POLL_ID, PollColumns.FULL_POLL_ID);
            //sPollProjectionMap.put(PollColumns.OWNER_ID, PollColumns.FULL_OWNER_ID);
            //sPollProjectionMap.put(PollColumns.CREATED, PollColumns.FULL_CREATED);
            //sPollProjectionMap.put(PollColumns.QUESTION, PollColumns.FULL_QUESTION);
            //sPollProjectionMap.put(PollColumns.VOTES, PollColumns.FULL_VOTES);
            //sPollProjectionMap.put(PollColumns.ANSWER_ID, PollColumns.FULL_ANSWER_ID);
            //sPollProjectionMap.put(PollColumns.ANSWER_COUNT, PollColumns.FULL_ANSWER_COUNT);
            //sPollProjectionMap.put(PollColumns.ANONYMOUS, PollColumns.FULL_ANONYMOUS);
            //sPollProjectionMap.put(PollColumns.IS_BOARD, PollColumns.FULL_IS_BOARD);
            //sPollProjectionMap.put(PollColumns.ANSWERS, PollColumns.FULL_ANSWERS);

            sNewsProjectionMap = HashMap()
            sNewsProjectionMap[BaseColumns._ID] = NewsColumns.FULL_ID
            sNewsProjectionMap[NewsColumns.TYPE] = NewsColumns.FULL_TYPE
            sNewsProjectionMap[NewsColumns.SOURCE_ID] =
                NewsColumns.FULL_SOURCE_ID
            sNewsProjectionMap[NewsColumns.DATE] = NewsColumns.FULL_DATE
            sNewsProjectionMap[NewsColumns.POST_ID] = NewsColumns.FULL_POST_ID
            sNewsProjectionMap[NewsColumns.POST_TYPE] = NewsColumns.FULL_POST_TYPE
            sNewsProjectionMap[NewsColumns.FINAL_POST] = NewsColumns.FULL_FINAL_POST
            sNewsProjectionMap[NewsColumns.COPY_OWNER_ID] = NewsColumns.FULL_COPY_OWNER_ID
            sNewsProjectionMap[NewsColumns.COPY_POST_ID] =
                NewsColumns.FULL_COPY_POST_ID
            sNewsProjectionMap[NewsColumns.COPY_POST_DATE] = NewsColumns.FULL_COPY_POST_DATE
            sNewsProjectionMap[NewsColumns.TEXT] = NewsColumns.FULL_TEXT
            sNewsProjectionMap[NewsColumns.CAN_EDIT] = NewsColumns.FULL_CAN_EDIT
            sNewsProjectionMap[NewsColumns.CAN_DELETE] =
                NewsColumns.FULL_CAN_DELETE
            sNewsProjectionMap[NewsColumns.COMMENT_COUNT] =
                NewsColumns.FULL_COMMENT_COUNT
            sNewsProjectionMap[NewsColumns.COPYRIGHT_BLOB] =
                NewsColumns.FULL_COPYRIGHT_BLOB
            sNewsProjectionMap[NewsColumns.COMMENT_CAN_POST] = NewsColumns.FULL_COMMENT_CAN_POST
            sNewsProjectionMap[NewsColumns.LIKE_COUNT] = NewsColumns.FULL_LIKE_COUNT
            sNewsProjectionMap[NewsColumns.USER_LIKE] =
                NewsColumns.FULL_USER_LIKE
            sNewsProjectionMap[NewsColumns.CAN_LIKE] =
                NewsColumns.FULL_CAN_LIKE
            sNewsProjectionMap[NewsColumns.IS_DONUT] =
                NewsColumns.FULL_IS_DONUT
            sNewsProjectionMap[NewsColumns.CAN_PUBLISH] = NewsColumns.FULL_CAN_PUBLISH
            sNewsProjectionMap[NewsColumns.REPOSTS_COUNT] =
                NewsColumns.FULL_REPOSTS_COUNT
            sNewsProjectionMap[NewsColumns.USER_REPOSTED] = NewsColumns.FULL_USER_REPOSTED
            sNewsProjectionMap[NewsColumns.TAG_FRIENDS] = NewsColumns.FULL_TAG_FRIENDS
            sNewsProjectionMap[NewsColumns.ATTACHMENTS_BLOB] =
                NewsColumns.FULL_ATTACHMENTS_BLOB
            sNewsProjectionMap[NewsColumns.VIEWS] = NewsColumns.FULL_VIEWS

            sGroupsDetProjectionMap = HashMap()
            sGroupsDetProjectionMap[BaseColumns._ID] =
                GroupsDetColumns.FULL_ID
            sGroupsDetProjectionMap[GroupsDetColumns.DATA] =
                GroupsDetColumns.FULL_DATA

            sVideoAlbumsProjectionMap = HashMap()
            sVideoAlbumsProjectionMap[BaseColumns._ID] =
                VideoAlbumsColumns.FULL_ID
            sVideoAlbumsProjectionMap[VideoAlbumsColumns.ALBUM_ID] =
                VideoAlbumsColumns.FULL_ALBUM_ID
            sVideoAlbumsProjectionMap[VideoAlbumsColumns.OWNER_ID] =
                VideoAlbumsColumns.FULL_OWNER_ID
            sVideoAlbumsProjectionMap[VideoAlbumsColumns.TITLE] =
                VideoAlbumsColumns.FULL_TITLE
            sVideoAlbumsProjectionMap[VideoAlbumsColumns.IMAGE] = VideoAlbumsColumns.FULL_IMAGE
            sVideoAlbumsProjectionMap[VideoAlbumsColumns.COUNT] =
                VideoAlbumsColumns.FULL_COUNT
            sVideoAlbumsProjectionMap[VideoAlbumsColumns.UPDATE_TIME] =
                VideoAlbumsColumns.FULL_UPDATE_TIME
            sVideoAlbumsProjectionMap[VideoAlbumsColumns.PRIVACY] = VideoAlbumsColumns.FULL_PRIVACY

            sTopicsProjectionMap = HashMap()
            sTopicsProjectionMap[BaseColumns._ID] = TopicsColumns.FULL_ID
            sTopicsProjectionMap[TopicsColumns.TOPIC_ID] = TopicsColumns.FULL_TOPIC_ID
            sTopicsProjectionMap[TopicsColumns.OWNER_ID] =
                TopicsColumns.FULL_OWNER_ID
            sTopicsProjectionMap[TopicsColumns.TITLE] =
                TopicsColumns.FULL_TITLE
            sTopicsProjectionMap[TopicsColumns.CREATED] =
                TopicsColumns.FULL_CREATED
            sTopicsProjectionMap[TopicsColumns.CREATED_BY] = TopicsColumns.FULL_CREATED_BY
            sTopicsProjectionMap[TopicsColumns.UPDATED] = TopicsColumns.FULL_UPDATED
            sTopicsProjectionMap[TopicsColumns.UPDATED_BY] = TopicsColumns.FULL_UPDATED_BY
            sTopicsProjectionMap[TopicsColumns.IS_CLOSED] =
                TopicsColumns.FULL_IS_CLOSED
            sTopicsProjectionMap[TopicsColumns.IS_FIXED] = TopicsColumns.FULL_IS_FIXED
            sTopicsProjectionMap[TopicsColumns.COMMENTS] =
                TopicsColumns.FULL_COMMENTS
            sTopicsProjectionMap[TopicsColumns.FIRST_COMMENT] = TopicsColumns.FULL_FIRST_COMMENT
            sTopicsProjectionMap[TopicsColumns.LAST_COMMENT] = TopicsColumns.FULL_LAST_COMMENT
            sTopicsProjectionMap[TopicsColumns.ATTACHED_POLL] = TopicsColumns.FULL_ATTACHED_POLL
            //sTopicsProjectionMap.put(TopicsColumns.POLL_ID, TopicsColumns.FULL_POLL_ID);

            sNoticationsProjectionMap = HashMap()
            sNoticationsProjectionMap[BaseColumns._ID] = NotificationColumns.FULL_ID
            sNoticationsProjectionMap[NotificationColumns.DATE] =
                NotificationColumns.FULL_DATE
            sNoticationsProjectionMap[NotificationColumns.CONTENT_PACK] =
                NotificationColumns.FULL_CONTENT_PACK

            sUserDetProjectionMap = HashMap()
            sUserDetProjectionMap[BaseColumns._ID] = UsersDetColumns.FULL_ID
            sUserDetProjectionMap[UsersDetColumns.DATA] = UsersDetColumns.FULL_DATA

            sFavePhotosProjectionMap = HashMap()
            sFavePhotosProjectionMap[BaseColumns._ID] =
                FavePhotosColumns.FULL_ID
            sFavePhotosProjectionMap[FavePhotosColumns.PHOTO_ID] = FavePhotosColumns.FULL_PHOTO_ID
            sFavePhotosProjectionMap[FavePhotosColumns.OWNER_ID] = FavePhotosColumns.FULL_OWNER_ID
            sFavePhotosProjectionMap[FavePhotosColumns.POST_ID] =
                FavePhotosColumns.FULL_POST_ID
            sFavePhotosProjectionMap[FavePhotosColumns.PHOTO] =
                FavePhotosColumns.FULL_PHOTO

            sFaveVideosProjectionMap = HashMap()
            sFaveVideosProjectionMap[BaseColumns._ID] = FaveVideosColumns.FULL_ID
            sFaveVideosProjectionMap[FaveVideosColumns.VIDEO] =
                FaveVideosColumns.FULL_VIDEO

            sFaveArticlesProjectionMap = HashMap()
            sFaveArticlesProjectionMap[BaseColumns._ID] =
                FaveArticlesColumns.FULL_ID
            sFaveArticlesProjectionMap[FaveArticlesColumns.ARTICLE] =
                FaveArticlesColumns.FULL_ARTICLE

            sFaveProductsProjectionMap = HashMap()
            sFaveProductsProjectionMap[BaseColumns._ID] = FaveProductColumns.FULL_ID
            sFaveProductsProjectionMap[FaveProductColumns.PRODUCT] =
                FaveProductColumns.FULL_PRODUCT

            sFaveUsersProjectionMap = HashMap()
            sFaveUsersProjectionMap[BaseColumns._ID] =
                FavePageColumns.FULL_ID
            sFaveUsersProjectionMap[FavePageColumns.UPDATED_TIME] = FavePageColumns.UPDATED_TIME
            sFaveUsersProjectionMap[FavePageColumns.DESCRIPTION] =
                FavePageColumns.DESCRIPTION
            sFaveUsersProjectionMap[FavePageColumns.FAVE_TYPE] = FavePageColumns.FAVE_TYPE

            sFaveGroupsProjectionMap = HashMap()
            sFaveGroupsProjectionMap[BaseColumns._ID] =
                FavePageColumns.FULL_GROUPS_ID
            sFaveGroupsProjectionMap[FavePageColumns.UPDATED_TIME] =
                FavePageColumns.UPDATED_TIME
            sFaveGroupsProjectionMap[FavePageColumns.DESCRIPTION] = FavePageColumns.DESCRIPTION
            sFaveGroupsProjectionMap[FavePageColumns.FAVE_TYPE] = FavePageColumns.FAVE_TYPE

            sFaveLinksProjectionMap = HashMap()
            sFaveLinksProjectionMap[BaseColumns._ID] =
                FaveLinksColumns.FULL_ID
            sFaveLinksProjectionMap[FaveLinksColumns.LINK_ID] = FaveLinksColumns.FULL_LINK_ID
            sFaveLinksProjectionMap[FaveLinksColumns.URL] =
                FaveLinksColumns.FULL_URL
            sFaveLinksProjectionMap[FaveLinksColumns.TITLE] = FaveLinksColumns.FULL_TITLE
            sFaveLinksProjectionMap[FaveLinksColumns.DESCRIPTION] =
                FaveLinksColumns.FULL_DESCRIPTION
            sFaveLinksProjectionMap[FaveLinksColumns.PHOTO] =
                FaveLinksColumns.FULL_PHOTO

            sFavePostsProjectionMap = HashMap()
            sFavePostsProjectionMap[BaseColumns._ID] =
                FavePostsColumns.FULL_ID
            sFavePostsProjectionMap[FavePostsColumns.POST] = FavePostsColumns.FULL_POST

            sCountriesProjectionMap = HashMap()
            sCountriesProjectionMap[BaseColumns._ID] = CountriesColumns.FULL_ID
            sCountriesProjectionMap[CountriesColumns.NAME] = CountriesColumns.FULL_NAME

            sFeedListsProjectionMap = HashMap()
            sFeedListsProjectionMap[BaseColumns._ID] =
                FeedListsColumns.FULL_ID
            sFeedListsProjectionMap[FeedListsColumns.TITLE] =
                FeedListsColumns.FULL_TITLE
            sFeedListsProjectionMap[FeedListsColumns.NO_REPOSTS] = FeedListsColumns.FULL_NO_REPOSTS
            sFeedListsProjectionMap[FeedListsColumns.SOURCE_IDS] =
                FeedListsColumns.FULL_SOURCE_IDS

            sFriendListsProjectionMap = HashMap()
            sFriendListsProjectionMap[BaseColumns._ID] = FriendListsColumns.FULL_ID
            sFriendListsProjectionMap[FriendListsColumns.USER_ID] =
                FriendListsColumns.FULL_USER_ID
            sFriendListsProjectionMap[FriendListsColumns.LIST_ID] = FriendListsColumns.FULL_LIST_ID
            sFriendListsProjectionMap[FriendListsColumns.NAME] =
                FriendListsColumns.FULL_NAME

            sKeysProjectionMap = HashMap()
            sKeysProjectionMap[BaseColumns._ID] = KeyColumns.FULL_ID
            sKeysProjectionMap[KeyColumns.VERSION] = KeyColumns.FULL_VERSION
            sKeysProjectionMap[KeyColumns.PEER_ID] =
                KeyColumns.FULL_PEER_ID
            sKeysProjectionMap[KeyColumns.SESSION_ID] =
                KeyColumns.FULL_SESSION_ID
            sKeysProjectionMap[KeyColumns.DATE] = KeyColumns.FULL_DATE
            sKeysProjectionMap[KeyColumns.START_SESSION_MESSAGE_ID] =
                KeyColumns.FULL_START_SESSION_MESSAGE_ID
            sKeysProjectionMap[KeyColumns.END_SESSION_MESSAGE_ID] =
                KeyColumns.FULL_END_SESSION_MESSAGE_ID
            sKeysProjectionMap[KeyColumns.OUT_KEY] = KeyColumns.FULL_OUT_KEY
            sKeysProjectionMap[KeyColumns.IN_KEY] =
                KeyColumns.FULL_IN_KEY

            //testProjectionMaps()
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    private fun getDbHelper(aid: Long): DBHelper {
        return DBHelper.getInstance(context!!, aid)
    }

    private fun extractAidFromUri(uri: Uri): Long {
        val said = uri.getQueryParameter(AID)
        require(said.nonNullNoEmpty()) { "AID query parameter not found, uri: $uri" }
        val targetAid = said.toLong()
        require(targetAid != 0L) { "Invalid account id=0, uri: $uri" }
        return targetAid
    }

    /**
     * Проверяем все операции на соответствие aid
     * Потому что будет открыватся транзакция только к одной базе данных
     */
    private fun validateUris(operations: List<ContentProviderOperation>) {
        var aid: Long? = null
        for (operation in operations) {
            val uri: Uri = operation.uri
            if (aid == null) {
                aid = extractAidFromUri(uri)
            }
            val thisAid = extractAidFromUri(uri)
            require(aid == thisAid) { "There are different aids in operations" }
        }
    }

    override fun applyBatch(operations: ArrayList<ContentProviderOperation>): Array<ContentProviderResult?> {
        if (operations.isEmpty()) {
            return arrayOfNulls(0)
        }
        validateUris(operations)
        val aid = extractAidFromUri(operations[0].uri)
        val result: Array<ContentProviderResult?> =
            arrayOfNulls(operations.size)
        var i = 0
        // Opens the database object in "write" mode.
        val db: SQLiteDatabase = getDbHelper(aid).writableDatabase
        // Begin a transaction
        db.beginTransaction()
        try {
            for (operation in operations) {
                // Chain the result for back references
                result[i++] = operation.apply(this, result, i)
            }
            db.setTransactionSuccessful()
        } catch (e: OperationApplicationException) {
            Logger.d("DATABASE", "batch failed: " + e.localizedMessage)
        } finally {
            db.endTransaction()
        }
        return result
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val db: SQLiteDatabase = getDbHelper(uri).writableDatabase
        val rowId: Long
        val resultUri: Uri
        val matchUri: Int = sUriMatcher.match(uri)
        when (matchUri) {
            URI_USERS -> {
                rowId = db.replace(UserColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(USER_CONTENT_URI, rowId)
            }

            URI_MESSAGES -> {
                rowId = db.replace(MessageColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(MESSAGE_CONTENT_URI, rowId)
            }

            URI_MESSAGES_ATTACHMENTS -> {
                rowId = db.replace(MessagesAttachmentsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(MESSAGES_ATTACHMENTS_CONTENT_URI, rowId)
            }

            URI_PHOTOS -> {
                rowId = db.replace(PhotosColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(PHOTOS_CONTENT_URI, rowId)
            }

            URI_PHOTOS_EXTENDED -> {
                rowId = db.replace(PhotosExtendedColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(PHOTOS_EXTENDED_CONTENT_URI, rowId)
            }

            URI_DIALOGS -> {
                rowId = db.replace(DialogsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(DIALOGS_CONTENT_URI, rowId)
            }

            URI_PEERS -> {
                rowId = db.replace(PeersColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(PEERS_CONTENT_URI, rowId)
            }

            URI_DOCS -> {
                rowId = db.replace(DocColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(DOCS_CONTENT_URI, rowId)
            }

            URI_VIDEOS -> {
                rowId = db.replace(VideoColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(VIDEOS_CONTENT_URI, rowId)
            }

            URI_POSTS -> {
                rowId = db.replace(PostsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(POSTS_CONTENT_URI, rowId)
            }

            URI_POST_ATTACHMENTS -> {
                rowId = db.replace(WallAttachmentsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(POSTS_ATTACHMENTS_CONTENT_URI, rowId)
            }

            URI_GROUPS -> {
                rowId = db.replace(GroupColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(GROUPS_CONTENT_URI, rowId)
            }

            URI_RELATIVESHIP -> {
                rowId = db.replace(RelationshipColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(RELATIVESHIP_CONTENT_URI, rowId)
            }

            URI_COMMENTS -> {
                rowId = db.replace(CommentsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(COMMENTS_CONTENT_URI, rowId)
            }

            URI_COMMENTS_ATTACHMENTS -> {
                rowId = db.replace(CommentsAttachmentsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(COMMENTS_ATTACHMENTS_CONTENT_URI, rowId)
            }

            URI_PHOTO_ALBUMS -> {
                rowId = db.replace(PhotoAlbumsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(PHOTO_ALBUMS_CONTENT_URI, rowId)
            }

            URI_NEWS -> {
                rowId = db.replace(NewsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(NEWS_CONTENT_URI, rowId)
            }

            URI_GROUPS_DET -> {
                rowId = db.replace(GroupsDetColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(GROUPS_DET_CONTENT_URI, rowId)
            }

            URI_VIDEO_ALBUMS -> {
                rowId = db.replace(VideoAlbumsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(VIDEO_ALBUMS_CONTENT_URI, rowId)
            }

            URI_TOPICS -> {
                rowId = db.replace(TopicsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(TOPICS_CONTENT_URI, rowId)
            }

            URI_NOTIFICATIONS -> {
                rowId = db.replace(NotificationColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(NOTIFICATIONS_CONTENT_URI, rowId)
            }

            URI_USER_DET -> {
                rowId = db.replace(UsersDetColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(USER_DET_CONTENT_URI, rowId)
            }

            URI_FAVE_PHOTOS -> {
                rowId = db.replace(FavePhotosColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(FAVE_PHOTOS_CONTENT_URI, rowId)
            }

            URI_FAVE_VIDEOS -> {
                rowId = db.replace(FaveVideosColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(FAVE_VIDEOS_CONTENT_URI, rowId)
            }

            URI_FAVE_PAGES -> {
                rowId = db.replace(FavePageColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(FAVE_PAGES_CONTENT_URI, rowId)
            }

            URI_FAVE_GROUPS -> {
                rowId = db.replace(FavePageColumns.GROUPSTABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(FAVE_GROUPS_CONTENT_URI, rowId)
            }

            URI_FAVE_LINKS -> {
                rowId = db.replace(FaveLinksColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(FAVE_LINKS_CONTENT_URI, rowId)
            }

            URI_FAVE_ARTICLES -> {
                rowId = db.replace(FaveArticlesColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(FAVE_ARTICLES_CONTENT_URI, rowId)
            }

            URI_FAVE_PRODUCTS -> {
                rowId = db.replace(FaveProductColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(FAVE_PRODUCTS_CONTENT_URI, rowId)
            }

            URI_FAVE_POSTS -> {
                rowId = db.replace(FavePostsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(FAVE_POSTS_CONTENT_URI, rowId)
            }

            URI_COUNTRIES -> {
                rowId = db.replace(CountriesColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(COUNTRIES_CONTENT_URI, rowId)
            }

            URI_FEED_LISTS -> {
                rowId = db.replace(FeedListsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(FEED_LISTS_CONTENT_URI, rowId)
            }

            URI_FRIEND_LISTS -> {
                rowId = db.replace(FriendListsColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(FRIEND_LISTS_CONTENT_URI, rowId)
            }

            URI_KEYS -> {
                rowId = db.replace(KeyColumns.TABLENAME, null, values)
                resultUri = ContentUris.withAppendedId(KEYS_CONTENT_URI, rowId)
            }

            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        safeNotifyChange(resultUri)
        if (matchUri == URI_MESSAGES && values != null) {
            val peerId: Long = values.getAsLong(MessageColumns.PEER_ID)
            val dUri: Uri = ContentUris.withAppendedId(DIALOGS_CONTENT_URI, peerId)
            safeNotifyChange(dUri)
        }
        return resultUri
    }

    private fun safeNotifyChange(uri: Uri) {
        context?.contentResolver?.notifyChange(uri, null)
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val _QB = SQLiteQueryBuilder()
        val _TableType: Int = when (sUriMatcher.match(uri)) {
            URI_USERS -> {
                _QB.tables = UserColumns.TABLENAME
                _QB.projectionMap = sUsersProjectionMap
                URI_USERS
            }

            URI_USERS_ID -> {
                _QB.tables = UserColumns.TABLENAME
                _QB.projectionMap = sUsersProjectionMap
                _QB.appendWhere(UserColumns.FULL_ID + "=" + uri.pathSegments[1])
                URI_USERS
            }

            URI_GROUPS -> {
                _QB.tables = GroupColumns.TABLENAME
                _QB.projectionMap = sGroupsProjectionMap
                URI_GROUPS
            }

            URI_GROUPS_ID -> {
                _QB.tables = GroupColumns.TABLENAME
                _QB.projectionMap = sGroupsProjectionMap
                _QB.appendWhere(GroupColumns.FULL_ID + "=" + uri.pathSegments[1])
                URI_GROUPS
            }

            URI_MESSAGES -> {
                _QB.tables = MessageColumns.TABLENAME
                //" LEFT OUTER JOIN " + PeerColumns.TABLENAME + " ON " + MessageColumns.FULL_FROM_ID + " = " + PeerColumns.FULL_ID +
                //" LEFT OUTER JOIN " + UserColumns.TABLENAME + " ON " + MessageColumns.FULL_ACTION_MID + " = " + UserColumns.FULL_ID);
                _QB.projectionMap = sMessagesProjectionMap
                URI_MESSAGES
            }

            URI_MESSAGES_ID -> {
                _QB.tables = MessageColumns.TABLENAME
                //" LEFT OUTER JOIN " + PeerColumns.TABLENAME + " ON " + MessageColumns.FULL_FROM_ID + " = " + PeerColumns.FULL_ID +
                //" LEFT OUTER JOIN " + UserColumns.TABLENAME + " ON " + MessageColumns.FULL_ACTION_MID + " = " + UserColumns.FULL_ID);
                _QB.projectionMap = sMessagesProjectionMap
                _QB.appendWhere(MessageColumns.FULL_ID + "=" + uri.pathSegments[1])
                URI_MESSAGES
            }

            URI_MESSAGES_ATTACHMENTS -> {
                _QB.tables = MessagesAttachmentsColumns.TABLENAME
                //" LEFT OUTER JOIN " + AudiosColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + AudiosColumns.FULL_AUDIO_ID + " AND " + AttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + AudiosColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + StickersColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + StickersColumns.FULL_ID +
                //" LEFT OUTER JOIN " + DocColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + DocColumns.FULL_DOC_ID + " AND " + AttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + DocColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + VideoColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + VideoColumns.FULL_VIDEO_ID + " AND " + AttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + VideoColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + PostsColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + PostsColumns.FULL_ID + " AND " + AttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + PostsColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + PhotosColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + PhotosColumns.FULL_PHOTO_ID + " AND " + AttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + PhotosColumns.FULL_OWNER_ID);
                _QB.projectionMap = sMessagesAttachmentsProjectionMap
                URI_MESSAGES_ATTACHMENTS
            }

            URI_MESSAGES_ATTACHMENTS_ID -> {
                _QB.tables = MessagesAttachmentsColumns.TABLENAME
                //" LEFT OUTER JOIN " + AudiosColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + AudiosColumns.FULL_AUDIO_ID + " AND " + AttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + AudiosColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + StickersColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + StickersColumns.FULL_ID +
                //" LEFT OUTER JOIN " + DocColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + DocColumns.FULL_DOC_ID + " AND " + AttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + DocColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + VideoColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + VideoColumns.FULL_VIDEO_ID + " AND " + AttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + VideoColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + PostsColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + PostsColumns.FULL_ID + " AND " + AttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + PostsColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + PhotosColumns.TABLENAME + " ON " + AttachmentsColumns.FULL_ATTACHMENT_ID + " = " + PhotosColumns.FULL_PHOTO_ID + " AND " + AttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + PhotosColumns.FULL_OWNER_ID);
                _QB.projectionMap = sMessagesAttachmentsProjectionMap
                _QB.appendWhere(MessagesAttachmentsColumns.FULL_ID + "=" + uri.pathSegments[1])
                URI_MESSAGES_ATTACHMENTS
            }

            URI_PHOTOS -> {
                _QB.tables = PhotosColumns.TABLENAME
                _QB.projectionMap = sPhotosProjectionMap
                URI_PHOTOS
            }

            URI_PHOTOS_EXTENDED -> {
                _QB.tables = PhotosExtendedColumns.TABLENAME
                _QB.projectionMap = sPhotosExtendedProjectionMap
                URI_PHOTOS_EXTENDED
            }

            URI_PHOTOS_ID -> {
                _QB.tables = PhotosColumns.TABLENAME
                _QB.projectionMap = sPhotosProjectionMap
                _QB.appendWhere(PhotosColumns.FULL_ID + "=" + uri.pathSegments[1])
                URI_PHOTOS
            }

            URI_DIALOGS -> {
                _QB.tables =
                    DialogsColumns.TABLENAME + " LEFT OUTER JOIN " + MessageColumns.TABLENAME + " ON " + DialogsColumns.FULL_LAST_MESSAGE_ID + " = " + MessageColumns.FULL_ID
                _QB.projectionMap = sDialogsProjectionMap
                URI_DIALOGS
            }

            URI_PEERS -> {
                _QB.tables = PeersColumns.TABLENAME
                _QB.projectionMap = sPeersProjectionMap
                URI_PEERS
            }

            URI_DOCS -> {
                _QB.tables = DocColumns.TABLENAME
                _QB.projectionMap = sDocsProjectionMap
                URI_DOCS
            }

            URI_DOCS_ID -> {
                _QB.tables = DocColumns.TABLENAME
                _QB.projectionMap = sDocsProjectionMap
                _QB.appendWhere(DocColumns.FULL_ID + "=" + uri.pathSegments[1])
                URI_DOCS
            }

            URI_VIDEOS -> {
                _QB.tables = VideoColumns.TABLENAME
                _QB.projectionMap = sVideosProjectionMap
                URI_VIDEOS
            }

            URI_VIDEOS_ID -> {
                _QB.tables = VideoColumns.TABLENAME
                _QB.projectionMap = sVideosProjectionMap
                _QB.appendWhere(VideoColumns.FULL_ID + "=" + uri.pathSegments[1])
                URI_DOCS
            }

            URI_POSTS -> {
                _QB.tables = PostsColumns.TABLENAME
                _QB.projectionMap = sPostsProjectionMap
                URI_POSTS
            }

            URI_POSTS_ID -> {
                _QB.tables = PostsColumns.TABLENAME
                _QB.projectionMap = sPostsProjectionMap
                _QB.appendWhere(PostsColumns.FULL_ID + " = " + uri.pathSegments[1])
                URI_POSTS
            }

            URI_POST_ATTACHMENTS -> {
                _QB.tables = WallAttachmentsColumns.TABLENAME
                _QB.projectionMap = sPostsMessagesAttachmentsProjectionMap
                URI_POST_ATTACHMENTS
            }

            URI_POST_ATTACHMENTS_ID -> {
                _QB.tables = WallAttachmentsColumns.TABLENAME
                _QB.projectionMap = sPostsMessagesAttachmentsProjectionMap
                _QB.appendWhere(WallAttachmentsColumns.FULL_ID + "=" + uri.pathSegments[1])
                URI_POST_ATTACHMENTS
            }

            URI_RELATIVESHIP -> {
                _QB.tables = RelationshipColumns.TABLENAME +
                        " LEFT OUTER JOIN " + UserColumns.TABLENAME + " ON " + RelationshipColumns.FULL_SUBJECT_ID + " = " + UserColumns.FULL_ID +
                        " LEFT OUTER JOIN " + GroupColumns.TABLENAME + " ON -" + RelationshipColumns.FULL_SUBJECT_ID + " = " + GroupColumns.FULL_ID
                _QB.projectionMap = sRelativeshipProjectionMap
                URI_RELATIVESHIP
            }

            URI_COMMENTS -> {
                _QB.tables = CommentsColumns.TABLENAME
                _QB.projectionMap = sCommentsProjectionMap
                URI_COMMENTS
            }

            URI_COMMENTS_ID -> {
                _QB.tables = CommentsColumns.TABLENAME
                _QB.projectionMap = sCommentsProjectionMap
                _QB.appendWhere(BaseColumns._ID + " = " + uri.pathSegments[1])
                URI_COMMENTS
            }

            URI_COMMENTS_ATTACHMENTS -> {
                _QB.tables = CommentsAttachmentsColumns.TABLENAME
                //" LEFT OUTER JOIN " + AudiosColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + AudiosColumns.FULL_AUDIO_ID + " AND " + CommentsAttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + AudiosColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + StickersColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + StickersColumns.FULL_ID +
                //" LEFT OUTER JOIN " + DocColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + DocColumns.FULL_DOC_ID + " AND " + CommentsAttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + DocColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + VideoColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + VideoColumns.FULL_VIDEO_ID + " AND " + CommentsAttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + VideoColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + PostsColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + PostsColumns.FULL_ID + " AND " + CommentsAttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + PostsColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + PhotosColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + PhotosColumns.FULL_PHOTO_ID + " AND " + CommentsAttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + PhotosColumns.FULL_OWNER_ID);
                _QB.projectionMap = sCommentsMessagesAttachmentsProjectionMap
                URI_COMMENTS_ATTACHMENTS
            }

            URI_COMMENTS_ATTACHMENTS_ID -> {
                _QB.tables = CommentsAttachmentsColumns.TABLENAME
                //" LEFT OUTER JOIN " + AudiosColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + AudiosColumns.FULL_AUDIO_ID + " AND " + CommentsAttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + AudiosColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + StickersColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + StickersColumns.FULL_ID +
                //" LEFT OUTER JOIN " + DocColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + DocColumns.FULL_DOC_ID + " AND " + CommentsAttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + DocColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + VideoColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + VideoColumns.FULL_VIDEO_ID + " AND " + CommentsAttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + VideoColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + PostsColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + PostsColumns.FULL_ID + " AND " + CommentsAttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + PostsColumns.FULL_OWNER_ID +
                //" LEFT OUTER JOIN " + PhotosColumns.TABLENAME + " ON " + CommentsAttachmentsColumns.FULL_ATTACHMENT_ID + " = " + PhotosColumns.FULL_PHOTO_ID + " AND " + CommentsAttachmentsColumns.FULL_ATTACHMENT_OWNER_ID + " = " + PhotosColumns.FULL_OWNER_ID);
                _QB.projectionMap = sCommentsMessagesAttachmentsProjectionMap
                _QB.appendWhere(CommentsAttachmentsColumns.FULL_ID + "=" + uri.pathSegments[1])
                URI_COMMENTS_ATTACHMENTS
            }

            URI_PHOTO_ALBUMS -> {
                _QB.tables = PhotoAlbumsColumns.TABLENAME
                _QB.projectionMap = sPhotoAlbumsProjectionMap
                URI_PHOTO_ALBUMS
            }

            URI_NEWS -> {
                _QB.tables = NewsColumns.TABLENAME
                _QB.projectionMap = sNewsProjectionMap
                URI_NEWS
            }

            URI_GROUPS_DET -> {
                _QB.tables = GroupsDetColumns.TABLENAME
                _QB.projectionMap = sGroupsDetProjectionMap
                URI_GROUPS_DET
            }

            URI_GROUPS_DET_ID -> {
                _QB.tables = GroupsDetColumns.TABLENAME
                _QB.projectionMap = sGroupsDetProjectionMap
                _QB.appendWhere(GroupsDetColumns.FULL_ID + " = " + uri.pathSegments[1])
                URI_GROUPS_DET
            }

            URI_VIDEO_ALBUMS -> {
                _QB.tables = VideoAlbumsColumns.TABLENAME
                _QB.projectionMap = sVideoAlbumsProjectionMap
                URI_VIDEO_ALBUMS
            }

            URI_TOPICS -> {
                _QB.tables = TopicsColumns.TABLENAME
                _QB.projectionMap = sTopicsProjectionMap
                URI_TOPICS
            }

            URI_NOTIFICATIONS -> {
                _QB.tables = NotificationColumns.TABLENAME
                _QB.projectionMap = sNoticationsProjectionMap
                URI_NOTIFICATIONS
            }

            URI_USER_DET -> {
                _QB.tables = UsersDetColumns.TABLENAME
                _QB.projectionMap = sUserDetProjectionMap
                URI_USER_DET
            }

            URI_USER_DET_ID -> {
                _QB.tables = UsersDetColumns.TABLENAME
                _QB.projectionMap = sUserDetProjectionMap
                _QB.appendWhere(UsersDetColumns.FULL_ID + " = " + uri.pathSegments[1])
                URI_USER_DET
            }

            URI_FAVE_PHOTOS -> {
                _QB.tables = FavePhotosColumns.TABLENAME
                _QB.projectionMap = sFavePhotosProjectionMap
                URI_FAVE_PHOTOS
            }

            URI_FAVE_VIDEOS -> {
                _QB.tables = FaveVideosColumns.TABLENAME
                _QB.projectionMap = sFaveVideosProjectionMap
                URI_FAVE_VIDEOS
            }

            URI_FAVE_ARTICLES -> {
                _QB.tables = FaveArticlesColumns.TABLENAME
                _QB.projectionMap = sFaveArticlesProjectionMap
                URI_FAVE_ARTICLES
            }

            URI_FAVE_PRODUCTS -> {
                _QB.tables = FaveProductColumns.TABLENAME
                _QB.projectionMap = sFaveProductsProjectionMap
                URI_FAVE_PRODUCTS
            }

            URI_FAVE_PAGES -> {
                _QB.tables = FavePageColumns.TABLENAME +
                        " LEFT OUTER JOIN " + UserColumns.TABLENAME +
                        " users ON " + FavePageColumns.FULL_ID + " = users." + BaseColumns._ID
                _QB.projectionMap = sFaveUsersProjectionMap
                URI_FAVE_PAGES
            }

            URI_FAVE_GROUPS -> {
                _QB.tables = FavePageColumns.GROUPSTABLENAME +
                        " LEFT OUTER JOIN " + GroupColumns.TABLENAME +
                        " groups ON " + FavePageColumns.FULL_GROUPS_ID + " = groups." + BaseColumns._ID
                _QB.projectionMap = sFaveGroupsProjectionMap
                URI_FAVE_GROUPS
            }

            URI_FAVE_LINKS -> {
                _QB.tables = FaveLinksColumns.TABLENAME
                _QB.projectionMap = sFaveLinksProjectionMap
                URI_FAVE_LINKS
            }

            URI_FAVE_POSTS -> {
                _QB.tables = FavePostsColumns.TABLENAME
                _QB.projectionMap = sFavePostsProjectionMap
                URI_FAVE_POSTS
            }

            URI_COUNTRIES -> {
                _QB.tables = CountriesColumns.TABLENAME
                _QB.projectionMap = sCountriesProjectionMap
                URI_COUNTRIES
            }

            URI_FEED_LISTS -> {
                _QB.tables = FeedListsColumns.TABLENAME
                _QB.projectionMap = sFeedListsProjectionMap
                URI_FEED_LISTS
            }

            URI_FRIEND_LISTS -> {
                _QB.tables = FriendListsColumns.TABLENAME
                _QB.projectionMap = sFriendListsProjectionMap
                URI_FRIEND_LISTS
            }

            URI_KEYS -> {
                _QB.tables = KeyColumns.TABLENAME
                _QB.projectionMap = sKeysProjectionMap
                URI_KEYS
            }

            else -> throw IllegalArgumentException("Unknown URI $uri")
        }

        //Set your sort order here
        val _OrderBy: String = if (sortOrder.isNullOrEmpty()) {
            // If no sort order is specified use the default
            when (_TableType) {
                URI_USERS -> UserColumns.FULL_LAST_NAME + " ASC"
                URI_GROUPS -> GroupColumns.FULL_NAME + " ASC"
                URI_MESSAGES -> MessageColumns.FULL_STATUS + ", " + MessageColumns.FULL_ID + " ASC"
                URI_MESSAGES_ATTACHMENTS -> MessagesAttachmentsColumns.FULL_ID + " ASC"
                URI_PHOTOS -> PhotosColumns.FULL_ID + " ASC"
                URI_PHOTOS_EXTENDED -> PhotosExtendedColumns.FULL_ID + " ASC"
                URI_DIALOGS -> MessageColumns.FULL_DATE + " DESC"
                URI_PEERS -> PeersColumns.FULL_ID + " DESC"
                URI_DOCS -> DocColumns.FULL_ID + " ASC"
                URI_VIDEOS -> VideoColumns.FULL_ID + " ASC"
                URI_POSTS -> PostsColumns.FULL_ID + " ASC"
                URI_POST_ATTACHMENTS -> WallAttachmentsColumns.FULL_ID + " ASC"
                URI_RELATIVESHIP -> RelationshipColumns.FULL_ID + " ASC"
                URI_COMMENTS -> CommentsColumns.FULL_COMMENT_ID + " ASC"
                URI_COMMENTS_ATTACHMENTS -> CommentsAttachmentsColumns.FULL_ID + " ASC"
                URI_PHOTO_ALBUMS -> PhotoAlbumsColumns.FULL_ID + " ASC"
                URI_NEWS -> NewsColumns.FULL_ID + " ASC"
                URI_GROUPS_DET -> GroupsDetColumns.FULL_ID + " ASC"
                URI_VIDEO_ALBUMS -> VideoAlbumsColumns.FULL_ID + " ASC"
                URI_TOPICS -> TopicsColumns.FULL_ID + " ASC"
                URI_NOTIFICATIONS -> NotificationColumns.FULL_ID + " ASC"
                URI_USER_DET -> UsersDetColumns.FULL_ID + " ASC"
                URI_FAVE_PHOTOS -> FavePhotosColumns.FULL_ID + " ASC"
                URI_FAVE_VIDEOS -> FaveVideosColumns.FULL_ID + " ASC"
                URI_FAVE_ARTICLES -> FaveArticlesColumns.FULL_ID + " ASC"
                URI_FAVE_PRODUCTS -> FaveProductColumns.FULL_ID + " ASC"
                URI_FAVE_PAGES -> FavePageColumns.UPDATED_TIME + " DESC"
                URI_FAVE_GROUPS -> FavePageColumns.UPDATED_TIME + " DESC"
                URI_FAVE_LINKS -> FaveLinksColumns.FULL_ID + " ASC"
                URI_FAVE_POSTS -> FavePostsColumns.FULL_ID + " ASC"
                URI_COUNTRIES -> CountriesColumns.FULL_ID + " ASC"
                URI_FEED_LISTS -> FeedListsColumns.FULL_ID + " ASC"
                URI_FRIEND_LISTS -> FriendListsColumns.FULL_ID + " ASC"
                URI_KEYS -> KeyColumns.FULL_ID + " ASC"
                else -> throw UnknownError("Unknown table type for sort order")
            }
        } else {
            sortOrder
        }

        // Get the database and run the query
        //SQLiteDatabase _DB = getDbHelper(uri).getReadableDatabase();
        val _DB: SQLiteDatabase = getDbHelper(uri).writableDatabase
        val _Result: Cursor =
            _QB.query(_DB, projection, selection, selectionArgs, null, null, _OrderBy)

        // Tell the cursor what uri to watch, so it knows when its source data changes
        if (context != null) {
            _Result.setNotificationUri(context!!.contentResolver, uri)
        }
        return _Result
    }

    private fun getDbHelper(uri: Uri): DBHelper {
        return getDbHelper(extractAidFromUri(uri))
    }

    override fun getType(uri: Uri): String? {
        when (sUriMatcher.match(uri)) {
            URI_USERS -> return USER_CONTENT_TYPE
            URI_USERS_ID -> return USER_CONTENT_ITEM_TYPE
            URI_MESSAGES -> return MESSAGE_CONTENT_TYPE
            URI_MESSAGES_ID -> return MESSAGE_CONTENT_ITEM_TYPE
            URI_MESSAGES_ATTACHMENTS -> return MESSAGES_ATTACHMENTS_CONTENT_TYPE
            URI_MESSAGES_ATTACHMENTS_ID -> return MESSAGES_ATTACHMENTS_CONTENT_ITEM_TYPE
            URI_PHOTOS -> return PHOTOS_CONTENT_TYPE
            URI_PHOTOS_EXTENDED -> return PHOTOS_EXTENDED_CONTENT_TYPE
            URI_PHOTOS_ID -> return PHOTOS_CONTENT_ITEM_TYPE
            URI_DIALOGS -> return DIALOGS_CONTENT_TYPE
            URI_PEERS -> return PEERS_CONTENT_TYPE
            URI_DOCS -> return DOCS_CONTENT_TYPE
            URI_DOCS_ID -> return DOCS_CONTENT_ITEM_TYPE
            URI_VIDEOS -> return VIDEOS_CONTENT_TYPE
            URI_VIDEOS_ID -> return VIDEOS_CONTENT_ITEM_TYPE
            URI_POSTS -> return POSTS_CONTENT_TYPE
            URI_POSTS_ID -> return POSTS_CONTENT_ITEM_TYPE
            URI_POST_ATTACHMENTS -> return POSTS_ATTACHMENTS_CONTENT_TYPE
            URI_POST_ATTACHMENTS_ID -> return POSTS_ATTACHMENTS_CONTENT_ITEM_TYPE
            URI_GROUPS -> return GROUPS_CONTENT_TYPE
            URI_GROUPS_ID -> return GROUPS_CONTENT_ITEM_TYPE
            URI_RELATIVESHIP -> return RELATIVESHIP_CONTENT_TYPE
            URI_COMMENTS -> return COMMENTS_CONTENT_TYPE
            URI_COMMENTS_ID -> return COMMENTS_CONTENT_ITEM_TYPE
            URI_COMMENTS_ATTACHMENTS -> return COMMENTS_ATTACHMENTS_CONTENT_TYPE
            URI_COMMENTS_ATTACHMENTS_ID -> return COMMENTS_ATTACHMENTS_CONTENT_ITEM_TYPE
            URI_PHOTO_ALBUMS -> return PHOTO_ALBUMS_CONTENT_TYPE
            URI_NEWS -> return NEWS_CONTENT_TYPE
            URI_GROUPS_DET -> return GROUPS_DET_CONTENT_TYPE
            URI_GROUPS_DET_ID -> return GROUPS_DET_CONTENT_ITEM_TYPE
            URI_VIDEO_ALBUMS -> return VIDEO_ALBUMS_CONTENT_TYPE
            URI_TOPICS -> return TOPICS_CONTENT_TYPE
            URI_NOTIFICATIONS -> return NOTIFICATIONS_CONTENT_TYPE
            URI_USER_DET -> return USER_DET_CONTENT_TYPE
            URI_USER_DET_ID -> return USER_DET_CONTENT_ITEM_TYPE
            URI_FAVE_PHOTOS -> return FAVE_PHOTOS_CONTENT_TYPE
            URI_FAVE_VIDEOS -> return FAVE_VIDEOS_CONTENT_TYPE
            URI_FAVE_ARTICLES -> return FAVE_ARTICLES_CONTENT_TYPE
            URI_FAVE_PRODUCTS -> return FAVE_PRODUCTS_CONTENT_TYPE
            URI_FAVE_PAGES -> return FAVE_PAGES_CONTENT_TYPE
            URI_FAVE_GROUPS -> return FAVE_GROUPS_CONTENT_TYPE
            URI_FAVE_LINKS -> return FAVE_LINKS_CONTENT_TYPE
            URI_FAVE_POSTS -> return FAVE_POSTS_CONTENT_TYPE
            URI_COUNTRIES -> return COUNTRIES_CONTENT_TYPE
            URI_FEED_LISTS -> return FEED_LISTS_CONTENT_TYPE
            URI_FRIEND_LISTS -> return FRIEND_LISTS_CONTENT_TYPE
            URI_KEYS -> return KEYS_CONTENT_TYPE
        }
        return null
    }

    override fun delete(uri: Uri, pSelection: String?, selectionArgs: Array<String>?): Int {
        var selection = pSelection
        val tbName: String
        when (sUriMatcher.match(uri)) {
            URI_MESSAGES -> tbName = MessageColumns.TABLENAME
            URI_MESSAGES_ID -> {
                val id = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    MessageColumns._ID + " = " + id
                } else {
                    selection + " AND " + MessageColumns._ID + " = " + id
                }
                tbName = MessageColumns.TABLENAME
            }

            URI_DIALOGS -> tbName = DialogsColumns.TABLENAME
            URI_RELATIVESHIP -> tbName = RelationshipColumns.TABLENAME
            URI_POSTS -> tbName = PostsColumns.TABLENAME
            URI_POSTS_ID -> {
                val postId = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    BaseColumns._ID + " = " + postId
                } else {
                    selection + " AND " + BaseColumns._ID + " = " + postId
                }
                tbName = PostsColumns.TABLENAME
            }

            URI_PHOTOS -> tbName = PhotosColumns.TABLENAME
            URI_PHOTOS_EXTENDED -> tbName = PhotosExtendedColumns.TABLENAME
            URI_MESSAGES_ATTACHMENTS -> tbName = MessagesAttachmentsColumns.TABLENAME
            URI_COMMENTS -> tbName = CommentsColumns.TABLENAME
            URI_PHOTO_ALBUMS -> tbName = PhotoAlbumsColumns.TABLENAME
            URI_POST_ATTACHMENTS -> tbName = WallAttachmentsColumns.TABLENAME
            URI_COMMENTS_ATTACHMENTS -> tbName = CommentsAttachmentsColumns.TABLENAME
            URI_DOCS -> tbName = DocColumns.TABLENAME
            URI_NEWS -> tbName = NewsColumns.TABLENAME
            URI_GROUPS_DET -> tbName = GroupsDetColumns.TABLENAME
            URI_GROUPS_DET_ID -> {
                val groupDetId = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    BaseColumns._ID + " = " + groupDetId
                } else {
                    selection + " AND " + BaseColumns._ID + " = " + groupDetId
                }
                tbName = GroupsDetColumns.TABLENAME
            }

            URI_VIDEO_ALBUMS -> tbName = VideoAlbumsColumns.TABLENAME
            URI_VIDEOS -> tbName = VideoColumns.TABLENAME
            URI_TOPICS -> tbName = TopicsColumns.TABLENAME
            URI_NOTIFICATIONS -> tbName = NotificationColumns.TABLENAME
            URI_USER_DET -> tbName = UsersDetColumns.TABLENAME
            URI_USER_DET_ID -> {
                val userDetId = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    BaseColumns._ID + " = " + userDetId
                } else {
                    selection + " AND " + BaseColumns._ID + " = " + userDetId
                }
                tbName = UsersDetColumns.TABLENAME
            }

            URI_FAVE_PHOTOS -> tbName = FavePhotosColumns.TABLENAME
            URI_FAVE_VIDEOS -> tbName = FaveVideosColumns.TABLENAME
            URI_FAVE_ARTICLES -> tbName = FaveArticlesColumns.TABLENAME
            URI_FAVE_PRODUCTS -> tbName = FaveProductColumns.TABLENAME
            URI_FAVE_PAGES -> tbName = FavePageColumns.TABLENAME
            URI_FAVE_GROUPS -> tbName = FavePageColumns.GROUPSTABLENAME
            URI_FAVE_LINKS -> tbName = FaveLinksColumns.TABLENAME
            URI_FAVE_POSTS -> tbName = FavePostsColumns.TABLENAME
            URI_COUNTRIES -> tbName = CountriesColumns.TABLENAME
            URI_FEED_LISTS -> tbName = FeedListsColumns.TABLENAME
            URI_FRIEND_LISTS -> tbName = FriendListsColumns.TABLENAME
            URI_KEYS -> tbName = KeyColumns.TABLENAME
            else -> throw IllegalArgumentException("Wrong URI: $uri")
        }
        val db: SQLiteDatabase = getDbHelper(uri).writableDatabase
        val cnt: Int = db.delete(tbName, selection, selectionArgs)
        safeNotifyChange(uri)
        return cnt
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        pSelection: String?,
        selectionArgs: Array<String>?
    ): Int {
        var selection = pSelection
        val tbName: String
        when (sUriMatcher.match(uri)) {
            URI_MESSAGES -> tbName = MessageColumns.TABLENAME
            URI_MESSAGES_ID -> {
                val id = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    MessageColumns._ID + " = " + id
                } else {
                    selection + " AND " + MessageColumns._ID + " = " + id
                }
                tbName = MessageColumns.TABLENAME
            }

            URI_USERS -> tbName = UserColumns.TABLENAME
            URI_USERS_ID -> {
                val userID = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    BaseColumns._ID + " = " + userID
                } else {
                    selection + " AND " + BaseColumns._ID + " = " + userID
                }
                tbName = UserColumns.TABLENAME
            }

            URI_GROUPS -> tbName = GroupColumns.TABLENAME
            URI_GROUPS_ID -> {
                val groupID = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    BaseColumns._ID + " = " + groupID
                } else {
                    selection + " AND " + BaseColumns._ID + " = " + groupID
                }
                tbName = GroupColumns.TABLENAME
            }

            URI_DIALOGS -> tbName = DialogsColumns.TABLENAME
            URI_PEERS -> tbName = PeersColumns.TABLENAME
            URI_POSTS -> tbName = PostsColumns.TABLENAME
            URI_POSTS_ID -> {
                val postId = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    BaseColumns._ID + " = " + postId
                } else {
                    selection + " AND " + BaseColumns._ID + " = " + postId
                }
                tbName = PostsColumns.TABLENAME
            }

            URI_PHOTOS -> tbName = PhotosColumns.TABLENAME
            URI_PHOTOS_EXTENDED -> tbName = PhotosExtendedColumns.TABLENAME
            URI_PHOTOS_ID -> {
                val photoId = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    BaseColumns._ID + " = " + photoId
                } else {
                    selection + " AND " + BaseColumns._ID + " = " + photoId
                }
                tbName = PhotosColumns.TABLENAME
            }

            URI_VIDEOS -> tbName = VideoColumns.TABLENAME
            URI_COMMENTS -> tbName = CommentsColumns.TABLENAME
            URI_COMMENTS_ID -> {
                val commentId = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    BaseColumns._ID + " = " + commentId
                } else {
                    selection + " AND " + BaseColumns._ID + " = " + commentId
                }
                tbName = CommentsColumns.TABLENAME
            }

            URI_RELATIVESHIP -> tbName = RelationshipColumns.TABLENAME
            URI_PHOTO_ALBUMS -> tbName = PhotoAlbumsColumns.TABLENAME
            URI_NEWS -> tbName = NewsColumns.TABLENAME
            URI_GROUPS_DET -> tbName = GroupsDetColumns.TABLENAME
            URI_GROUPS_DET_ID -> {
                val groupDetId = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    BaseColumns._ID + " = " + groupDetId
                } else {
                    selection + " AND " + BaseColumns._ID + " = " + groupDetId
                }
                tbName = GroupsDetColumns.TABLENAME
            }

            URI_VIDEO_ALBUMS -> tbName = VideoAlbumsColumns.TABLENAME
            URI_TOPICS -> tbName = TopicsColumns.TABLENAME
            URI_NOTIFICATIONS -> tbName = NotificationColumns.TABLENAME
            URI_USER_DET -> tbName = UsersDetColumns.TABLENAME
            URI_USER_DET_ID -> {
                val userDetId = uri.lastPathSegment
                selection = if (selection.isNullOrEmpty()) {
                    BaseColumns._ID + " = " + userDetId
                } else {
                    selection + " AND " + BaseColumns._ID + " = " + userDetId
                }
                tbName = UsersDetColumns.TABLENAME
            }

            URI_FAVE_PHOTOS -> tbName = FavePhotosColumns.TABLENAME
            URI_FAVE_VIDEOS -> tbName = FaveVideosColumns.TABLENAME
            URI_FAVE_ARTICLES -> tbName = FaveArticlesColumns.TABLENAME
            URI_FAVE_PRODUCTS -> tbName = FaveProductColumns.TABLENAME
            URI_FAVE_PAGES -> tbName = FavePageColumns.TABLENAME
            URI_FAVE_GROUPS -> tbName = FavePageColumns.GROUPSTABLENAME
            URI_FAVE_LINKS -> tbName = FaveLinksColumns.TABLENAME
            URI_FAVE_POSTS -> tbName = FavePostsColumns.TABLENAME
            URI_COUNTRIES -> tbName = CountriesColumns.TABLENAME
            URI_FEED_LISTS -> tbName = FeedListsColumns.TABLENAME
            URI_FRIEND_LISTS -> tbName = FriendListsColumns.TABLENAME
            URI_KEYS -> tbName = KeyColumns.TABLENAME
            else -> throw IllegalArgumentException("Wrong URI: $uri")
        }
        val db: SQLiteDatabase = getDbHelper(uri).writableDatabase
        val cnt: Int =
            db.updateWithOnConflict(tbName, values, selection, selectionArgs, CONFLICT_REPLACE)
        safeNotifyChange(uri)
        if (tbName == MessageColumns.TABLENAME) {
            safeNotifyChange(DIALOGS_CONTENT_URI)
        }
        return cnt
    }
}