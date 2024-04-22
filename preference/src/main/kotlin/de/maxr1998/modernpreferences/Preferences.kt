/*
 * Copyright (C) 2018 Max Rumpf alias Maxr1998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.maxr1998.modernpreferences

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.core.view.isVisible
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import de.maxr1998.modernpreferences.helpers.DISABLED_RESOURCE_ID
import de.maxr1998.modernpreferences.helpers.DependencyManager
import de.maxr1998.modernpreferences.helpers.KEY_ROOT_SCREEN
import de.maxr1998.modernpreferences.helpers.PreferenceMarker
import de.maxr1998.modernpreferences.preferences.AccentButtonPreference
import de.maxr1998.modernpreferences.preferences.CheckBoxPreference
import de.maxr1998.modernpreferences.preferences.CollapsePreference
import de.maxr1998.modernpreferences.preferences.ColorPickPreference
import de.maxr1998.modernpreferences.preferences.CustomTextPreference
import de.maxr1998.modernpreferences.preferences.EditTextPreference
import de.maxr1998.modernpreferences.preferences.ExpandableTextPreference
import de.maxr1998.modernpreferences.preferences.ImagePreference
import de.maxr1998.modernpreferences.preferences.MultiLineEditTextPreference
import de.maxr1998.modernpreferences.preferences.SeekBarPreference
import de.maxr1998.modernpreferences.preferences.SeparatorSpaceTextPreference
import de.maxr1998.modernpreferences.preferences.SwitchPreference
import de.maxr1998.modernpreferences.preferences.choice.MultiChoiceDialogPreference
import de.maxr1998.modernpreferences.preferences.choice.SingleChoiceDialogPreference
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("UnnecessaryAbstractClass")
abstract class AbstractPreference internal constructor(val key: String) {
    // UI
    @StringRes
    var titleRes: Int = DISABLED_RESOURCE_ID
    var title: CharSequence = ""

    @StringRes
    var summaryRes: Int = DISABLED_RESOURCE_ID
    var summary: CharSequence? = null

    @StringRes
    var summaryDisabledRes: Int = DISABLED_RESOURCE_ID
    var summaryDisabled: CharSequence? = null

    @DrawableRes
    var iconRes: Int = DISABLED_RESOURCE_ID
    var icon: Drawable? = null

    fun getTitle(context: Context): String {
        if (title.isNotEmpty()) {
            return title.toString()
        } else if (titleRes != DISABLED_RESOURCE_ID) {
            return context.getString(titleRes)
        }
        return key
    }

    fun icon(@DrawableRes res: Int) {
        iconRes = res
        icon = null
    }

    fun icon(drawable: Drawable?) {
        icon = drawable
        iconRes = DISABLED_RESOURCE_ID
    }

    @StringRes
    var badgeRes: Int = DISABLED_RESOURCE_ID
    var badge: CharSequence? = null

    // State
    var visible = true
    var collapsed = false

    internal fun copyAbstract(other: AbstractPreference) {
        title = other.title
        titleRes = other.titleRes
        summary = other.summary
        summaryRes = other.summaryRes
        summaryDisabled = other.summaryDisabled
        summaryDisabledRes = other.summaryDisabledRes
        icon = other.icon
        iconRes = other.iconRes
        badge = other.badge
        badgeRes = other.badgeRes

        visible = other.visible
        collapsed = other.collapsed
    }

    override fun equals(other: Any?): Boolean {
        return other is AbstractPreference && key == other.key
    }

    override fun hashCode() = key.hashCode()
}

/**
 * The base class for the Preference system - it corresponds to a single item in the list, regardless
 * if [switch][de.maxr1998.modernpreferences.preferences.SwitchPreference],
 * [category header][de.maxr1998.modernpreferences.preferences.CategoryHeader] or
 * [sub-screen][PreferenceScreen].
 *
 * Further (global) configuration options can be found in [Preference.Config].
 */
@PreferenceMarker
open class Preference(key: String) : AbstractPreference(key) {
    fun makeCopyForFind(): Preference {
        val pref: Preference
        when (this) {
            is AccentButtonPreference -> {
                pref = AccentButtonPreference(key)
            }

            is CheckBoxPreference -> {
                pref = CheckBoxPreference(key).copyTwoState(this)
            }

            is ColorPickPreference -> {
                pref = ColorPickPreference(key, fragmentManager).copyColorPick(this)
            }

            is EditTextPreference -> {
                pref = EditTextPreference(key, fragmentManager).copyEdit(this)
            }

            is SeparatorSpaceTextPreference -> {
                pref = SeparatorSpaceTextPreference(key, fragmentManager).copySeparator(this)
            }

            is MultiLineEditTextPreference -> {
                pref = MultiLineEditTextPreference(key, fragmentManager).copyMultiLine(this)
            }

            is CustomTextPreference -> {
                pref = CustomTextPreference(key, fragmentManager).copyCustomText(this)
            }

            is ExpandableTextPreference -> {
                pref = ExpandableTextPreference(key).copyExpandable(this)
            }

            is ImagePreference -> {
                pref = ImagePreference(key).copyImage(this)
            }

            is SeekBarPreference -> {
                pref = SeekBarPreference(key).copySeek(this)
            }

            is SwitchPreference -> {
                pref = SwitchPreference(key).copyTwoState(this)
            }

            is MultiChoiceDialogPreference -> {
                pref =
                    MultiChoiceDialogPreference(key, cloneItems(), fragmentManager).copyMultiChoice(
                        this
                    )
            }

            is SingleChoiceDialogPreference -> {
                pref = SingleChoiceDialogPreference(
                    key,
                    cloneItems(),
                    fragmentManager
                ).copySingleChoice(this)
            }

            else -> {
                pref = Preference(key)
            }
        }
        pref.copyPreference(this)
        pref.collapsed = false
        return pref
    }

    private fun copyPreference(other: Preference) {
        copyAbstract(other)
        enabled = other.enabled
        dependency = other.dependency
        preBindListener = other.preBindListener
        clickListener = other.clickListener
        longClickListener = other.longClickListener
        prefs = other.prefs
        persistent = other.persistent
        includeInCollapseSummary = other.includeInCollapseSummary
    }

    // State
    var enabled = true
        set(value) {
            if (field != value) {
                field = value
                requestRebind()
            }
        }

    var dependency: String? = null

    var preBindListener: OnPreBindListener? = null

    var clickListener: OnClickListener? = null

    var longClickListener: OnLongClickListener? = null

    /**
     * The screen this Preference currently is attached to, or null
     */
    var parent: PreferenceScreen? = null
        private set

    var inSearchParentStory: String? = null
        private set

    var screenPosition: Int = 0
        private set

    private var prefs: SharedPreferences? = null

    /**
     * Whether or not to persist changes to this preference to the attached [SharedPreferences] instance
     */
    var persistent: Boolean = true

    private var highlightOnNextBind = AtomicBoolean(false)

    var includeInCollapseSummary = true

    @LayoutRes
    open fun getWidgetLayoutResource() = DISABLED_RESOURCE_ID

    internal fun attachToScreen(screen: PreferenceScreen, position: Int) {
        check(parent == null) { "Preference was already attached to a screen!" }
        parent = screen
        screenPosition = position
        prefs = if (persistent) screen.prefs else null
        DependencyManager.register(this)
        onAttach()
    }

    internal open fun onAttach() {}

    /**
     * Check if this preference has a parent of the given [key]
     *
     * That is, somewhere in the hierarchy above this preference or screen
     * there's a parent screen with that key.
     */
    fun hasParent(key: String): Boolean {
        return generateSequence(parent, PreferenceScreen::parent).any { it.key == key }
    }

    protected open fun resolveSummary(context: Context): CharSequence? = when {
        !enabled && summaryDisabledRes != DISABLED_RESOURCE_ID -> context.resources.getText(
            summaryDisabledRes
        )

        !enabled && summaryDisabled != null -> summaryDisabled
        summaryRes != DISABLED_RESOURCE_ID -> context.resources.getText(summaryRes)
        summary != null -> summary
        else -> null
    }

    /**
     * Binds the preference-data to its views from the [view holder][PreferencesAdapter.ViewHolder]
     * Don't call this yourself, it will get called from the [PreferencesAdapter].
     */
    @CallSuper
    @Suppress("ComplexMethod")
    open fun bindViews(holder: PreferencesAdapter.ViewHolder) {
        val preferenceParent = checkNotNull(parent) {
            "Trying to bind view for a preference not attached to a screen!"
        }

        preBindListener?.onPreBind(this, holder)

        holder.itemView.layoutParams.height =
            if (visible && !collapsed) ViewGroup.LayoutParams.WRAP_CONTENT else 0
        if (!visible || collapsed) {
            holder.itemView.isVisible = false
            return
        }
        if (enabled != holder.itemView.isEnabled) { // Only set if different from ViewHolder
            holder.setEnabledState(enabled)
        }

        var itemVisible = false
        holder.icon?.apply {
            itemVisible = true
            when {
                iconRes != DISABLED_RESOURCE_ID -> setImageResource(iconRes)
                icon != null -> setImageDrawable(icon)
                else -> {
                    setImageDrawable(null)
                    itemVisible = false
                }
            }
        }
        holder.iconFrame.apply {
            isVisible = itemVisible || !preferenceParent.collapseIcon
            if (isVisible && this is LinearLayout) {
                gravity =
                    if (preferenceParent.centerIcon) Gravity.CENTER else Gravity.START or Gravity.CENTER_VERTICAL
            }
        }
        holder.title.apply {
            if (titleRes != DISABLED_RESOURCE_ID) setText(titleRes) else text = title
            maxLines = Config.titleMaxLines
        }
        holder.summary?.apply {
            val summary = resolveSummary(context)
            text = summary
            maxLines = Config.summaryMaxLines
            isVisible = summary != null
        }
        holder.badge?.apply {
            itemVisible = true
            when {
                badgeRes != DISABLED_RESOURCE_ID -> setText(badgeRes)
                badge != null -> text = badge
                else -> {
                    text = null
                    itemVisible = false
                }
            }
            isVisible = itemVisible
        }
        holder.widgetFrame?.apply {
            isVisible = childCount > 0 && this@Preference !is SeekBarPreference
        }
        holder.itemView.isVisible = true
        if (highlightOnNextBind.getAndSet(false)) {
            val v = holder.itemView
            val highlightRunnable = Runnable {
                v.background.setHotspot(v.width / 2f, v.height / 2f)
                v.isPressed = true
                v.isPressed = false
            }
            @Suppress("MagicNumber")
            v.postDelayed(highlightRunnable, 300)
            @Suppress("MagicNumber")
            v.postDelayed(highlightRunnable, 600)
        }
    }

    fun requestRebind() {
        parent?.requestRebind(screenPosition)
    }

    fun requestRebindAndHighlight() {
        highlightOnNextBind.set(true)
        requestRebind()
    }

    internal fun performClick(holder: PreferencesAdapter.ViewHolder) {
        onClick(holder)
        if (clickListener?.onClick(this, holder) == true) bindViews(holder)
    }

    internal fun performLongClick(holder: PreferencesAdapter.ViewHolder): Boolean {
        val ret = onLongClick(holder)
        if (longClickListener?.onLongClick(this, holder) == true) {
            bindViews(holder)
            return true
        }
        return ret
    }

    fun makeInSearchParentStoryFrom(context: Context, pref: Preference) {
        val list = ArrayList<String>()
        var prScreen = pref.parent
        while (prScreen != null && prScreen.key != "root" && prScreen.key != "found_result") {
            list.add(prScreen.getTitle(context))
            prScreen = prScreen.parent
        }
        list.reverse()
        val res = StringBuilder()
        for (i in list) {
            res.append(i).append(" -> ")
        }
        res.append(pref.getTitle(context))
        inSearchParentStory = res.toString()
    }

    open fun onClick(holder: PreferencesAdapter.ViewHolder) {}

    open fun onLongClick(holder: PreferencesAdapter.ViewHolder): Boolean {
        if (inSearchParentStory != null) {
            Snackbar.make(holder.itemView, inSearchParentStory.orEmpty(), LENGTH_LONG).show()
            return true
        }
        return false
    }

    /**
     * Save an int for this [Preference]s' [key] to the [SharedPreferences] of the attached [PreferenceScreen]
     */
    fun commitInt(value: Int) {
        prefs?.edit {
            putInt(key, value)
        }
    }

    fun getInt(defaultValue: Int): Int =
        prefs?.getInt(key, defaultValue) ?: defaultValue

    /**
     * Save a boolean for this [Preference]s' [key] to the [SharedPreferences] of the attached [PreferenceScreen]
     */
    fun commitBoolean(value: Boolean) {
        prefs?.edit {
            putBoolean(key, value)
        }
    }

    fun getBoolean(defaultValue: Boolean): Boolean =
        prefs?.getBoolean(key, defaultValue) ?: defaultValue

    /**
     * Save a String for this [Preference]s' [key] to the [SharedPreferences] of the attached [PreferenceScreen]
     */
    fun commitString(value: String?) {
        prefs?.edit {
            putString(key, value)
        }
    }

    fun getString(): String? = prefs?.getString(key, null)

    @Deprecated(
        "Passing a default value is not supported anymore, " +
                "use the nullable replacement getString() and an elvis operator",
        ReplaceWith("getString() ?: defaultValue"),
        DeprecationLevel.ERROR
    )
    fun getString(@Suppress("UNUSED_PARAMETER") defaultValue: String): String =
        throw UnsupportedOperationException("Not implemented")

    fun commitStringSet(values: Set<String>?) {
        prefs?.edit {
            putStringSet(key, values)
        }
    }

    fun getStringSet(): Set<String>? = prefs?.getStringSet(key, null)

    /**
     * Can be set to [Preference.preBindListener]
     */
    fun interface OnPreBindListener {
        /**
         * Called before [Preference.bindViews], allows you to set data right before the [Preference][preference]
         * is bound to a view.
         * Note that you mustn't compute any data here, as you'll block the UI thread by doing that.
         */
        fun onPreBind(preference: Preference, holder: PreferencesAdapter.ViewHolder)
    }

    fun interface OnClickListener {
        /**
         * Notified when the connected [Preference] is clicked
         *
         * @return true if the preference changed and needs to update its views
         */
        fun onClick(preference: Preference, holder: PreferencesAdapter.ViewHolder): Boolean
    }

    fun interface OnLongClickListener {
        /**
         * Notified when the connected [Preference] is long clicked
         *
         * @return true if the preference changed and needs to update its views
         */
        fun onLongClick(preference: Preference, holder: PreferencesAdapter.ViewHolder): Boolean
    }

    /**
     * Global configuration object that allows to apply settings to *all* [Preference] instances
     */
    object Config {
        /**
         * Override the maximum allowed number of lines for the title text.
         *
         * 1 by default, allowed values are 1 to 3.
         */
        @Suppress("MagicNumber")
        var titleMaxLines: Int = 2
            set(value) {
                require(value in 1..3) { "titleMaxLines must be within [1,3]" }
                field = value
            }

        /**
         * Override the maximum allowed number of lines for the summary text.
         *
         * 3 by default, allowed values are 1 to 5.
         */
        @Suppress("MagicNumber")
        var summaryMaxLines: Int = 3
            set(value) {
                require(value in 1..5) { "subtitleMaxLines must be within [1,5]" }
                field = value
            }
    }
}

/**
 * Management class for Preference views. Contains a list of preferences, created through [PreferenceScreen.Builder].
 *
 * It extends the [Preference] class, but gets handled slightly differently in a few things:
 * - [PreferenceScreen]s don't have a key attached to them
 * - Every [PreferenceScreen] can be bound to a different [SharedPreferences] file
 * - Even though you can change the [enabled] or the [persistent] state, it doesn't have any effect in this instance
 */
class PreferenceScreen internal constructor(builder: Builder) : Preference(builder.key) {
    internal val prefs = builder.prefs
    private val keyMap: Map<String, Preference> = builder.keyMap
    private val preferences: List<Preference> = builder.preferences
    internal val collapseIcon: Boolean = builder.collapseIcon
    internal val centerIcon: Boolean = builder.centerIcon
    internal val searchQuery: String? = builder.searchQuery

    internal var adapter: PreferencesAdapter? = null

    var scrollPosition = 0
    var scrollOffset = 0

    fun getSearchQuery(): String? {
        return searchQuery
    }

    init {
        copyAbstract(builder)
        for (i in preferences.indices)
            preferences[i].attachToScreen(this, i)
    }

    fun getPreferenceList(): List<Preference> {
        return preferences
    }

    /**
     * Gets the [Preference] at the specified index on this screen
     *
     * @throws IndexOutOfBoundsException if index > [size]
     */
    operator fun get(index: Int) = preferences[index]

    /**
     * Gets a [Preference] on this screen by its key
     */
    operator fun get(key: String) = keyMap[key]

    /**
     * Find the index of the Preference with the supplied [key]
     *
     * @return the index or -1 if it wasn't found
     */
    fun indexOf(key: String): Int {
        if (key in this) {
            for (i in preferences.indices) {
                if (key == preferences[i].key) return i
            }
        }
        return -1
    }

    fun size() = preferences.size

    operator fun contains(key: String) = keyMap.containsKey(key)

    /**
     * Request rebind of the Preference in this screen with the specified [key]
     * No-op if this screen doesn't contain such preference
     */
    fun requestRebind(key: String) {
        val index = indexOf(key)
        if (index > 0) requestRebind(index)
    }

    internal fun requestRebind(position: Int, itemCount: Int = 1) {
        adapter?.notifyItemRangeChanged(position, itemCount)
    }

    override fun equals(other: Any?): Boolean {
        return other is PreferenceScreen && key == other.key && preferences == other.preferences
    }

    override fun hashCode() = (31 * key.hashCode()) + preferences.hashCode()

    @PreferenceMarker
    class Builder(private var context: Context?, key: String) :
        AbstractPreference(key), Appendable {
        constructor(context: Context?) : this(context, KEY_ROOT_SCREEN)
        constructor(builder: Builder, key: String = "") : this(builder.context, key)
        constructor(collapse: CollapsePreference, key: String = "") : this(
            collapse.screen?.context,
            key
        )

        // Internal structures
        internal var prefs: SharedPreferences? = null
        internal val keyMap = HashMap<String, Preference>()
        internal val preferences = ArrayList<Preference>()

        fun getPreferences(): ArrayList<Preference> {
            return preferences
        }

        var searchQuery: String? = null

        /**
         * If true, the preference items in this screen will have a smaller left padding when they have no icon
         */
        var collapseIcon: Boolean = false

        /**
         * Center the icon inside its keylines. If false, it will be aligned with a potential back arrow in the toolbar
         */
        var centerIcon: Boolean = true

        /**
         * Add the specified preference to this screen - it doesn't make sense to call this directly,
         * use the dsl helper methods like [pref][de.maxr1998.modernpreferences.helpers.pref],
         * [switch][de.maxr1998.modernpreferences.helpers.switch] and
         * [subScreen][de.maxr1998.modernpreferences.helpers.subScreen] for this.
         */
        override fun addPreferenceItem(p: Preference) {
            if (p.key == KEY_ROOT_SCREEN) {
                val message by lazy {
                    """
                    A screen with key '$KEY_ROOT_SCREEN' cannot be added as a sub-screen!
                    If you are trying to add a sub-screen to your preferences model,
                    use the `subScreen {}` function.
                    """.trimIndent()
                }
                throw UnsupportedOperationException(message)
            }
            if (p.key.isEmpty() && p !is PreferenceScreen) {
                throw UnsupportedOperationException("Preference key may not be empty!")
            }
            if (p.key.isNotEmpty() && keyMap.put(p.key, p) != null) {
                throw UnsupportedOperationException("A preference with this key is already in the screen!")
            }

            preferences.add(p)
        }

        fun build(): PreferenceScreen {
            prefs = context?.let { getPreferences(it) }
            context = null
            return PreferenceScreen(this)
        }
    }

    companion object {
        fun getPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(
                (context.packageName ?: "package") + "_preferences",
                Context.MODE_PRIVATE
            )
        }
    }

    fun interface Appendable {
        fun addPreferenceItem(p: Preference)

        operator fun plusAssign(p: Preference) {
            addPreferenceItem(p)
        }

        operator fun <T : Preference> T.unaryPlus(): T {
            addPreferenceItem(this)
            return this
        }
    }
}
