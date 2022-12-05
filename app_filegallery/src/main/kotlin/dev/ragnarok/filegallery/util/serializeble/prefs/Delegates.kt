// SPDX-FileCopyrightText: 2020-2021 Eduard Wolf
//
// SPDX-License-Identifier: Apache-2.0

package dev.ragnarok.filegallery.util.serializeble.prefs

import android.content.SharedPreferences
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.serializer
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/* Knit setup
<!--- INCLUDE .*-property-.*
import android.content.*
import androidx.test.filters.SmallTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.*
import kotlinx.serialization.builtins.*
import dev.ragnarok.filegallery.util.serializeble.prefs.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PropertyTest {

    val sharedPreferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("test_preferences", Context.MODE_PRIVATE)
    val preferences = Preferences(sharedPreferences)

    @AfterTest
    fun tearDown() {
        sharedPreferences.edit().clear().apply()
    }

----- SUFFIX .*-property-without-default-.*
    @Test
    fun test() {
        setting = false
        assertFalse(setting)
        setting = true
        assertTrue(setting)
    }
}
----- SUFFIX .*-property-with-default-.*
    @Test
    fun test() {
        assertFalse(setting)
        setting = true
        assertTrue(setting)
    }
}
-->
*/
/**
 * Encodes changes to the delegated property into the [SharedPreferences] and decodes the current value from them.
 * ```kotlin
 * var setting by preferences.asProperty(Boolean.serializer())
 * ```
 *
 * @param serializer which encodes and decodes the value
 * @param tag optional tag which is used as SharedPreferences key - default to property name
 */
// <!--- KNIT example-property-without-default-01.kt -->
fun <T> Preferences.asProperty(
    serializer: KSerializer<T>,
    tag: String? = null,
): ReadWriteProperty<Any?, T> = PreferenceProperty(this, serializer, tag)

/**
 * Encodes changes to the delegated property into the [SharedPreferences] and decodes the current value from them.
 * ```kotlin
 * var setting by preferences.asProperty(Boolean.serializer(), tag = "aSetting", default = false)
 * ```
 *
 * @param serializer which encodes and decodes the value
 * @param tag optional tag which is used as SharedPreferences key - default to property name
 * @param default optional default value for not initialized preferences
 */
// <!--- KNIT example-property-with-default-01.kt -->
fun <T : Any> Preferences.asProperty(
    serializer: KSerializer<T>,
    tag: String? = null,
    default: T? = null,
): ReadWriteProperty<Any?, T> = if (default == null) {
    PreferenceProperty(this, serializer, tag)
} else {
    PreferencePropertyWithDefault(this, serializer.nullable, tag, default)
}

/**
 * Encodes changes to the delegated property into the [SharedPreferences] and decodes the current value from them.
 * ```kotlin
 * var setting: Boolean by preferences.asProperty()
 * ```
 *
 * @param tag optional tag which is used as SharedPreferences key - default to property name
 */
// <!--- KNIT example-property-without-default-02.kt -->
@Suppress("RemoveExplicitTypeArguments")
inline fun <reified T> Preferences.asProperty(tag: String? = null): ReadWriteProperty<Any?, T> =
    asProperty<T>(serializersModule.serializer(), tag)

/**
 * Encodes changes to the delegated property into the [SharedPreferences] and decodes the current value from them.
 * ```kotlin
 * var setting: Boolean by preferences.asProperty(tag = "aSetting", default = false)
 * ```
 *
 * @param tag optional tag which is used as SharedPreferences key - default to property name
 * @param default optional default value for not initialized preferences
 */
// <!--- KNIT example-property-with-default-02.kt -->
inline fun <reified T : Any> Preferences.asProperty(
    tag: String? = null,
    default: T? = null,
): ReadWriteProperty<Any?, T> = asProperty(serializersModule.serializer(), tag, default)

private class PreferenceProperty<T>(
    private val preferences: Preferences,
    private val serializer: KSerializer<T>,
    private val tag: String?,
) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val tag = this.tag ?: property.name
        return preferences.decode(serializer, tag)
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val tag = this.tag ?: property.name
        preferences.encode(serializer, tag, value)
    }
}

private class PreferencePropertyWithDefault<T : Any>(
    private val preferences: Preferences,
    private val serializer: KSerializer<T?>,
    private val tag: String?,
    private val default: T,
) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val tag = this.tag ?: property.name
        return preferences.decode(serializer, tag) ?: default
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val tag = this.tag ?: property.name
        preferences.encode(serializer, tag, value)
    }
}
