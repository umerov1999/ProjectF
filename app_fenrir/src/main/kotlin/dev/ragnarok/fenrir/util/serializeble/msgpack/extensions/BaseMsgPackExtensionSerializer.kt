package dev.ragnarok.fenrir.util.serializeble.msgpack.extensions

import dev.ragnarok.fenrir.util.serializeble.msgpack.exceptions.MsgPackSerializationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class BaseMsgPackExtensionSerializer<T> : KSerializer<T> {
    private val serializer = MsgPackExtension.serializer()

    override fun deserialize(decoder: Decoder): T {
        val extension = decoder.decodeSerializableValue(serializer)
        if (checkTypeId && extension.extTypeId != extTypeId) {
            throw MsgPackSerializationException.extensionDeserializationWrongType(
                extension,
                extTypeId,
                extension.extTypeId
            )
        }
        return deserialize(extension)
    }

    final override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        val extension = serialize(value)
        if (checkTypeId && extension.extTypeId != extTypeId) {
            throw MsgPackSerializationException.extensionSerializationWrongType(
                extension,
                extTypeId,
                extension.extTypeId
            )
        }
        encoder.encodeSerializableValue(serializer, serialize(value))
    }

    abstract fun deserialize(extension: MsgPackExtension): T
    abstract fun serialize(extension: T): MsgPackExtension
    abstract val extTypeId: Byte
    internal open val checkTypeId: Boolean = true
}
