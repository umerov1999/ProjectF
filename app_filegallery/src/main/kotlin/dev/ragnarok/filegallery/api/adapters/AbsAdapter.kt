package dev.ragnarok.filegallery.api.adapters

import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.util.serializeble.json.*
import dev.ragnarok.filegallery.util.serializeble.msgpack.internal.BasicMsgPackDecoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.contracts.contract

abstract class AbsAdapter<T>(name: String) : KSerializer<T> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name)

    override fun deserialize(decoder: Decoder): T {
        require(decoder is JsonDecoder || decoder is BasicMsgPackDecoder)
        return deserialize(if (decoder is JsonDecoder) decoder.decodeJsonElement() else (decoder as BasicMsgPackDecoder).decodeMsgPackElement())
    }

    abstract fun deserialize(json: JsonElement): T

    override fun serialize(encoder: Encoder, value: T) {
        throw UnsupportedOperationException()
    }

    companion object {
        fun checkObject(element: JsonElement?): Boolean {
            contract {
                returns(true) implies (element is JsonObject)
            }
            return element is JsonObject
        }

        fun checkPrimitive(element: JsonElement?): Boolean {
            contract {
                returns(true) implies (element is JsonPrimitive)
            }
            return element is JsonPrimitive
        }

        fun checkArray(element: JsonElement?): Boolean {
            contract {
                returns(true) implies (element is JsonArray)
            }
            return element is JsonArray && element.size > 0
        }


        fun hasPrimitive(obj: JsonObject?, name: String): Boolean {
            contract {
                returns(true) implies (obj != null)
            }
            obj ?: return false
            if (obj.containsKey(name)) {
                return obj[name] is JsonPrimitive
            }
            return false
        }


        fun hasObject(obj: JsonObject?, name: String): Boolean {
            contract {
                returns(true) implies (obj != null)
            }
            obj ?: return false
            if (obj.containsKey(name)) {
                return obj[name] is JsonObject
            }
            return false
        }


        fun hasArray(obj: JsonObject?, name: String): Boolean {
            contract {
                returns(true) implies (obj != null)
            }
            obj ?: return false
            if (obj.containsKey(name)) {
                val element = obj[name]
                return element is JsonArray && element.jsonArray.size > 0
            }
            return false
        }


        @JvmOverloads
        fun optString(json: JsonObject?, name: String, fallback: String? = null): String? {
            contract {
                returns(true) implies (json != null)
            }
            json ?: return fallback
            return try {
                val element = json[name]
                if (element is JsonPrimitive) element.content else fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }


        fun optBoolean(json: JsonObject?, name: String): Boolean {
            contract {
                returns(true) implies (json != null)
            }
            json ?: return false
            return try {
                val element = json[name]
                if (!checkPrimitive(element)) {
                    return false
                }
                val prim = element.jsonPrimitive
                prim.booleanOrNull ?: prim.intOrNull?.equals(1) ?: false
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                false
            }
        }

        @JvmOverloads
        fun optInt(json: JsonObject?, name: String, fallback: Int = 0): Int {
            contract {
                returns(true) implies (json != null)
            }
            json ?: return fallback
            return try {
                val element = json[name]
                (element as? JsonPrimitive)?.intOrNull ?: fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        @JvmOverloads
        fun optLong(json: JsonObject?, name: String, fallback: Long = 0L): Long {
            contract {
                returns(true) implies (json != null)
            }
            json ?: return fallback
            return try {
                val element = json[name]
                (element as? JsonPrimitive)?.longOrNull ?: fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        fun JsonObject?.getAsJsonArray(name: String): JsonArray? {
            return this?.get(name)?.jsonArray
        }

        fun JsonObject?.getAsJsonObject(name: String): JsonObject? {
            return this?.get(name)?.jsonObject
        }

        val JsonElement.asJsonObject: JsonObject
            get() = this as? JsonObject ?: error("JsonObject")

        val JsonElement.asJsonObjectSafe: JsonObject?
            get() = this as? JsonObject

        val JsonElement.asPrimitiveSafe: JsonPrimitive?
            get() = this as? JsonPrimitive

        val JsonElement.asJsonArraySafe: JsonArray?
            get() = this as? JsonArray

        val JsonElement.asJsonArray: JsonArray
            get() = this as? JsonArray ?: error("JsonArray")

        fun JsonObject?.has(name: String): Boolean {
            return this?.containsKey(name) ?: false
        }
    }
}