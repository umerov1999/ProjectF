package dev.ragnarok.fenrir.db.column

import android.provider.BaseColumns

object FaveProductsColumns : BaseColumns {
    const val TABLENAME = "fave_products"
    const val PRODUCT_ID = "product_id"
    const val OWNER_ID = "owner_id"
    const val PRODUCT = "product"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_PRODUCT_ID = "$TABLENAME.$PRODUCT_ID"
    const val FULL_OWNER_ID = "$TABLENAME.$OWNER_ID"
    const val FULL_PRODUCT = "$TABLENAME.$PRODUCT"
}