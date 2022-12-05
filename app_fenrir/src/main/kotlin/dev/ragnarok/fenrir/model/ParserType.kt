package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(ParserType.JSON, ParserType.MSGPACK)
@Retention(AnnotationRetention.SOURCE)
annotation class ParserType {
    companion object {
        const val JSON = 0
        const val MSGPACK = 1
    }
}
