package dev.ragnarok.filegallery.util.serializeble.msgpack.extensions

import kotlinx.serialization.SerializationException
import kotlin.reflect.KClass

open class DynamicMsgPackExtensionSerializer : BaseMsgPackExtensionSerializer<Any?>() {
    companion object Default : DynamicMsgPackExtensionSerializer()
    private data class SerializerPair<T : Any>(
        val klass: KClass<T>,
        val serializer: BaseMsgPackExtensionSerializer<T>
    ) {
        fun deserialize(extension: MsgPackExtension): T {
            return serializer.deserialize(extension)
        }

        @Suppress("UNCHECKED_CAST")
        fun serialize(value: Any?): MsgPackExtension {
            return serializer.serialize(value as T)
        }
    }

    private val serializers: MutableMap<Byte, SerializerPair<*>> = mutableMapOf()

    inline fun <reified T : Any> register(serializer: BaseMsgPackExtensionSerializer<T>) {
        register(serializer, T::class)
    }

    fun <T : Any> register(serializer: BaseMsgPackExtensionSerializer<T>, klass: KClass<T>) {
        serializers[serializer.extTypeId] = SerializerPair(klass, serializer)
    }

    fun unregister(serializer: BaseMsgPackExtensionSerializer<*>) {
        serializers.remove(serializer.extTypeId)
    }

    fun canSerialize(value: Any?): Boolean {
        if (value is MsgPackExtension) return true
        for ((klass, _) in serializers.values) {
            if (klass.isInstance(value)) {
                return true
            }
        }
        return false
    }

    final override fun deserialize(extension: MsgPackExtension): Any {
        val pair = serializers[extension.extTypeId] ?: return extension
        return pair.deserialize(extension)
    }

    final override fun serialize(extension: Any?): MsgPackExtension {
        if (extension is MsgPackExtension) {
            return extension
        }
        for (serializer in serializers.values) {
            if (serializer.klass.isInstance(extension)) {
                return serializer.serialize(extension)
            }
        }
        throw SerializationException("Missing serializer for extension: $extension")
    }

    final override val extTypeId: Byte = 0x00
    final override val checkTypeId: Boolean = false
}
