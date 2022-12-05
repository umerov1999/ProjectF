package dev.ragnarok.fenrir.util.serializeble.msgpack.extensions

import dev.ragnarok.fenrir.util.serializeble.msgpack.types.MsgPackType
import kotlinx.serialization.Serializable

@Serializable
class MsgPackExtension(
    val type: Byte,
    val extTypeId: Byte,
    val data: ByteArray
) {
    object Type {
        const val FIXEXT1 = MsgPackType.Ext.FIXEXT1
        const val FIXEXT2 = MsgPackType.Ext.FIXEXT2
        const val FIXEXT4 = MsgPackType.Ext.FIXEXT4
        const val FIXEXT8 = MsgPackType.Ext.FIXEXT8
        const val FIXEXT16 = MsgPackType.Ext.FIXEXT16
        const val EXT8 = MsgPackType.Ext.EXT8
        const val EXT16 = MsgPackType.Ext.EXT16
        const val EXT32 = MsgPackType.Ext.EXT32
    }
}
