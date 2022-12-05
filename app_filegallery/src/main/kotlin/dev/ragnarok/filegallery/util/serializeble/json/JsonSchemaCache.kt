/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.ragnarok.filegallery.util.serializeble.json

import dev.ragnarok.filegallery.util.serializeble.json.internal.DescriptorSchemaCache

@Suppress("DEPRECATION_ERROR")
internal val Json.schemaCache: DescriptorSchemaCache
    get() = this._schemaCache
