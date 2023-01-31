package dev.ragnarok.fenrir

import androidx.annotation.IntDef

@IntDef(
    AccountType.NULL,
    AccountType.VK_ANDROID,
    AccountType.VK_ANDROID_HIDDEN,
    AccountType.KATE,
    AccountType.KATE_HIDDEN
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class AccountType {
    companion object {
        @AccountType
        fun toAccountType(type: Int): Int {
            return when (type) {
                2 -> VK_ANDROID_HIDDEN
                3 -> KATE
                4 -> KATE_HIDDEN
                else -> VK_ANDROID
            }
        }

        const val NULL = 0
        const val VK_ANDROID = 1
        const val VK_ANDROID_HIDDEN = 2
        const val KATE = 3
        const val KATE_HIDDEN = 4
    }
}