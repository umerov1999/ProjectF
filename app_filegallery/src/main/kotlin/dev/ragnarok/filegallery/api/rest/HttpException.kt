package dev.ragnarok.filegallery.api.rest

class HttpException(val code: Int) : RuntimeException(
    getMessage(
        code
    )
) {
    companion object {
        internal fun getMessage(code: Int): String {
            return "HTTP $code"
        }
    }
}
