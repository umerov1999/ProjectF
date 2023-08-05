/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.ragnarok.filegallery.util.serializeble.json.internal

import kotlinx.serialization.InternalSerializationApi

@InternalSerializationApi
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.ANNOTATION_CLASS
)
annotation class FormatLanguage(
    val value: String,
    // default parameters are not used due to https://youtrack.jetbrains.com/issue/KT-25946/
    val prefix: String,
    val suffix: String,
)