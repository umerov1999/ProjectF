package dev.ragnarok.fenrir.api.rest

class HttpException(val code: Int) : RuntimeException(
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
