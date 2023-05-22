package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FaveArticlesColumns : BaseColumns {
    const val TABLENAME = "fave_articles"
    const val ARTICLE = "article"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_ARTICLE = "$TABLENAME.$ARTICLE"
}