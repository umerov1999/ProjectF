package dev.ragnarok.fenrir.crypt.ver

import dev.ragnarok.fenrir.settings.Settings

object Version {
    // aes-128, rsa-512
    private const val V1 = 1

    //// aes-256, rsa-2048
    private const val V2 = 2
    private val ATTRS1: Attrs = object : Attrs {
        override val rsaKeySize: Int
            get() = 512
        override val aesKeySize: Int
            get() = 128
    }
    private val ATTRS2: Attrs = object : Attrs {
        override val rsaKeySize: Int
            get() = 2048
        override val aesKeySize: Int
            get() = 256
    }


    val currentVersion: Int
        get() = Settings.get().main().cryptVersion()


    fun ofCurrent(): Attrs {
        return of(currentVersion)
    }

    fun of(v: Int): Attrs {
        return when (v) {
            V1 -> ATTRS1
            V2 -> ATTRS2
            else -> throw IllegalArgumentException("Unsupported crytp version")
        }
    }

    interface Attrs {
        val rsaKeySize: Int
        val aesKeySize: Int
    }
}