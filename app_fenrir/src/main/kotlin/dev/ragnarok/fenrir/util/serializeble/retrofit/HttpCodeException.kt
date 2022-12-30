package dev.ragnarok.fenrir.util.serializeble.retrofit

class HttpCodeException(val code: Int) : RuntimeException(
    getMessage(
        code
    )
) {
    companion object {
        private fun getMessage(code: Int): String {
            return "HTTP $code"
        }
    }
}