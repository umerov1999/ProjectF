package dev.ragnarok.fenrir.util

import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcel
import android.util.ArrayMap
import android.util.SparseArray
import android.util.TypedValue
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.util.size
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.maxr1998.modernpreferences.PreferenceScreen
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.BuildConfig
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.Includes.proxySettings
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.UserAgentTool
import dev.ragnarok.fenrir.activity.MainActivity
import dev.ragnarok.fenrir.activity.SwipebleActivity
import dev.ragnarok.fenrir.activity.SwipebleActivity.Companion.start
import dev.ragnarok.fenrir.activity.qr.CustomQRCodeWriter
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.toRequestBuilder
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.vkHeader
import dev.ragnarok.fenrir.api.ProxyUtil.applyProxyConfig
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiOwner
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.interfaces.Identificable
import dev.ragnarok.fenrir.api.model.interfaces.IdentificableOwner
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.media.exo.OkHttpDataSource
import dev.ragnarok.fenrir.model.ISelectable
import dev.ragnarok.fenrir.model.ISomeones
import dev.ragnarok.fenrir.model.Lang
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.model.ReactionAsset
import dev.ragnarok.fenrir.model.Sticker.LocalSticker
import dev.ragnarok.fenrir.module.FenrirNative
import dev.ragnarok.fenrir.module.animation.thorvg.ThorVGSVGRender
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.util.AppTextUtils.updateDateLang
import dev.ragnarok.fenrir.util.FileUtil.updateDateLang
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.view.emoji.EmojiconTextView
import dev.ragnarok.fenrir.view.natives.animation.ThorVGLottieView
import dev.ragnarok.fenrir.view.pager.BackgroundToForegroundTransformer
import dev.ragnarok.fenrir.view.pager.ClockSpinTransformer
import dev.ragnarok.fenrir.view.pager.CubeInDepthTransformer
import dev.ragnarok.fenrir.view.pager.DepthTransformer
import dev.ragnarok.fenrir.view.pager.FanTransformer
import dev.ragnarok.fenrir.view.pager.GateTransformer
import dev.ragnarok.fenrir.view.pager.SliderTransformer
import dev.ragnarok.fenrir.view.pager.Transformers_Types
import dev.ragnarok.fenrir.view.pager.ZoomOutTransformer
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.Closeable
import java.io.IOException
import java.util.Calendar
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

object Utils {
    private val reload_news: MutableList<Long> = LinkedList()
    private val reload_dialogs: MutableList<Long> = LinkedList()
    private val cachedMyStickers: MutableList<LocalSticker> = LinkedList()
    private val registeredParcels: MutableSet<Long> = HashSet()
    private val reactionsAssets: MutableMap<Long, MutableMap<Int, ReactionAsset>> =
        Collections.synchronizedMap(LinkedHashMap())
    private val reload_reactions_assets: MutableList<Long> = LinkedList()

    var follower_kick_mode = false
    private val displaySize = Point()
    private var device_id: String? = null
    private var hiddenDevice_id: String? = null
    var density = 1f
        private set
    var scaledDensity = 1f
        private set
    var isCompressIncomingTraffic = true
    var isCompressOutgoingTraffic = false
    var currentParser = 0

    var currentColorsReplacement = ArrayMap<String, Int>()

    fun getReactionsAssets(): MutableMap<Long, MutableMap<Int, ReactionAsset>> {
        return reactionsAssets
    }

    fun getCachedMyStickers(): MutableList<LocalSticker> {
        return cachedMyStickers
    }

    fun registerParcelNative(pointer: Long) {
        registeredParcels.add(pointer)
    }

    fun unregisterParcelNative(pointer: Long) {
        registeredParcels.remove(pointer)
    }

    fun isParcelNativeRegistered(pointer: Long): Boolean {
        return registeredParcels.contains(pointer)
    }

    /*
    fun compareFingerprintHashForPackage(context: Context): Boolean {
        return try {
            val sign = context.getSignature()
            if (sign != null) {
                return "hash" == StringHash.calculateSha1(sign.toByteArray())
            }
            true
        } catch (_: Exception) {
            true
        }
    }
     */

    fun registerColorsThorVG(context: Context) {
        if (!FenrirNative.isNativeLoaded) {
            return
        }
        val tmpMap = mapOf(
            "primary_color" to CurrentTheme.getColorPrimary(context),
            "secondary_color" to CurrentTheme.getColorSecondary(context),
            "on_surface_color" to CurrentTheme.getColorOnSurface(context),
            "white_color_contrast_fix" to CurrentTheme.getColorWhiteContrastFix(context),
            "black_color_contrast_fix" to CurrentTheme.getColorBlackContrastFix(context)
        )
        if (currentColorsReplacement != tmpMap) {
            currentColorsReplacement.clear()
            currentColorsReplacement.putAll(tmpMap)
            ThorVGSVGRender.registerColors(currentColorsReplacement)
        }
    }

    fun needReloadNews(account_id: Long): Boolean {
        if (!reload_news.contains(account_id)) {
            reload_news.add(account_id)
            return true
        }
        return false
    }

    fun needReloadReactionAssets(account_id: Long): Boolean {
        if (!reload_reactions_assets.contains(account_id)) {
            reload_reactions_assets.add(account_id)
            return true
        }
        return false
    }

    fun needReloadDialogs(account_id: Long): Boolean {
        if (!reload_dialogs.contains(account_id)) {
            reload_dialogs.add(account_id)
            return true
        }
        return false
    }

    fun needReloadStickerSets(account_id: Long): Boolean {
        Settings.get().main().get_last_sticker_sets_sync(account_id).let {
            if (it <= 0 || (System.currentTimeMillis() / 1000L) - it > 900) {
                Settings.get().main()
                    .set_last_sticker_sets_sync(account_id, System.currentTimeMillis() / 1000L)
                return true
            }
        }
        return false
    }

    fun needReloadStickerSetsCustom(account_id: Long): Boolean {
        Settings.get().main().get_last_sticker_sets_custom_sync(account_id).let {
            if (it <= 0 || (System.currentTimeMillis() / 1000L) - it > 400) {
                Settings.get().main()
                    .set_last_sticker_sets_custom_sync(
                        account_id,
                        System.currentTimeMillis() / 1000L
                    )
                return true
            }
        }
        return false
    }

    fun needReloadStickerKeywords(account_id: Long): Boolean {
        Settings.get().main().get_last_sticker_keywords_sync(account_id).let {
            if (it <= 0 || (System.currentTimeMillis() / 1000L) - it > 3600) {
                Settings.get().main()
                    .set_last_sticker_keywords_sync(account_id, System.currentTimeMillis() / 1000L)
                return true
            }
        }
        return false
    }

    fun needFetchReactionAssets(account_id: Long): Boolean {
        Settings.get().main().get_last_reaction_assets_sync(account_id).let {
            if (it <= 0 || (System.currentTimeMillis() / 1000L) - it > 3600) {
                Settings.get().main()
                    .set_last_reaction_assets_sync(account_id, System.currentTimeMillis() / 1000L)
                return true
            }
        }
        return false
    }

    inline fun <reified T> lastOf(data: List<T>): T {
        return data[data.size - 1]
    }

    fun stringEmptyIfNull(orig: String?): String {
        return orig ?: ""
    }

    inline fun <reified T> listEmptyIfNull(orig: List<T>?): List<T> {
        return orig ?: emptyList()
    }

    inline fun <reified T> listEmptyIfNullMutable(orig: MutableList<T>?): MutableList<T> {
        return orig ?: mutableListOf()
    }

    fun <T> singletonArrayList(data: T): ArrayList<T> {
        val list = ArrayList<T>(1)
        list.add(data)
        return list
    }

    inline fun <reified T> findIndexByPredicate(data: List<T>?, predicate: (T) -> Boolean): Int {
        data ?: return -1
        for (i in data.indices) {
            if (predicate.invoke(data[i])) {
                return i
            }
        }
        return -1
    }

    fun <T> findInfoByPredicate(data: List<T>?, predicate: (T) -> Boolean): Pair<Int, T>? {
        data ?: return null
        for (i in data.indices) {
            val t = data[i]
            if (predicate.invoke(t)) {
                return create(i, t)
            }
        }
        return null
    }

    inline fun <reified T : Identificable> collectIds(
        data: Collection<T?>?,
        predicate: (T) -> Boolean
    ): List<Int> {
        data ?: return emptyList()
        val count = countOf(data, predicate)
        if (count == 0) {
            return emptyList()
        }
        val ids: MutableList<Int> = ArrayList(count)
        for (t in data) {
            if (t?.let { predicate.invoke(it) } == true) {
                ids.add(t.getObjectId())
            }
        }
        return ids
    }

    inline fun <reified T : Identificable> countOf(
        data: Collection<T?>?,
        predicate: (T) -> Boolean
    ): Int {
        data ?: return 0
        var count = 0
        for (t in data) {
            if (t?.let { predicate.invoke(it) } == true) {
                count++
            }
        }
        return count
    }

    fun getCauseIfRuntime(throwable: Throwable?): Throwable {
        var target = throwable
        while (target is RuntimeException) {
            if (target.cause == null) {
                break
            }
            target = target.cause
        }
        return target ?: Throwable()
    }

    inline fun <reified T> cloneListAsArrayList(original: List<T>?): ArrayList<T>? {
        if (original == null) {
            return null
        }
        val clone = ArrayList<T>(original.size)
        clone.addAll(original)
        return clone
    }

    inline fun <reified T> copyToArrayListWithPredicate(
        orig: List<T>,
        predicate: (T) -> Boolean
    ): ArrayList<T> {
        val data = ArrayList<T>(orig.size)
        for (t in orig) {
            if (predicate.invoke(t)) {
                data.add(t)
            }
        }
        return data
    }

    inline fun <reified T> join(
        tokens: Array<T>?,
        delimiter: String,
        crossinline function: (T) -> String?
    ): String? {
        if (tokens == null) {
            return null
        }
        val sb = StringBuilder()
        var firstTime = true
        for (token in tokens) {
            if (firstTime) {
                firstTime = false
            } else {
                sb.append(delimiter)
            }
            sb.append(function.invoke(token))
        }
        return sb.toString()
    }

    fun joinNonEmptyStrings(delimiter: String, vararg tokens: String?): String? {
        val nonEmpty: MutableList<String> = ArrayList()
        for (token in tokens) {
            if (token.nonNullNoEmpty()) {
                nonEmpty.add(token)
            }
        }
        return join(nonEmpty, delimiter) { orig ->
            orig
        }
    }

    inline fun <reified T> join(
        tokens: Iterable<T>?,
        delimiter: String,
        crossinline function: (T) -> String?
    ): String? {
        if (tokens == null) {
            return null
        }
        val sb = StringBuilder()
        var firstTime = true
        for (token in tokens) {
            if (firstTime) {
                firstTime = false
            } else {
                sb.append(delimiter)
            }
            sb.append(function.invoke(token))
        }
        return sb.toString()
    }

    inline fun <reified T, reified E : Collection<T>> join(
        tokens: E?,
        delimiter: String,
        crossinline function: (T) -> String
    ): String? {
        if (tokens == null) {
            return null
        }
        val sb = StringBuilder()
        var firstTime = true
        for (token in tokens) {
            if (firstTime) {
                firstTime = false
            } else {
                sb.append(delimiter)
            }
            sb.append(function.invoke(token))
        }
        return sb.toString()
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     *
     * @param tokens an array objects to be joined. Strings will be formed from
     * the objects by calling object.toString().
     */
    fun join(delimiter: CharSequence, tokens: Iterable<*>): String {
        val it = tokens.iterator()
        if (!it.hasNext()) {
            return ""
        }
        val sb = StringBuilder()
        if (it.hasNext()) {
            sb.append(it.next())
            while (it.hasNext()) {
                sb.append(delimiter)
                sb.append(it.next())
            }
        }
        return sb.toString()
    }

    fun join(delimiter: CharSequence, tokens: Array<*>): String {
        val it = tokens.iterator()
        if (!it.hasNext()) {
            return ""
        }
        val sb = StringBuilder()
        if (it.hasNext()) {
            sb.append(it.next())
            while (it.hasNext()) {
                sb.append(delimiter)
                sb.append(it.next())
            }
        }
        return sb.toString()
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     *
     * @param tokens an array strings to be joined
     */

    fun stringJoin(delimiter: CharSequence, vararg tokens: String): String {
        val sb = StringBuilder()
        var firstTime = true
        for (token in tokens) {
            if (firstTime) {
                firstTime = false
            } else {
                sb.append(delimiter)
            }
            sb.append(token)
        }
        return sb.toString()
    }

    fun safeLengthOf(text: CharSequence?): Int {
        return text?.length ?: 0
    }

    inline fun <reified T> indexOf(data: List<T>?, predicate: (T) -> Boolean): Int {
        data ?: return -1
        for (i in data.indices) {
            val t = data[i]
            if (predicate.invoke(t)) {
                return i
            }
        }
        return -1
    }

    inline fun <reified T> removeIf(
        data: MutableCollection<T>,
        predicate: (T) -> Boolean
    ): Boolean {
        var hasChanges = false
        val iterator = data.iterator()
        while (iterator.hasNext()) {
            if (predicate.invoke(iterator.next())) {
                iterator.remove()
                hasChanges = true
            }
        }
        return hasChanges
    }

    fun safelyCloseCursor(cursor: Cursor?) {
        cursor?.close()
    }

    fun safelyRecycle(bitmap: Bitmap?) {
        if (bitmap != null) {
            try {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun safelyClose(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (_: IOException) {
            }
        }
    }

    fun safeCountOf(sparseArray: SparseArray<*>?): Int {
        return sparseArray?.size ?: 0
    }

    fun safeCountOf(map: Map<*, *>?): Int {
        return map?.size ?: 0
    }

    fun safeCountOf(cursor: Cursor?): Int {
        return cursor?.count ?: 0
    }

    fun startOfTodayMillis(): Long {
        return startOfToday().timeInMillis
    }

    private fun startOfToday(): Calendar {
        val current = Calendar.getInstance()
        current[current[Calendar.YEAR], current[Calendar.MONTH], current[Calendar.DATE], 0, 0] = 0
        return current
    }

    fun idsListOf(data: Collection<Identificable>): List<Int> {
        val ids: MutableList<Int> = ArrayList(data.size)
        for (identifiable in data) {
            ids.add(identifiable.getObjectId())
        }
        return ids
    }

    fun idsListOfOwner(data: Collection<IdentificableOwner>): List<Long> {
        val ids: MutableList<Long> = ArrayList(data.size)
        for (identifiable in data) {
            ids.add(identifiable.getOwnerObjectId())
        }
        return ids
    }

    fun <T : Identificable> findById(data: Collection<T?>?, id: Int): T? {
        data ?: return null
        for (element in data) {
            if (element?.getObjectId() == id) {
                return element
            }
        }
        return null
    }

    fun <T : IdentificableOwner> findById(data: Collection<T?>?, id: Long): T? {
        data ?: return null
        for (element in data) {
            if (element?.getOwnerObjectId() == id) {
                return element
            }
        }
        return null
    }

    fun <T : Identificable> findIndexById(data: List<T?>?, id: Int): Int {
        data ?: return -1
        for (i in data.indices) {
            if (data[i]?.getObjectId() == id) {
                return i
            }
        }
        return -1
    }

    fun <T : IdentificableOwner> findIndexById(data: List<T?>?, id: Long): Int {
        data ?: return -1
        for (i in data.indices) {
            if (data[i]?.getOwnerObjectId() == id) {
                return i
            }
        }
        return -1
    }

    fun <T : ISomeones> findIndexById(data: List<T?>?, id: Int, ownerId: Long): Int {
        data ?: return -1
        for (i in data.indices) {
            val t = data[i]
            if (t?.getObjectId() == id && t.ownerId == ownerId) {
                return i
            }
        }
        return -1
    }

    fun <T : ISelectable> getSelected(fullData: List<T>): ArrayList<T> {
        return getSelected(fullData, false)
    }

    fun <T : ISelectable> getSelected(fullData: List<T>, reverse: Boolean): ArrayList<T> {
        val result = ArrayList<T>()
        if (reverse) {
            for (i in fullData.indices.reversed()) {
                val m = fullData[i]
                if (m.isSelected) {
                    result.add(m)
                }
            }
        } else {
            for (item in fullData) {
                if (item.isSelected) {
                    result.add(item)
                }
            }
        }
        return result
    }

    fun countOfSelection(data: List<ISelectable>?): Int {
        data ?: return 0
        var count = 0
        for (selectable in data) {
            if (selectable.isSelected) {
                count++
            }
        }
        return count
    }

    fun hasFlag(mask: Int, flag: Int): Boolean {
        return mask and flag != 0
    }

    fun removeFlag(mask: Int, flag: Int): Int {
        var mMask = mask
        mMask = mMask and flag.inv()
        return mMask
    }

    fun addFlagIf(mask: Int, flag: Int, ifTrue: Boolean): Int {
        return if (ifTrue) {
            mask + flag
        } else mask
    }

    /**
     * Проверка, содержит ли маска флаги
     *
     * @param mask  маска
     * @param flags флаги
     * @return если содержит - true
     */
    fun hasFlags(mask: Int, vararg flags: Int): Boolean {
        for (flag in flags) {
            if (!hasFlag(mask, flag)) {
                return false
            }
        }
        return true
    }

    /**
     * Проверка, содержит ли маска какой нибудь из флагов
     *
     * @param mask  маска
     * @param flags флаги
     * @return если содержит - true
     */
    fun hasSomeFlag(mask: Int, vararg flags: Int): Boolean {
        for (flag in flags) {
            if (hasFlag(mask, flag)) {
                return true
            }
        }
        return false
    }

    /**
     *
     * Adds an object to the list. The object will be inserted in the correct
     * place so that the objects in the list are sorted. When the list already
     * contains objects that are equal according to the comparator, the new
     * object will be inserted immediately after these other objects.
     *
     * @param o the object to be added
     */

    fun <T> addElementToList(o: T, data: MutableList<T>, comparator: Comparator<T>): Int {
        var i = 0
        var found = false
        while (!found && i < data.size) {
            found = comparator.compare(o, data[i]) < 0
            if (!found) {
                i++
            }
        }
        data.add(i, o)
        return i
    }

    fun hasScopedStorage(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && BuildConfig.TARGET_SDK >= Build.VERSION_CODES.R
    }

    fun hasTiramisuTarget(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && BuildConfig.TARGET_SDK >= Build.VERSION_CODES.TIRAMISU
    }

    fun hasVanillaIceCreamTarget(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && BuildConfig.TARGET_SDK >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    }

    fun finishActivityImmediate(activity: Activity) {
        activity.finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("deprecation")
            activity.overridePendingTransition(0, 0)
        } else {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
    }

    fun activityTransactionImmediate(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("deprecation")
            activity.overridePendingTransition(0, 0)
        } else {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        }
    }

    fun indexOf(data: List<Identificable>?, id: Int): Int {
        data ?: return -1
        for (i in data.indices) {
            if (data[i].getObjectId() == id) {
                return i
            }
        }
        return -1
    }

    fun indexOf(data: List<IdentificableOwner>?, id: Long): Int {
        data ?: return -1
        for (i in data.indices) {
            if (data[i].getOwnerObjectId() == id) {
                return i
            }
        }
        return -1
    }

    fun indexOfOwner(data: List<Owner>?, o: Owner?): Int {
        if (data == null || o == null) {
            return -1
        }
        for (i in data.indices) {
            if (data[i].ownerId == o.ownerId) {
                return i
            }
        }
        return -1
    }

    fun ownerOfApiOwner(
        users: List<VKApiUser>?,
        groups: List<VKApiCommunity>?,
        id: Long
    ): VKApiOwner? {
        if (users.isNullOrEmpty() && id >= 0 || groups.isNullOrEmpty() && id < 0) {
            return null
        }
        if (id < 0) {
            for (i in groups.orEmpty()) {
                if (i.id == abs(id)) {
                    return i
                }
            }
        } else {
            for (i in users.orEmpty()) {
                if (i.id == id) {
                    return i
                }
            }
        }
        return null
    }

    fun firstNonEmptyString(vararg array: String?): String? {
        for (s in array) {
            if (!s.isNullOrEmpty()) {
                return s
            }
        }
        return null
    }

    inline fun <reified T> firstNonNull(vararg items: T?): T? {
        for (t in items) {
            if (t != null) {
                return t
            }
        }
        return null
    }

    fun <T> createSingleElementList(element: T): ArrayList<T> {
        val list = ArrayList<T>()
        list.add(element)
        return list
    }

    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == ORIENTATION_LANDSCAPE
    }

    fun is600dp(context: Context): Boolean {
        return context.resources.getBoolean(R.bool.is_tablet)
    }

    fun intValueNotIn(value: Int, vararg variants: Int): Boolean {
        for (variant in variants) {
            if (value == variant) {
                return false
            }
        }
        return true
    }

    fun intValueIn(value: Int, vararg variants: Int): Boolean {
        for (variant in variants) {
            if (value == variant) {
                return true
            }
        }
        return false
    }

    fun hasOneElement(collection: Collection<*>?): Boolean {
        return safeCountOf(collection) == 1
    }

    fun safeCountOf(collection: Collection<*>?): Int {
        return collection?.size ?: 0
    }

    fun safeCountOfMultiple(vararg collections: Collection<*>?): Int {
        var count = 0
        for (collection in collections) {
            count += safeCountOf(collection)
        }
        return count
    }

    /**
     * Добавляет прозрачность к цвету
     *
     * @param color  цвет
     * @param factor степень прозрачности
     * @return прозрачный цвет
     */

    fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).roundToInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    val androidVersion: String
        get() {
            val release = Build.VERSION.RELEASE
            val sdkVersion = Build.VERSION.SDK_INT
            return "Android SDK: $sdkVersion ($release)"
        }
    val deviceName: String
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                capitalize(model)
            } else {
                capitalize(manufacturer) + " " + model
            }
        }

    val isHiddenCurrent: Boolean
        get() = isHiddenAccount(Settings.get().accounts().current)

    fun isHiddenAccount(account_id: Long): Boolean {
        return isHiddenType(Settings.get().accounts().getType(account_id))
    }

    fun isHiddenType(@AccountType accountType: Int): Boolean {
        return accountType == AccountType.VK_ANDROID_HIDDEN || accountType == AccountType.KATE_HIDDEN || accountType == AccountType.IOS_HIDDEN
    }

    val isOfficialVKCurrent: Boolean
        get() = isOfficialVKAccount(Settings.get().accounts().current)

    fun isOfficialVKAccount(account_id: Long): Boolean {
        val accType = Settings.get().accounts().getType(account_id)
        return accType == AccountType.VK_ANDROID || accType == AccountType.VK_ANDROID_HIDDEN || accType == AccountType.IOS_HIDDEN
    }

    fun getDeviceId(@AccountType type: Int, context: Context): String {
        if (isHiddenType(type)) {
            if (hiddenDevice_id.isNullOrEmpty()) {
                hiddenDevice_id =
                    PreferenceScreen.getPreferences(context).getString("hidden_device_id", null)
                if (hiddenDevice_id.isNullOrEmpty()) {
                    val allowedChars = ('a'..'f') + ('0'..'9')
                    hiddenDevice_id = (1..16).map { allowedChars.random() }
                        .joinToString("") + ":" + (1..32).map { allowedChars.random() }
                        .joinToString("")
                    PreferenceScreen.getPreferences(context).edit {
                        putString("hidden_device_id", hiddenDevice_id)
                    }
                }
            }
            return hiddenDevice_id!!
        } else {
            if (device_id.isNullOrEmpty()) {
                device_id =
                    PreferenceScreen.getPreferences(context).getString("installation_id", null)
                if (device_id.isNullOrEmpty()) {
                    val allowedChars = ('a'..'f') + ('0'..'9')
                    device_id = (1..16).map { allowedChars.random() }
                        .joinToString("") + ":" + (1..32).map { allowedChars.random() }
                        .joinToString("")
                    PreferenceScreen.getPreferences(context).edit {
                        putString("installation_id", device_id)
                    }
                }
            }
            return device_id!!
        }
    }

    /**
     * @return Application's version code from the `PackageManager`.
     */

    fun getAppVersionName(context: Context): String? {
        return try {
            val packageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ) else context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun capitalize(s: String?): String {
        if (s.isNullOrEmpty()) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first).toString() + s.substring(1)
        }
    }

    fun dpToPx(dp: Float, context: Context): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    fun spToPx(dp: Float, context: Context): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            dp,
            context.resources.displayMetrics
        )
    }

    fun shareLink(activity: Activity, link: String?, subject: String?) {
        link ?: return
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        sharingIntent.putExtra(Intent.EXTRA_TEXT, link)
        activity.startActivity(
            Intent.createChooser(
                sharingIntent,
                activity.resources.getString(R.string.share_using)
            )
        )
    }

    fun setTint(view: ImageView?, @ColorInt color: Int) {
        view?.imageTintList = ColorStateList.valueOf(color)
    }

    fun setBackgroundTint(view: View?, @ColorInt color: Int) {
        view?.backgroundTintList = ColorStateList.valueOf(color)
    }

    fun setColorFilter(drawable: Drawable?, @ColorInt color: Int) {
        if (drawable == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        } else {
            @Suppress("deprecation")
            drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        }
    }

    fun setColorFilter(view: FloatingActionButton?, @ColorInt color: Int) {
        if (view == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        } else {
            view.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        }
    }

    fun setColorFilter(view: ImageView?, @ColorInt color: Int) {
        if (view == null) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            view.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        } else {
            view.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
        }
    }

    @StringRes
    fun declOfNum(number_z: Int, @StringRes titles: IntArray): Int {
        val number = abs(number_z)
        val cases = intArrayOf(2, 0, 1, 1, 1, 2)
        return titles[if (number % 100 in 5..19) 2 else cases[(number % 10).coerceAtMost(5)]]
    }

    @StringRes
    fun declOfNum(number_z: Long, @StringRes titles: IntArray): Int {
        val number = abs(number_z)
        val cases = intArrayOf(2, 0, 1, 1, 1, 2)
        return titles[if (number % 100 in 5..19) 2 else cases[(number % 10).coerceAtMost(5)
            .toInt()]]
    }

    fun doWavesLottie(visual: ThorVGLottieView?, Play: Boolean) {
        if (Play) {
            visual?.setRepeat(true)
            visual?.fromRes(dev.ragnarok.fenrir_common.R.raw.waves)
        } else {
            visual?.setRepeat(false)
            visual?.fromRes(dev.ragnarok.fenrir_common.R.raw.waves_end)
        }
        visual?.startAnimation()
    }

    fun doWavesLottieBig(visual: ThorVGLottieView?, Play: Boolean) {
        if (Play) {
            visual?.setRepeat(true)
            visual?.fromRes(dev.ragnarok.fenrir_common.R.raw.s_waves)
        } else {
            visual?.setRepeat(false)
            visual?.fromRes(dev.ragnarok.fenrir_common.R.raw.s_waves_end)
        }
        visual?.startAnimation()
    }

    fun createGradientChatImage(width: Int, height: Int, owner_id: Long): Bitmap {
        val color1: String
        val color2: String
        when (owner_id % 10) {
            1L -> {
                color1 = "#00ABD6"
                color2 = "#8700D6"
            }

            2L -> {
                color1 = "#FF7900"
                color2 = "#FF9500"
            }

            3L -> {
                color1 = "#55D600"
                color2 = "#00D67A"
            }

            4L -> {
                color1 = "#9400D6"
                color2 = "#D6008E"
            }

            5L -> {
                color1 = "#cd8fff"
                color2 = "#9100ff"
            }

            6L -> {
                color1 = "#ff7f69"
                color2 = "#fe0bdb"
            }

            7L -> {
                color1 = "#FE790B"
                color2 = "#0BFEAB"
            }

            8L -> {
                color1 = "#9D0BFE"
                color2 = "#0BFEAB"
            }

            9L -> {
                color1 = "#9D0BFE"
                color2 = "#FEDF0B"
            }

            else -> {
                color1 = "#FF0061"
                color2 = "#FF4200"
            }
        }
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val gradient = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            color1.toColor(),
            color2.toColor(),
            Shader.TileMode.CLAMP
        )
        val canvas = Canvas(bitmap)
        val paint2 = Paint(Paint.ANTI_ALIAS_FLAG)
        paint2.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint2)
        return bitmap
    }

    fun getExoPlayerFactory(
        userAgent: String?,
        proxyConfig: ProxyConfig?
    ): OkHttpDataSource.Factory {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(Constants.EXO_PLAYER_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(Constants.EXO_PLAYER_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.EXO_PLAYER_TIMEOUT, TimeUnit.SECONDS)
            .callTimeout(Constants.EXO_PLAYER_TIMEOUT, TimeUnit.SECONDS)
        applyProxyConfig(builder, proxyConfig)
        return OkHttpDataSource.Factory(builder.build()).setUserAgent(userAgent)
    }

    fun isColorDark(color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    fun getAnimator(view: View?): Animator {
        return ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
    }

    fun writeStringMap(parcel: Parcel, map: Map<String, String>?) {
        if (map.isNullOrEmpty()) {
            parcel.writeInt(0)
            return
        }
        parcel.writeInt(map.size)
        for ((key, value) in map) {
            parcel.writeString(key)
            parcel.writeString(value)
        }
    }

    fun readStringMap(parcel: Parcel): Map<String, String>? {
        val size = parcel.readInt()
        if (size == 0) return null
        val map: MutableMap<String, String> = HashMap(size)
        for (i in 0 until size) {
            map[parcel.readString() ?: continue] = parcel.readString() ?: continue
        }
        return map
    }

    fun getArrayFromHash(data: Map<String, String>?): Array<Array<String?>> {
        data ?: return emptyArray()
        val str: Array<Array<String?>>
        val keys: Array<Any> = data.keys.toTypedArray()
        val values: Array<Any> = data.values.toTypedArray()
        str = Array(2) { arrayOfNulls(values.size) }
        for (i in keys.indices) {
            str[0][i] = keys[i] as String
            str[1][i] = values[i] as String
        }
        return str
    }

    fun getVerifiedColor(context: Context, verified: Boolean): Int {
        return if (!verified) CurrentTheme.getPrimaryTextColorCode(context) else "#009900".toColor()
    }

    fun dp(value: Float): Int {
        return if (value == 0f) {
            0
        } else ceil((density * value).toDouble())
            .toInt()
    }

    fun sp(value: Float): Int {
        return if (value == 0f) {
            0
        } else ceil((scaledDensity * value).toDouble())
            .toInt()
    }

    fun dpr(value: Float): Int {
        return if (value == 0f) {
            0
        } else (density * value).roundToInt()
    }

    fun dp2(value: Float): Int {
        return if (value == 0f) {
            0
        } else floor((density * value).toDouble())
            .toInt()
    }

    fun dpf2(value: Float): Float {
        return if (value == 0f) {
            0f
        } else density * value
    }

    fun prepareDensity(context: Context) {
        val metrics = context.resources.displayMetrics
        density = metrics.density
        scaledDensity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1F, metrics)
        } else {
            @Suppress("deprecation")
            metrics.scaledDensity
        }
        var display: Display? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display = context.display
        } else {
            val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
            if (manager != null) {
                @Suppress("deprecation")
                display = manager.defaultDisplay
            }
        }
        if (display != null) {
            val configuration = context.resources.configuration
            if (configuration.screenWidthDp != Configuration.SCREEN_WIDTH_DP_UNDEFINED) {
                val newSize = ceil((configuration.screenWidthDp * density).toDouble())
                    .toInt()
                if (abs(displaySize.x - newSize) > 3) {
                    displaySize.x = newSize
                }
            }
            if (configuration.screenHeightDp != Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
                val newSize = ceil((configuration.screenHeightDp * density).toDouble())
                    .toInt()
                if (abs(displaySize.y - newSize) > 3) {
                    displaySize.y = newSize
                }
            }
        }
    }

    inline fun <reified T> isValueAssigned(value: T, args: Array<T>): Boolean {
        return listOf(*args).contains(value)
    }

    inline fun <reified T> isValueAssigned(value: T, args: List<T>): Boolean {
        return args.contains(value)
    }

    inline fun <reified T> isOneElementAssigned(array: List<T>, args: Array<T>): Boolean {
        val temp = listOf(*args)
        for (i in array) {
            if (temp.contains(i)) {
                return true
            }
        }
        return false
    }

    inline fun <reified T> isOneElementAssigned(array: List<T>, args: List<T>): Boolean {
        for (i in array) {
            if (args.contains(i)) {
                return true
            }
        }
        return false
    }

    inline fun <reified T> stripEqualsWithCounter(
        list: List<T>,
        dataContainer: List<T>,
        count: Int
    ): List<T> {
        if (list.isEmpty()) {
            return emptyList()
        }
        val ret = ArrayList<T>(list)
        for ((counter, i) in (dataContainer.size - 1 downTo 0).withIndex()) {
            if (counter >= count) {
                break
            }
            if (ret.contains(dataContainer[i])) {
                ret.remove(dataContainer[i])
            }
        }
        return ret
    }

    inline fun safeCheck(obj: CharSequence?, crossinline function: () -> Boolean): Boolean {
        return if (obj.nonNullNoEmpty()) {
            function.invoke()
        } else false
    }

    fun clamp(value: Int, min: Int, max: Int): Int {
        if (value > max) {
            return max
        } else if (value < min) {
            return min
        }
        return value
    }

    fun clamp(value: Float, min: Float, max: Float): Float {
        if (value > max) {
            return max
        } else if (value < min) {
            return min
        }
        return value
    }

    fun createOkHttp(timeouts: Long, compressIntercept: Boolean): OkHttpClient.Builder {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .connectTimeout(timeouts, TimeUnit.SECONDS)
            .readTimeout(timeouts, TimeUnit.SECONDS)
            .writeTimeout(timeouts, TimeUnit.SECONDS)
            .callTimeout(timeouts, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                chain.proceed(
                    chain.toRequestBuilder(false).vkHeader(true).addHeader(
                        "User-Agent", UserAgentTool.USER_AGENT_CURRENT_ACCOUNT
                    ).build()
                )
            })
        if (compressIntercept) {
            builder.addInterceptor(UncompressDefaultInterceptor)
        }
        applyProxyConfig(builder, proxySettings.activeProxy)
        return builder
    }

    val isKateDefault: Boolean = Constants.DEFAULT_ACCOUNT_TYPE == AccountType.KATE

    val isOfficialDefault: Boolean = Constants.DEFAULT_ACCOUNT_TYPE == AccountType.VK_ANDROID

    /**
     * Returns the bitmap position inside an imageView.
     *
     * @param imageView source ImageView
     * @return 0: left, 1: top, 2: width, 3: height
     */

    fun getBitmapPositionInsideImageView(imageView: ImageView): IntArray {
        val ret = IntArray(4)
        if (imageView.drawable == null) return ret
        // Get image dimensions
        // Get image matrix values and place them in an array
        val f = FloatArray(9)
        imageView.imageMatrix.getValues(f)
        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        val scaleX = f[Matrix.MSCALE_X]
        val scaleY = f[Matrix.MSCALE_Y]
        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        val d = imageView.drawable
        val origW = d.intrinsicWidth
        val origH = d.intrinsicHeight
        // Calculate the actual dimensions
        val actW = (origW * scaleX).roundToInt()
        val actH = (origH * scaleY).roundToInt()
        ret[2] = actW
        ret[3] = actH
        // Get image position
        // We assume that the image is centered into ImageView
        val imgViewW = imageView.width
        val imgViewH = imageView.height
        val top = (imgViewH.toFloat() - actH).toInt() / 2
        val left = (imgViewW.toFloat() - actW).toInt() / 2
        ret[0] = left
        ret[1] = top
        return ret
    }

    fun makeMediaItem(url: String?): MediaItem {
        return MediaItem.Builder().setUri(url).build()
    }

    fun <T : RecyclerView.ViewHolder?> createAlertRecycleFrame(
        context: Context,
        adapter: RecyclerView.Adapter<T>,
        message: String?,
        accountId: Long
    ): View {
        val root = View.inflate(context, R.layout.alert_recycle_frame, null)
        val recyclerView: RecyclerView = root.findViewById(R.id.alert_recycle)
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        recyclerView.adapter = adapter
        val mMessage: EmojiconTextView = root.findViewById(R.id.alert_message)
        if (message.isNullOrEmpty()) {
            mMessage.visibility = View.GONE
        } else {
            mMessage.visibility = View.VISIBLE
            mMessage.text =
                OwnerLinkSpanFactory.withSpans(
                    message,
                    owners = true,
                    topics = false,
                    listener = object : LinkActionAdapter() {
                        override fun onOwnerClick(ownerId: Long) {
                            getOwnerWallPlace(accountId, ownerId, null).tryOpenWith(context)
                        }
                    })
        }
        return root
    }

    private fun getLocaleSettings(@Lang lang: Int): Locale {
        when (lang) {
            Lang.ENGLISH -> {
                Constants.DEVICE_COUNTRY_CODE = "en"
                return Locale.ENGLISH
            }

            Lang.RUSSIA -> {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    Locale.of("ru", "RU")
                } else {
                    @Suppress("deprecation")
                    Locale("ru", "RU")
                }
            }

            Lang.DEFAULT -> {}
        }
        Constants.DEVICE_COUNTRY_CODE = "ru"
        return Locale.getDefault()
    }

    val appLocale: Locale
        get() = getLocaleSettings(Settings.get().main().language)

    private fun getSystemLocale(config: Configuration): Locale? {
        return config.locales[0]
    }

    private fun setSystemLocaleLegacy(config: Configuration, locale: Locale) {
        config.setLocale(locale)
    }

    fun updateActivityContext(base: Context, isChatActivity: Boolean = false): Context {
        if (getSystemLocale(base.resources.configuration) != null && getSystemLocale(base.resources.configuration)?.language.nonNullNoEmpty()) {
            Constants.DEVICE_COUNTRY_CODE =
                getSystemLocale(base.resources.configuration)?.language?.lowercase(Locale.getDefault())
                    ?: "ru"
        } else {
            Constants.DEVICE_COUNTRY_CODE = "ru"
        }
        val size = Settings.get().main().fontSize
        @Lang val lang = Settings.get().main().language
        val locale = getLocaleSettings(lang)
        updateDateLang(locale)
        updateDateLang()
        return if (size == 0 || Settings.get().main().fontOnlyForChats && !isChatActivity) {
            if (lang == Lang.DEFAULT) {
                base
            } else {
                val res = base.resources
                val config = Configuration(res.configuration)
                setSystemLocaleLegacy(config, getLocaleSettings(lang))
                base.createConfigurationContext(config)
            }
        } else {
            val res = base.resources
            val config = Configuration(res.configuration)
            config.fontScale = res.configuration.fontScale + 0.05f * size
            if (lang != Lang.DEFAULT) {
                setSystemLocaleLegacy(config, getLocaleSettings(lang))
            }
            base.createConfigurationContext(config)
        }
    }

    fun checkValues(values: Collection<Boolean>): Boolean {
        for (i in values) {
            if (!i) {
                return false
            }
        }
        return true
    }

    fun checkEditInfo(info: String?, original: String?): String? {
        return if (info.isNullOrEmpty() || info == original) {
            null
        } else info
    }

    fun checkEditInfo(info: Int?, original: Int?): Int? {
        return if (info == null || info == original) {
            null
        } else info
    }

    fun createPageTransform(@Transformers_Types type: Int): ViewPager2.PageTransformer? {
        return when (type) {
            Transformers_Types.SLIDER_TRANSFORMER -> SliderTransformer(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT)
            Transformers_Types.CLOCK_SPIN_TRANSFORMER -> ClockSpinTransformer()
            Transformers_Types.BACKGROUND_TO_FOREGROUND_TRANSFORMER -> BackgroundToForegroundTransformer()
            Transformers_Types.CUBE_IN_DEPTH_TRANSFORMER -> CubeInDepthTransformer()
            Transformers_Types.DEPTH_TRANSFORMER -> DepthTransformer()
            Transformers_Types.FAN_TRANSFORMER -> FanTransformer()
            Transformers_Types.GATE_TRANSFORMER -> GateTransformer()
            Transformers_Types.OFF -> null
            Transformers_Types.ZOOM_OUT_TRANSFORMER -> ZoomOutTransformer()
            else -> null
        }
    }

    fun generateQR(url: String, context: Context): Bitmap? {
        return CustomQRCodeWriter().encode(
            url,
            768,
            768,
            ResourcesCompat.getDrawable(context.resources, R.drawable.for_qr, context.theme)
        )
    }

    fun makeMutablePendingIntent(flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && BuildConfig.TARGET_SDK >= Build.VERSION_CODES.S) {
            if (flags == 0) {
                PendingIntent.FLAG_MUTABLE
            } else {
                flags or PendingIntent.FLAG_MUTABLE
            }
        } else flags
    }

    fun makeImmutablePendingIntent(flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && BuildConfig.TARGET_SDK >= Build.VERSION_CODES.S) {
            if (flags == 0) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                flags or PendingIntent.FLAG_IMMUTABLE
            }
        } else flags
    }

    fun openPlaceWithSwipebleActivity(context: Context, place: Place) {
        val intent = Intent(context, SwipebleActivity::class.java)
        intent.action = MainActivity.ACTION_OPEN_PLACE
        intent.putExtra(Extra.PLACE, place)
        start(context, intent)
    }

    fun BytesToSize(Bytes: Long): String {
        val tb = 1099511627776L
        val gb: Long = 1073741824
        val mb: Long = 1048576
        val kb: Long = 1024
        return when {
            Bytes >= tb -> String.format(
                Locale.getDefault(),
                "%.2f TB",
                Bytes.toDouble() / tb
            )

            Bytes >= gb -> String.format(
                Locale.getDefault(),
                "%.2f GB",
                Bytes.toDouble() / gb
            )

            Bytes >= mb -> String.format(
                Locale.getDefault(),
                "%.2f MB",
                Bytes.toDouble() / mb
            )

            Bytes >= kb -> String.format(
                Locale.getDefault(),
                "%.2f KB",
                Bytes.toDouble() / kb
            )

            else -> String.format(Locale.getDefault(), "%d Bytes", Bytes)
        }
    }

    fun getSingleElementsLayoutManager(activity: Activity): RecyclerView.LayoutManager {
        if (activity.resources.configuration.orientation == ORIENTATION_LANDSCAPE) {
            return StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }
        return LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
    }
}