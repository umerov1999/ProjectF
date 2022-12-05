package dev.ragnarok.fenrir.model

import android.os.Parcelable

interface AbsModel : Parcelable {
    @AbsModelType
    fun getModelType(): Int
}