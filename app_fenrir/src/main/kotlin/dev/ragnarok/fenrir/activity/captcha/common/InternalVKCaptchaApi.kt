package dev.ragnarok.fenrir.activity.captcha.common

/**
 * This annotation marks APIs that you should not use under any circumstances.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is internal VK Captcha api, do not use it in your code"
)
@Retention(
    AnnotationRetention.BINARY
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPEALIAS
)
annotation class InternalVKCaptchaApi
