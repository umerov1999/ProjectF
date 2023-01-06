package dev.ragnarok.fenrir.api

class NeedValidationException(
    val validationType: String?,
    val validationURL: String?,
    val sid: String?,
    val description: String?,
) : Exception("Need Validation $description")