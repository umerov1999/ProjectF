package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FaveProductsColumns : BaseColumns {
    const val TABLENAME = "fave_products"
    const val PRODUCT = "product"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_PRODUCT = "$TABLENAME.$PRODUCT"
}