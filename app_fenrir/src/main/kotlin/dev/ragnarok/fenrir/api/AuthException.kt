package dev.ragnarok.fenrir.api

class AuthException(val code: String, message: String?) : Exception(message) {
    override val message: String
        get() {
            val desc = super.message
            return if (!desc.isNullOrEmpty()) {
                desc
            } else "Unexpected auth error, code: [$code]"
        }
}