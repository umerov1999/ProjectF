package dev.ragnarok.fenrir.model

import androidx.annotation.StringDef

@StringDef(
    SwitchableCategory.FRIENDS,
    SwitchableCategory.DIALOGS,
    SwitchableCategory.FEED,
    SwitchableCategory.FEEDBACK,
    SwitchableCategory.GROUPS,
    SwitchableCategory.PHOTOS,
    SwitchableCategory.VIDEOS,
    SwitchableCategory.MUSIC,
    SwitchableCategory.DOCS,
    SwitchableCategory.FAVES,
    SwitchableCategory.SEARCH,
    SwitchableCategory.NEWSFEED_COMMENTS
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class SwitchableCategory {
    companion object {
        const val FRIENDS = "friends"
        const val DIALOGS = "dialogs"
        const val FEED = "feed"
        const val FEEDBACK = "feedback"
        const val GROUPS = "groups"
        const val PHOTOS = "photos"
        const val VIDEOS = "videos"
        const val MUSIC = "music"
        const val DOCS = "docs"
        const val FAVES = "faves"
        const val SEARCH = "search"
        const val NEWSFEED_COMMENTS = "newsfeed_comments"
    }
}