package dev.ragnarok.fenrir.module.parcel

import android.annotation.SuppressLint
import androidx.annotation.Keep

class ParcelNative {
    private external fun init(flags: Int): Long
    private external fun getFlags(parcel: Long): Int
    private external fun putBoolean(parcel: Long, value: Boolean)
    private external fun putByte(parcel: Long, value: Byte)
    private external fun putInt(parcel: Long, value: Int)
    private external fun putFirstInt(parcel: Long, value: Int)
    private external fun putLong(parcel: Long, value: Long)
    private external fun putFloat(parcel: Long, value: Float)
    private external fun putDouble(parcel: Long, value: Double)
    private external fun putString(parcel: Long, value: String)
    private external fun readBoolean(parcel: Long, listener: UpdatePointerListener): Boolean
    private external fun readByte(parcel: Long, listener: UpdatePointerListener): Byte
    private external fun readInt(parcel: Long, listener: UpdatePointerListener): Int
    private external fun readLong(parcel: Long, listener: UpdatePointerListener): Long
    private external fun readFloat(parcel: Long, listener: UpdatePointerListener): Float
    private external fun readDouble(parcel: Long, listener: UpdatePointerListener): Double
    private external fun readString(parcel: Long, listener: UpdatePointerListener): String

    var nativePointer: Long = 0
        private set
    private val updateListener: UpdatePointerListener =
        object : UpdatePointerListener {
            override fun doUpdateNative(pointer: Long) {
                updateNative(pointer)
            }
        }

    @ParcelFlags
    private var listFlags = 0

    internal fun updateNative(value: Long) {
        nativePointer = value
    }

    private fun initialize_pointer(@ParcelFlags flags: Int) {
        nativePointer = init(flags)
    }

    private fun collectFlags() {
        if (nativePointer <= 0) {
            listFlags = ParcelFlags.MUTABLE_LIST
        }
        listFlags = getFlags(nativePointer)
    }

    fun writeString(value: String?): ParcelNative {
        if (value == null) {
            putBoolean(nativePointer, false)
        } else {
            putBoolean(nativePointer, true)
            putString(nativePointer, value)
        }
        return this
    }

    fun writeBoolean(value: Boolean): ParcelNative {
        putBoolean(nativePointer, value)
        return this
    }

    fun writeByte(value: Byte): ParcelNative {
        putByte(nativePointer, value)
        return this
    }

    fun writeInt(value: Int): ParcelNative {
        putInt(nativePointer, value)
        return this
    }

    fun writeFirstInt(value: Int): ParcelNative {
        putFirstInt(nativePointer, value)
        return this
    }

    fun writeLong(value: Long): ParcelNative {
        putLong(nativePointer, value)
        return this
    }

    fun writeFloat(value: Float): ParcelNative {
        putFloat(nativePointer, value)
        return this
    }

    fun writeDouble(value: Double): ParcelNative {
        putDouble(nativePointer, value)
        return this
    }

    fun writeStringList(data: Collection<String>?): ParcelNative {
        if (data.isNullOrEmpty()) {
            putInt(nativePointer, 0)
            return this
        }
        putInt(nativePointer, data.size)
        for (i in data) {
            putString(nativePointer, i)
        }
        return this
    }

    fun writeIntegerList(data: Collection<Int>?): ParcelNative {
        if (data.isNullOrEmpty()) {
            putInt(nativePointer, 0)
            return this
        }
        putInt(nativePointer, data.size)
        for (i in data) {
            putInt(nativePointer, i)
        }
        return this
    }

    fun writeLongList(data: Collection<Long>?): ParcelNative {
        if (data.isNullOrEmpty()) {
            putInt(nativePointer, 0)
            return this
        }
        putInt(nativePointer, data.size)
        for (i in data) {
            putLong(nativePointer, i)
        }
        return this
    }

    fun writeFloatList(data: Collection<Float>?): ParcelNative {
        if (data.isNullOrEmpty()) {
            putInt(nativePointer, 0)
            return this
        }
        putInt(nativePointer, data.size)
        for (i in data) {
            putFloat(nativePointer, i)
        }
        return this
    }

    fun writeDoubleList(data: Collection<Double>?): ParcelNative {
        if (data.isNullOrEmpty()) {
            putInt(nativePointer, 0)
            return this
        }
        putInt(nativePointer, data.size)
        for (i in data) {
            putDouble(nativePointer, i)
        }
        return this
    }

    @SuppressLint("SwitchIntDef")
    fun readStringList(): List<String>? {
        val size = readInt(nativePointer, updateListener)
        if (size == 0) {
            return when (listFlags) {
                ParcelFlags.NULL_LIST -> null
                ParcelFlags.EMPTY_LIST -> emptyList()
                else -> ArrayList()
            }
        }
        val ret: MutableList<String> = ArrayList(size)
        for (i in 0 until size) {
            ret.add(readString(nativePointer, updateListener))
        }
        return ret
    }

    @SuppressLint("SwitchIntDef")
    fun readIntegerList(): List<Int>? {
        val size = readInt(nativePointer, updateListener)
        if (size == 0) {
            return when (listFlags) {
                ParcelFlags.NULL_LIST -> null
                ParcelFlags.EMPTY_LIST -> emptyList()
                else -> ArrayList()
            }
        }
        val ret: MutableList<Int> = ArrayList(size)
        for (i in 0 until size) {
            ret.add(readInt(nativePointer, updateListener))
        }
        return ret
    }

    @SuppressLint("SwitchIntDef")
    fun readLongList(): List<Long>? {
        val size = readInt(nativePointer, updateListener)
        if (size == 0) {
            return when (listFlags) {
                ParcelFlags.NULL_LIST -> null
                ParcelFlags.EMPTY_LIST -> emptyList()
                else -> ArrayList()
            }
        }
        val ret: MutableList<Long> = ArrayList(size)
        for (i in 0 until size) {
            ret.add(readLong(nativePointer, updateListener))
        }
        return ret
    }

    @SuppressLint("SwitchIntDef")
    fun readFloatList(): List<Float>? {
        val size = readInt(nativePointer, updateListener)
        if (size == 0) {
            return when (listFlags) {
                ParcelFlags.NULL_LIST -> null
                ParcelFlags.EMPTY_LIST -> emptyList()
                else -> ArrayList()
            }
        }
        val ret: MutableList<Float> = ArrayList(size)
        for (i in 0 until size) {
            ret.add(readFloat(nativePointer, updateListener))
        }
        return ret
    }

    @SuppressLint("SwitchIntDef")
    fun readDoubleList(): List<Double>? {
        val size = readInt(nativePointer, updateListener)
        if (size == 0) {
            return when (listFlags) {
                ParcelFlags.NULL_LIST -> null
                ParcelFlags.EMPTY_LIST -> emptyList()
                else -> ArrayList()
            }
        }
        val ret: MutableList<Double> = ArrayList(size)
        for (i in 0 until size) {
            ret.add(readDouble(nativePointer, updateListener))
        }
        return ret
    }

    fun readString(): String? {
        return if (!readBoolean(
                nativePointer,
                updateListener
            )
        ) {
            null
        } else readString(
            nativePointer,
            updateListener
        )
    }

    fun readBoolean(): Boolean {
        return readBoolean(nativePointer, updateListener)
    }

    fun readByte(): Byte {
        return readByte(nativePointer, updateListener)
    }

    fun readInt(): Int {
        return readInt(nativePointer, updateListener)
    }

    fun readLong(): Long {
        return readLong(nativePointer, updateListener)
    }

    fun readFloat(): Float {
        return readFloat(nativePointer, updateListener)
    }

    fun readDouble(): Double {
        return readDouble(nativePointer, updateListener)
    }

    fun <T : ParcelableNative> readParcelable(loader: Creator<T>): T? {
        val isNotNull = readBoolean()
        return if (!isNotNull) {
            null
        } else loader.readFromParcelNative(this)
    }

    fun <T : ParcelableNative> writeParcelable(parcelable: T?) {
        writeBoolean(parcelable != null)
        parcelable?.writeToParcelNative(this)
    }

    @SuppressLint("SwitchIntDef")
    fun <T : ParcelableNative> readParcelableList(loader: Creator<T>): List<T>? {
        val size = readInt(nativePointer, updateListener)
        if (size == 0) {
            return when (listFlags) {
                ParcelFlags.NULL_LIST -> null
                ParcelFlags.EMPTY_LIST -> emptyList()
                else -> ArrayList()
            }
        }
        val ret: MutableList<T> = ArrayList(size)
        for (i in 0 until size) {
            readParcelable(loader)?.let { ret.add(it) }
        }
        return ret
    }

    fun <T : ParcelableNative> readParcelableArrayList(loader: Creator<T>): ArrayList<T>? {
        val size = readInt(nativePointer, updateListener)
        if (size == 0) {
            return if (listFlags == ParcelFlags.NULL_LIST) {
                null
            } else ArrayList()
        }
        val ret = ArrayList<T>(size)
        for (i in 0 until size) {
            readParcelable(loader)?.let { ret.add(it) }
        }
        return ret
    }

    fun <T : ParcelableNative> writeParcelableList(data: Collection<T?>?): ParcelNative {
        if (data.isNullOrEmpty()) {
            putInt(nativePointer, 0)
            return this
        }
        putInt(nativePointer, data.size)
        for (i in data) {
            writeParcelable(i)
        }
        return this
    }

    @Keep
    interface UpdatePointerListener {
        fun doUpdateNative(pointer: Long)
    }

    interface ParcelableNative {
        fun writeToParcelNative(dest: ParcelNative)
    }

    interface Creator<T : ParcelableNative> {
        fun readFromParcelNative(dest: ParcelNative): T?
    }

    companion object {
        fun <T : ParcelableNative> createParcelableList(data: Collection<T?>?): Long {
            val ret = create()
            ret.writeParcelableList(data)
            return ret.nativePointer
        }

        fun <T : ParcelableNative> createParcelableList(
            data: Collection<T?>?,
            @ParcelFlags flags: Int
        ): Long {
            val ret = create(flags)
            ret.writeParcelableList(data)
            return ret.nativePointer
        }

        @SuppressLint("SwitchIntDef")
        fun <T : ParcelableNative> loadParcelableList(
            pointer: Long,
            loader: Creator<T>,
            @ParcelFlags rootFlags: Int
        ): List<T>? {
            val ret = fromNative(pointer)
            val ls = ret.readParcelableList(loader)
            return if (ls.isNullOrEmpty()) {
                when (rootFlags) {
                    ParcelFlags.NULL_LIST -> null
                    ParcelFlags.EMPTY_LIST -> emptyList()
                    else -> ArrayList()
                }
            } else ls
        }

        fun <T : ParcelableNative> loadParcelableArrayList(
            pointer: Long,
            loader: Creator<T>,
            @ParcelFlags rootFlags: Int
        ): ArrayList<T>? {
            val ret = fromNative(pointer)
            val ls = ret.readParcelableArrayList(loader)
            return if (ls.isNullOrEmpty()) {
                if (rootFlags == ParcelFlags.NULL_LIST) {
                    null
                } else ArrayList()
            } else ls
        }

        fun fromNative(value: Long): ParcelNative {
            val ret = ParcelNative()
            ret.updateNative(value)
            ret.collectFlags()
            return ret
        }

        fun create(): ParcelNative {
            val ret = ParcelNative()
            ret.initialize_pointer(ParcelFlags.MUTABLE_LIST)
            ret.listFlags = ParcelFlags.MUTABLE_LIST
            return ret
        }

        fun create(@ParcelFlags flags: Int): ParcelNative {
            val ret = ParcelNative()
            ret.initialize_pointer(flags)
            ret.listFlags = flags
            return ret
        }
    }
}