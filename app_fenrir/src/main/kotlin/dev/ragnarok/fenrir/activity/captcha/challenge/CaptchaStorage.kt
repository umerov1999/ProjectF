package dev.ragnarok.fenrir.activity.captcha.challenge

import java.util.concurrent.ConcurrentHashMap

internal class CaptchaStorage {

    private val storage = ConcurrentHashMap<String, String>()

    fun addToken(domain: String, token: String) {
        storage[domain] = token
    }

    fun removeToken(domain: String) {
        storage.remove(domain)
    }

    fun getToken(domain: String): String? = storage[domain]
}