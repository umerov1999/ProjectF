package dev.ragnarok.fenrir.api.validation

import dev.ragnarok.fenrir.activity.captcha.VKCaptcha
import dev.ragnarok.fenrir.activity.captcha.VKCaptchaResult
import dev.ragnarok.fenrir.activity.captcha.VKCaptchaResultListener
import dev.ragnarok.fenrir.api.OutOfDateException
import dev.ragnarok.fenrir.nonNullNoEmpty
import java.util.Collections

class VKIdCaptchaProvider() :
    IVKIdCaptchaProvider {
    private val entryMap: MutableMap<String, Entry> = Collections.synchronizedMap(HashMap())
    override fun requestCaptcha(redirectUri: String, domain: String) {
        entryMap[redirectUri] = Entry()
        VKCaptcha.openCaptcha(
            domain,
            redirectUri, object : VKCaptchaResultListener {
                override fun onResult(result: VKCaptchaResult) {
                    if (result.token.nonNullNoEmpty()) {
                        enterState(redirectUri, result.token)
                    } else {
                        entryMap.remove(redirectUri)
                    }
                }

                override fun onUserCancel() {
                    entryMap.remove(redirectUri)
                }
            }
        )
    }

    override fun cancel(redirectUri: String) {
        VKCaptcha.closeCaptcha()
        entryMap.remove(redirectUri)
    }

    @Throws(OutOfDateException::class)
    override fun lookupSuccessToken(redirectUri: String): String? {
        val iterator: MutableIterator<Map.Entry<String, Entry>> = entryMap.entries.iterator()
        while (iterator.hasNext()) {
            val (_, lookupEntry) = iterator.next()
            if (System.currentTimeMillis() - lookupEntry.lastActivityTime > MAX_WAIT_DELAY) {
                iterator.remove()
            }
        }
        val entry = entryMap[redirectUri] ?: throw OutOfDateException()
        return entry.successToken
    }

    override fun enterState(redirectUri: String, successToken: String?) {
        val entry = entryMap[redirectUri]
        if (entry != null) {
            entry.successToken = successToken
        }
    }

    private class Entry {
        var successToken: String? = null
        var lastActivityTime: Long = System.currentTimeMillis()
    }

    companion object {
        private const val MAX_WAIT_DELAY = 15 * 60 * 1000
    }
}
