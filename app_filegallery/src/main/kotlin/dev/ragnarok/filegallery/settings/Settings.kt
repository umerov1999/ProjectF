package dev.ragnarok.filegallery.settings

import dev.ragnarok.filegallery.Includes

object Settings {
    fun get(): ISettings {
        return Includes.settings
    }
}