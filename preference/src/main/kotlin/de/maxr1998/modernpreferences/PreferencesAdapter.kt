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

import android.animation.StateListAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.use
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.maxr1998.modernpreferences.helpers.DISABLED_RESOURCE_ID
import de.maxr1998.modernpreferences.helpers.categoryHeader
import de.maxr1998.modernpreferences.preferences.AccentButtonPreference
import de.maxr1998.modernpreferences.preferences.CategoryHeader
import de.maxr1998.modernpreferences.preferences.CollapsePreference
import de.maxr1998.modernpreferences.preferences.ExpandableTextPreference
import de.maxr1998.modernpreferences.preferences.ImagePreference
import de.maxr1998.modernpreferences.preferences.SeekBarPreference
import java.util.Stack
import kotlin.math.max

@Suppress("MemberVisibilityCanBePrivate", "NotifyDataSetChanged")
class PreferencesAdapter @VisibleForTesting constructor(
    root: PreferenceScreen? = null,
    hasStableIds: Boolean,
) : RecyclerView.Adapter<PreferencesAdapter.ViewHolder>() {

    private companion object {
        val emptyScreen: PreferenceScreen by lazy {
            PreferenceScreen.Builder(null).build()
        }
    }

    constructor(root: PreferenceScreen? = null) : this(root, true)

    private val screenStack: Stack<PreferenceScreen> = Stack<PreferenceScreen>().apply {
        push(emptyScreen)
    }

    private fun findPrefKey(
        preference: Preference,
        key: String,
        once: Boolean,
        found: OnPreferenceFoundListener
    ) {
        if (preference.key == key) {
            found.onFoundPreference(preference)
            if (once) {
                return
            }
        }
        if (preference is PreferenceScreen) {
            for (i in preference.getPreferenceList()) {
                findPrefKey(i, key, once, found)
            }
        }
    }

    private fun findScreenInRoot(screen: PreferenceScreen, key: String): PreferenceScreen? {
        if (screen.key == key) {
            return screen
        }
        for (i in screen.getPreferenceList()) {
            if (i is PreferenceScreen) {
                val k = findScreenInRoot(i, key)
                if (k != null) {
                    return k
                }
            }
        }
        return null
    }

    private fun findString(
        context: Context,
        str: CharSequence?,
        @StringRes res: Int,
        query: String
    ): Boolean {
        val obj = if (res != DISABLED_RESOURCE_ID) context.resources.getString(res) else str
        if (obj.isNullOrEmpty()) {
            return false
        }
        return obj.contains(query, true)
    }

    private fun prefNameHas(context: Context, query: String, preference: Preference): Boolean {
        return (findString(context, preference.title, preference.titleRes, query) || findString(
            context,
            preference.summary,
            preference.summaryRes,
            query
        ) || findString(
            context,
            preference.summaryDisabled,
            preference.summaryDisabledRes,
            query
        ) || preference.key.contains(query, true)
                )
    }

    private fun findPrefByName(
        context: Context,
        preference: Preference,
        query: String,
        found: PreferenceScreen.Builder, view: View?
    ) {
        if (preference is CategoryHeader || preference is CollapsePreference) {
            return
        } else if (preference is PreferenceScreen) {
            for (i in preference.getPreferenceList()) {
                findPrefByName(context, i, query, found, view)
            }
        } else {
            if (prefNameHas(context, query, preference)) {
                val tmp = preference.makeCopyForFind()
                tmp.makeInSearchParentStoryFrom(context, preference)
                found.addPreferenceItem(tmp)
            }
        }
    }

    fun findPreferences(context: Context, query: String, view: View?) {
        val data = currentScreen
        val builder = PreferenceScreen.Builder(context, "found_result")
        builder.searchQuery = query
        builder.titleRes = R.string.pref_find
        builder.categoryHeader("found_result_header") {
            title = context.getString(R.string.pref_found, query)
        }
        findPrefByName(context, data, query, builder, view)
        var iconHas = false
        for (s in builder.getPreferences()) {
            if (s.iconRes != DISABLED_RESOURCE_ID || s.icon != null) {
                iconHas = true
                break
            }
        }
        builder.collapseIcon = !iconHas
        openScreen(builder.build())
    }

    fun applyToPreference(key: String, found: OnPreferenceFoundListener) {
        for (i in screenStack) {
            findPrefKey(i, key, once = false, found = found)
        }
    }

    fun applyToPreferenceInScreen(screen: String?, key: String, found: OnPreferenceFoundListener) {
        for (i in screenStack) {
            if (screen == null || i.key == screen) {
                findPrefKey(i, key, once = true, found = found)
            }
        }
    }

    fun interface OnPreferenceFoundListener {
        fun onFoundPreference(preference: Preference)
    }

    val currentScreen: PreferenceScreen
        get() = screenStack.peek()

    /**
     * Listener which will be notified of screen change events
     *
     * Will dispatch the initial state when attached.
     */
    var onScreenChangeListener: OnScreenChangeListener? = null

    /**
     * A [StateListAnimator] that will be applied to all Preference item views
     *
     * *Attention*: This animator will be cloned using [Object.clone] before getting applied.
     */
    var stateListAnimator: StateListAnimator? = null

    init {
        // Necessary for testing, because setHasStableIds calls into an (in the stubbed android.jar)
        // uninitialized observer list which causes a NPE
        if (hasStableIds) setHasStableIds(true)
        root?.let(::setRootScreen)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        if (recyclerView.layoutManager !is LinearLayoutManager) {
            throw UnsupportedOperationException("ModernAndroidPreferences requires a LinearLayoutManager")
        }
    }

    @MainThread
    fun setRootScreen(root: PreferenceScreen) {
        if (onScreenChangeListener?.beforeScreenChange(root) == false) return
        currentScreen.adapter = null
        while (screenStack.peek() != emptyScreen) {
            screenStack.pop()
        }
        screenStack.push(root)
        notifyDataSetChanged()
        currentScreen.adapter = this
        onScreenChangeListener?.onScreenChanged(root, subScreen = false, animation = true)
    }

    @VisibleForTesting
    @MainThread
    internal fun openScreen(screen: PreferenceScreen) {
        if (onScreenChangeListener?.beforeScreenChange(screen) == false) return

        currentScreen.adapter = null
        screenStack.push(screen)
        notifyDataSetChanged()
        currentScreen.adapter = this
        onScreenChangeListener?.onScreenChanged(screen, subScreen = true, animation = true)
    }

    fun isInSubScreen() = screenStack.size > 2

    /**
     * If possible, return to the previous screen.
     *
     * @return true if it returned to an earlier screen, false if we're already at the root
     */
    @MainThread
    fun goBack(): Boolean = when {
        // Can't go back when not in a subscreen
        !isInSubScreen() -> false
        // Callback may disallow screen change
        onScreenChangeListener?.beforeScreenChange(screenStack[screenStack.size - 2]) == false -> true
        // Change screens!
        else -> {
            currentScreen.adapter = null
            screenStack.pop()
            notifyDataSetChanged()
            currentScreen.adapter = this
            onScreenChangeListener?.onScreenChanged(currentScreen, isInSubScreen(), true)
            true
        }
    }

    @MainThread
    fun canGoBack(): Boolean = isInSubScreen()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val layout = when (viewType) {
            CategoryHeader.RESOURCE_CONST -> R.layout.map_preference_category
            AccentButtonPreference.RESOURCE_CONST -> R.layout.map_accent_button_preference
            ImagePreference.RESOURCE_CONST -> R.layout.map_image_preference
            else -> R.layout.map_preference
        }

        // Inflate main preference view
        val view = layoutInflater.inflate(layout, parent, false)
        view.stateListAnimator = try {
            stateListAnimator?.clone()
        } catch (e: NoSuchMethodError) {
            // Some awful Android 5 devices apparently don't implement the clone method,
            // although it's part of the Android SDK since API 21. Thus, we catch it and return null instead.
            Log.e(
                "PreferencesAdapter",
                "Missing `clone()` method, stateListAnimator won't work for preferences",
                e
            )
            null
        }

        // Inflate preference widget
        if (viewType > 0) layoutInflater.inflate(
            viewType,
            view.findViewById(R.id.map_widget_frame),
            true
        )

        return ViewHolder(viewType, view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pref = currentScreen[position]
        pref.bindViews(holder)

        // Category header and seek bar shouldn't be clickable
        if (pref is CategoryHeader || pref is SeekBarPreference) {
            holder.itemView.setOnClickListener(null)
            holder.itemView.setOnLongClickListener(null)
        }

        holder.itemView.setOnClickListener {
            if (pref is PreferenceScreen) {
                openScreen(pref) // Navigate to sub screen
            } else pref.performClick(holder)
        }
        holder.itemView.setOnLongClickListener {
            pref.performLongClick(holder)
        }
    }

    override fun getItemCount() = currentScreen.size()

    override fun getItemId(position: Int) = currentScreen[position].hashCode().toLong()

    @LayoutRes
    override fun getItemViewType(position: Int) = currentScreen[position].getWidgetLayoutResource()

    /**
     * Restores the last scroll position if needed and (re-)attaches this adapter's scroll listener.
     *
     * Should be called from [OnScreenChangeListener.onScreenChanged].
     */
    fun restoreAndObserveScrollPosition(preferenceView: RecyclerView) {
        with(currentScreen) {
            if (scrollPosition != 0 || scrollOffset != 0) {
                val layoutManager = preferenceView.layoutManager as LinearLayoutManager?
                layoutManager?.scrollToPositionWithOffset(scrollPosition, scrollOffset)
            } else {
                val layoutManager = preferenceView.layoutManager as LinearLayoutManager?
                layoutManager?.scrollToPositionWithOffset(0, 0)
            }
        }
        preferenceView.addOnScrollListener(scrollListener)
    }

    fun stopObserveScrollPosition(preferenceView: RecyclerView) {
        preferenceView.removeOnScrollListener(scrollListener)
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(r: RecyclerView, state: Int) {
            if (state == RecyclerView.SCROLL_STATE_IDLE) currentScreen.apply {
                val layoutManager = r.layoutManager as LinearLayoutManager?
                scrollPosition = layoutManager?.findFirstCompletelyVisibleItemPosition() ?: 0
                scrollOffset =
                    r.findViewHolderForAdapterPosition(scrollPosition)?.run { itemView.top } ?: 0
            }
        }
    }

    /**
     * Common ViewHolder in [PreferencesAdapter] for every [Preference] object/every preference extending it
     */
    class ViewHolder internal constructor(
        type: Int,
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val root get() = itemView as ViewGroup
        val iconFrame: View = itemView.findViewById(R.id.map_icon_frame)
        val icon: ImageView? = itemView.findViewById(android.R.id.icon)
        val title: TextView = itemView.findViewById(android.R.id.title)
        val summary: TextView? = itemView.findViewById(android.R.id.summary)
        val badge: TextView? = itemView.findViewById(R.id.map_badge)
        val widgetFrame: ViewGroup? = itemView.findViewById(R.id.map_widget_frame)
        val widget: View? = widgetFrame?.getChildAt(0)

        init {
            // Apply accent text color via theme attribute from library or fallback to AppCompat
            val attrs =
                intArrayOf(R.attr.mapAccentTextColor, androidx.appcompat.R.attr.colorPrimary)
            val accentTextColor =
                itemView.context.theme.obtainStyledAttributes(attrs).use { array ->
                    // Return first resolved attribute or null
                    if (array.indexCount > 0) array.getColorStateList(array.getIndex(0)) else null
                }
                    ?: ColorStateList.valueOf(Color.BLACK) // fallback to black if no colorAccent is defined (unlikely)

            when (type) {
                CategoryHeader.RESOURCE_CONST,
                AccentButtonPreference.RESOURCE_CONST -> title.setTextColor(accentTextColor)
            }

            badge?.apply {
                setTextColor(accentTextColor)
                backgroundTintList = accentTextColor
                backgroundTintMode = PorterDuff.Mode.SRC_ATOP
            }
        }

        internal fun setEnabledState(enabled: Boolean) {
            setEnabledStateRecursive(itemView, enabled)
        }

        private fun setEnabledStateRecursive(v: View, enabled: Boolean) {
            v.isEnabled = enabled
            if (v is ViewGroup) {
                for (i in v.childCount - 1 downTo 0) {
                    setEnabledStateRecursive(v[i], enabled)
                }
            }
        }
    }

    /**
     * An interface to notify observers in [PreferencesAdapter] of screen change events,
     * when a sub-screen was opened or closed
     */
    interface OnScreenChangeListener {
        fun onScreenChanged(screen: PreferenceScreen, subScreen: Boolean, animation: Boolean)

        /**
         * Called when the user attempts to switch screens by pressing on a subscreen item or going back
         *
         * @return false to prevent the change from happening
         */
        fun beforeScreenChange(screen: PreferenceScreen): Boolean
    }

    fun getSavedState(): SavedState {
        val screenPath = ArrayList<ScreenInfo>(max(0, screenStack.size - 2))
        for (i in 2 until screenStack.size) {
            screenPath.add(
                ScreenInfo(
                    screenStack[i].key,
                    screenStack[i].searchQuery,
                    screenStack[i].scrollPosition,
                    screenStack[i].scrollOffset
                )
            )
        }
        val list: ArrayList<ExpandInfo> = ArrayList()
        for (i in 1 until screenStack.size) {
            findExpendedInScreen(screenStack[i], list)
        }
        return SavedState(screenPath, list)
    }

    private fun findExpendedInScreen(screen: PreferenceScreen, list: ArrayList<ExpandInfo>) {
        for (i in screen.getPreferenceList()) {
            if (i is CollapsePreference) {
                if (!i.visible) {
                    list.add(ExpandInfo(screen.key, i.key))
                }
            } else if (i is ExpandableTextPreference) {
                if (i.expanded) {
                    list.add(ExpandInfo(screen.key, i.key))
                }
            }
        }
    }

    private fun applyExpendedInScreen(screen: PreferenceScreen, list: ArrayList<ExpandInfo>) {
        for (i in screen.getPreferenceList()) {
            if (i is CollapsePreference) {
                if (list.contains(ExpandInfo(screen.key, i.key))) {
                    i.expand()
                }
            } else if (i is ExpandableTextPreference) {
                if (list.contains(ExpandInfo(screen.key, i.key))) {
                    i.expanded = true
                }
            }
        }
    }

    /**
     * Loads the specified state into this adapter
     *
     * @return whether the state could be loaded
     */
    @MainThread
    fun loadSavedState(
        context: Context,
        state: SavedState,
        root: PreferenceScreen,
        view: View?
    ): Boolean {
        currentScreen.adapter = null
        while (screenStack.peek() != emptyScreen) {
            screenStack.pop()
        }
        screenStack.push(root)
        if (screenStack.size != 2) return false
        state.screens.forEach { (key, searchQuery, scrollPosition, scrollOffset) ->
            val screen = if (searchQuery == null) {
                findScreenInRoot(screenStack[1], key)
            } else {
                val builder = PreferenceScreen.Builder(context, "found_result")
                builder.searchQuery = searchQuery
                builder.titleRes = R.string.pref_find
                builder.categoryHeader("found_result_header") {
                    title = context.getString(R.string.pref_found, searchQuery)
                }
                findPrefByName(context, currentScreen, searchQuery, builder, view)
                var iconHas = false
                for (s in builder.getPreferences()) {
                    if (s.iconRes != DISABLED_RESOURCE_ID || s.icon != null) {
                        iconHas = true
                        break
                    }
                }
                builder.collapseIcon = !iconHas
                builder.build()
            }
            if (screen != null) {
                screen.scrollOffset = scrollOffset
                screen.scrollPosition = scrollPosition
                screenStack.push(screen)
            }
        }
        for (i in 1 until screenStack.size) {
            applyExpendedInScreen(screenStack[i], state.expends)
        }
        currentScreen.adapter = this
        notifyDataSetChanged()
        onScreenChangeListener?.onScreenChanged(root, isInSubScreen(), false)
        return true
    }

    data class ScreenInfo(
        val key: String,
        val searchQuery: String?,
        val scrollPosition: Int,
        val scrollOffset: Int
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString(),
            parcel.readInt(),
            parcel.readInt()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(key)
            parcel.writeString(searchQuery)
            parcel.writeInt(scrollPosition)
            parcel.writeInt(scrollOffset)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<ScreenInfo> {
            override fun createFromParcel(parcel: Parcel): ScreenInfo {
                return ScreenInfo(parcel)
            }

            override fun newArray(size: Int): Array<ScreenInfo?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class ExpandInfo(
        val screenKey: String,
        val key: String,
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!
        )

        override fun equals(other: Any?): Boolean {
            return other is ExpandInfo && other.screenKey == screenKey && other.key == key
        }

        override fun hashCode(): Int {
            return screenKey.hashCode() + key.hashCode()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(screenKey)
            parcel.writeString(key)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<ExpandInfo> {
            override fun createFromParcel(parcel: Parcel): ExpandInfo {
                return ExpandInfo(parcel)
            }

            override fun newArray(size: Int): Array<ExpandInfo?> {
                return arrayOfNulls(size)
            }
        }
    }

    class SavedState : Parcelable {
        val screens: ArrayList<ScreenInfo>
        val expends: ArrayList<ExpandInfo>

        constructor(screens: ArrayList<ScreenInfo>, expends: ArrayList<ExpandInfo>) {
            this.screens = screens
            this.expends = expends
        }

        constructor(parcel: Parcel) {
            screens = parcel.createTypedArrayList(ScreenInfo.CREATOR)!!
            expends = parcel.createTypedArrayList(ExpandInfo.CREATOR)!!
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeTypedList(screens)
            parcel.writeTypedList(expends)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}
