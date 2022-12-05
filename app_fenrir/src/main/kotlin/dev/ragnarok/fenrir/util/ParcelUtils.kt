package dev.ragnarok.fenrir.util

import android.os.Parcel
import dev.ragnarok.fenrir.getBoolean
import dev.ragnarok.fenrir.putBoolean

object ParcelUtils {
    fun writeIntStringMap(dest: Parcel, map: Map<Int, String>?) {
        val isNull = map == null
        dest.putBoolean(isNull)
        if (isNull) {
            return
        }
        val size = (map ?: return).size
        dest.writeInt(size)
        for ((key, value) in map) {
            writeObjectInteger(dest, key)
            dest.writeString(value)
        }
    }

    fun readIntStringMap(p: Parcel): Map<Int, String>? {
        val isNull = p.getBoolean()
        if (isNull) {
            return null
        }
        val size = p.readInt()
        val map: MutableMap<Int, String> = HashMap(size)
        for (i in 0 until size) {
            val key = readObjectInteger(p) ?: continue
            val value = p.readString() ?: continue
            map[key] = value
        }
        return map
    }

    fun writeObjectDouble(dest: Parcel, value: Double?) {
        dest.writeInt(if (value == null) 1 else 0)
        if (value != null) {
            dest.writeDouble(value)
        }
    }

    fun readObjectDouble(p: Parcel): Double? {
        val isNull = p.getBoolean()
        return if (!isNull) {
            p.readDouble()
        } else null
    }

    fun writeObjectInteger(dest: Parcel, value: Int?) {
        dest.writeInt(if (value == null) 1 else 0)
        if (value != null) {
            dest.writeInt(value)
        }
    }

    fun readObjectInteger(p: Parcel): Int? {
        val isNull = p.getBoolean()
        return if (!isNull) {
            p.readInt()
        } else null
    }

    fun writeObjectLong(dest: Parcel, value: Long?) {
        dest.writeInt(if (value == null) 1 else 0)
        if (value != null) {
            dest.writeLong(value)
        }
    }

    fun readObjectLong(p: Parcel): Long? {
        val isNull = p.getBoolean()
        return if (!isNull) {
            p.readLong()
        } else null
    }
}