package dev.ragnarok.fenrir.db.column

import android.content.ContentValues
import android.provider.BaseColumns
import dev.ragnarok.fenrir.api.model.VKApiCountry

object CountriesColumns : BaseColumns {
    const val TABLENAME = "countries"
    const val NAME = "name"
    const val FULL_ID = TABLENAME + "." + BaseColumns._ID
    const val FULL_NAME = "$TABLENAME.$NAME"
    fun getCV(country: VKApiCountry): ContentValues {
        val cv = ContentValues()
        cv.put(BaseColumns._ID, country.id)
        cv.put(NAME, country.title)
        return cv
    }
}