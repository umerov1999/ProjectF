package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(
    CryptStatus.NO_ENCRYPTION,
    CryptStatus.ENCRYPTED,
    CryptStatus.DECRYPTED,
    CryptStatus.DECRYPT_FAILED
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class CryptStatus {
    companion object {
        const val NO_ENCRYPTION = 0
        const val ENCRYPTED = 1
        const val DECRYPTED = 2
        const val DECRYPT_FAILED = 3
    }
}