package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FaveArticlesColumns : BaseColumns {
    const val TABLENAME = "fave_articles"
    const val ARTICLE_ID = "article_id"
    const val OWNER_ID = "owner_id"
    const val ARTICLE = "article"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_ARTICLE_ID = "$TABLENAME.$ARTICLE_ID"
    const val FULL_OWNER_ID = "$TABLENAME.$OWNER_ID"
    const val FULL_ARTICLE = "$TABLENAME.$ARTICLE"
}