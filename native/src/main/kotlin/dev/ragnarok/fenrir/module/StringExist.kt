package dev.ragnarok.fenrir.module

class StringExist(useMutex: Boolean) {
    private external fun init(useMutex: Boolean): Long
    private external fun destroy(pointer: Long)
    private external fun clear(pointer: Long)
    private external fun insert(pointer: Long, value: String)
    private external fun delete(pointer: Long, value: String)
    private external fun has(pointer: Long, value: String, contains: Boolean): Boolean
    private external fun lockMutex(pointer: Long, lock: Boolean)

    private var nativePointer: Long = 0

    fun lockMutex(lock: Boolean) {
        if (nativePointer != 0L) {
            lockMutex(nativePointer, lock)
        }
    }

    fun getNativePointer(): Long {
        return nativePointer
    }

    fun clear() {
        if (nativePointer != 0L) {
            clear(nativePointer)
        }
    }

    fun insert(value: String) {
        if (value.isEmpty()) {
            return
        }
        if (nativePointer != 0L) {
            insert(nativePointer, value)
        }
    }

    fun delete(value: String) {
        if (value.isEmpty()) {
            return
        }
        if (nativePointer != 0L) {
            delete(nativePointer, value)
        }
    }

    fun has(value: String): Boolean {
        if (value.isEmpty()) {
            return false
        }
        return if (nativePointer != 0L) {
            has(nativePointer, value, false)
        } else false
    }

    operator fun contains(value: String): Boolean {
        if (value.isEmpty()) {
            return false
        }
        return if (nativePointer != 0L) {
            has(nativePointer, value, true)
        } else false
    }

    protected fun finalize() {
        if (nativePointer != 0L) {
            destroy(nativePointer)
            nativePointer = 0
        }
    }

    init {
        nativePointer = if (FenrirNative.isNativeLoaded) {
            init(useMutex)
        } else {
            0
        }
    }
}