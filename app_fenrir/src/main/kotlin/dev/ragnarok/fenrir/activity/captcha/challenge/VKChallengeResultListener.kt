package dev.ragnarok.fenrir.activity.captcha.challenge

import dev.ragnarok.fenrir.activity.captcha.VKCaptchaResult

interface VKChallengeResultListener {

    fun onResult(result: VKCaptchaResult)

    fun onUserCancel()
}