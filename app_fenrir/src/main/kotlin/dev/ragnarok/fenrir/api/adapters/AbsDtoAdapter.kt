package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonArray
import dev.ragnarok.fenrir.util.serializeble.json.JsonDecoder
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.JsonNull
import dev.ragnarok.fenrir.util.serializeble.json.JsonObject
import dev.ragnarok.fenrir.util.serializeble.json.JsonPrimitive
import dev.ragnarok.fenrir.util.serializeble.json.booleanOrNull
import dev.ragnarok.fenrir.util.serializeble.json.doubleOrNull
import dev.ragnarok.fenrir.util.serializeble.json.floatOrNull
import dev.ragnarok.fenrir.util.serializeble.json.intOrNull
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonPrimitive
import dev.ragnarok.fenrir.util.serializeble.json.longOrNull
import dev.ragnarok.fenrir.util.serializeble.msgpack.internal.BasicMsgPackDecoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.contracts.contract

abstract class AbsDtoAdapter<T>(name: String) : KSerializer<T> {
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
                returns(true) implies (element is JsonPrimitive && element !is JsonNull)
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
                return checkPrimitive(obj[name])
            }
            return false
        }


        fun hasObject(obj: JsonObject?, name: String): Boolean {
            contract {
                returns(true) implies (obj != null)
            }
            obj ?: return false
            if (obj.containsKey(name)) {
                return checkObject(obj[name])
            }
            return false
        }


        fun hasArray(obj: JsonObject?, name: String): Boolean {
            contract {
                returns(true) implies (obj != null)
            }
            obj ?: return false
            if (obj.containsKey(name)) {
                return checkArray(obj[name])
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
                if (checkPrimitive(element)) element.content else fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        @JvmOverloads
        fun optString(json: JsonObject?, names: List<String>, fallback: String? = null): String? {
            contract {
                returns(true) implies (json != null)
            }
            json ?: return fallback
            for (i in names) {
                try {
                    val element = json[i]
                    if (checkPrimitive(element)) return element.content else continue
                } catch (e: Exception) {
                    if (Constants.IS_DEBUG) {
                        e.printStackTrace()
                    }
                    continue
                }
            }
            return fallback
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
                if (checkPrimitive(element)) element.intOrNull ?: fallback else fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        @JvmOverloads
        fun optFloat(json: JsonObject?, name: String, fallback: Float = 0f): Float {
            contract {
                returns(true) implies (json != null)
            }
            json ?: return fallback
            return try {
                val element = json[name]
                if (checkPrimitive(element)) element.floatOrNull ?: fallback else fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        @JvmOverloads
        fun optDouble(json: JsonObject?, name: String, fallback: Double = 0.0): Double {
            contract {
                returns(true) implies (json != null)
            }
            json ?: return fallback
            return try {
                val element = json[name]
                if (checkPrimitive(element)) element.doubleOrNull ?: fallback else fallback
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
                if (checkPrimitive(element)) element.longOrNull ?: fallback else fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        fun getFirstInt(json: JsonObject?, fallback: Int, vararg names: String): Int {
            contract {
                returns(true) implies (json != null)
            }
            json ?: return fallback
            return try {
                for (name in names) {
                    val element = json[name]
                    if (checkPrimitive(element)) {
                        return element.intOrNull ?: fallback
                    }
                }
                fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        fun getFirstLong(json: JsonObject?, fallback: Long, vararg names: String): Long {
            contract {
                returns(true) implies (json != null)
            }
            json ?: return fallback
            return try {
                for (name in names) {
                    val element = json[name]
                    if (checkPrimitive(element)) {
                        return element.longOrNull ?: fallback
                    }
                }
                fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        @JvmOverloads
        fun optLong(array: JsonArray?, index: Int, fallback: Long = 0L): Long {
            contract {
                returns(true) implies (array != null)
            }
            array ?: return fallback
            return try {
                val opt = opt(array, index)
                if (checkPrimitive(opt)) opt.longOrNull ?: fallback else fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }


        @JvmOverloads
        fun optInt(array: JsonArray?, index: Int, fallback: Int = 0): Int {
            contract {
                returns(true) implies (array != null)
            }
            array ?: return fallback
            return try {
                val opt = opt(array, index)
                if (checkPrimitive(opt)) opt.intOrNull ?: fallback else fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }


        fun opt(array: JsonArray?, index: Int): JsonElement? {
            contract {
                returns(true) implies (array != null)
            }
            array ?: return null
            return if (index < 0 || index >= array.size) {
                null
            } else array[index]
        }


        @JvmOverloads
        fun optString(array: JsonArray?, index: Int, fallback: String? = null): String? {
            contract {
                returns(true) implies (array != null)
            }
            array ?: return fallback
            return try {
                val opt = opt(array, index)
                if (checkPrimitive(opt)) opt.content else fallback
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        inline fun <reified T> parseArray(
            array: JsonElement?,
            fallback: List<T>?, serializer: KSerializer<T>
        ): List<T>? {
            contract {
                returns(true) implies (array != null)
            }
            array ?: return fallback
            return if (!checkArray(array)) {
                fallback
            } else try {
                val list: MutableList<T> = ArrayList()
                for (i in 0 until array.jsonArray.size) {
                    list.add(kJson.decodeFromJsonElement(serializer, array[i]))
                }
                list
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        inline fun <reified T> parseArray(
            array: JsonArray?,
            fallback: List<T>?, serializer: KSerializer<T>
        ): List<T>? {
            contract {
                returns(true) implies (array != null)
            }
            array ?: return fallback
            return try {
                val list: MutableList<T> = ArrayList()
                for (i in 0 until array.size) {
                    list.add(kJson.decodeFromJsonElement(serializer, array[i]))
                }
                list
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        fun optStringArray(
            root: JsonObject?,
            name: String,
            fallback: Array<String>?
        ): Array<String>? {
            contract {
                returns(true) implies (root != null)
            }
            root ?: return fallback
            return try {
                val element = root[name]
                if (!checkArray(element)) {
                    fallback
                } else parseStringArray(element.jsonArray)
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        fun optIntArray(root: JsonObject?, name: String, fallback: IntArray?): IntArray? {
            contract {
                returns(true) implies (root != null)
            }
            root ?: return fallback
            return try {
                val element = root[name]
                if (!checkArray(element)) {
                    fallback
                } else parseIntArray(element.jsonArray)
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        fun optIntArray(array: JsonArray?, index: Int, fallback: IntArray?): IntArray? {
            contract {
                returns(true) implies (array != null)
            }
            array ?: return fallback
            return try {
                if (index < 0 || index >= array.size) {
                    return fallback
                }
                val array_r = array[index]
                if (!checkArray(array_r)) {
                    fallback
                } else parseIntArray(array_r.jsonArray)
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        fun optLongArray(root: JsonObject?, name: String, fallback: LongArray?): LongArray? {
            contract {
                returns(true) implies (root != null)
            }
            root ?: return fallback
            return try {
                val element = root[name]
                if (!checkArray(element)) {
                    fallback
                } else parseLongArray(element.jsonArray)
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        fun optLongArray(array: JsonArray?, index: Int, fallback: LongArray?): LongArray? {
            contract {
                returns(true) implies (array != null)
            }
            array ?: return fallback
            return try {
                if (index < 0 || index >= array.size) {
                    return fallback
                }
                val array_r = array[index]
                if (!checkArray(array_r)) {
                    fallback
                } else parseLongArray(array_r.jsonArray)
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
                fallback
            }
        }

        private fun parseLongArray(array: JsonArray?): LongArray {
            contract {
                returns(true) implies (array != null)
            }
            array ?: return LongArray(0)
            val list = LongArray(array.size)
            for (i in 0 until array.size) {
                list[i] = array[i].asPrimitiveSafe?.longOrNull ?: 0
            }
            return list
        }

        private fun parseIntArray(array: JsonArray?): IntArray {
            contract {
                returns(true) implies (array != null)
            }
            array ?: return IntArray(0)
            val list = IntArray(array.size)
            for (i in 0 until array.size) {
                list[i] = array[i].asPrimitiveSafe?.intOrNull ?: 0
            }
            return list
        }

        private fun parseStringArray(array: JsonArray?): Array<String> {
            contract {
                returns(true) implies (array != null)
            }
            array ?: return emptyArray()
            return Array(array.size) { optString(array, it) ?: "null" }
        }

        val JsonElement.asJsonObjectSafe: JsonObject?
            get() = if (checkObject(this)) this else null

        val JsonElement.asPrimitiveSafe: JsonPrimitive?
            get() = if (checkPrimitive(this)) this else null

        val JsonElement.asJsonArraySafe: JsonArray?
            get() = if (checkArray(this)) this else null

        fun JsonObject?.has(name: String): Boolean {
            return this?.containsKey(name) ?: false
        }
    }
}
