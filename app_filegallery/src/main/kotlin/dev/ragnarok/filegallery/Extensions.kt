package dev.ragnarok.filegallery

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.database.getBlobOrNull
import androidx.core.database.getStringOrNull
import androidx.core.graphics.toColorInt
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import java.io.Serializable
import kotlin.contracts.contract

val kJson: Json by lazy { Json { ignoreUnknownKeys = true; isLenient = true } }
val kJsonPretty: Json by lazy {
    Json {
        ignoreUnknownKeys = true; isLenient = true; prettyPrint = true
    }
}
val kJsonNotPretty: Json by lazy {
    Json {
        ignoreUnknownKeys = true; isLenient = true; prettyPrint = false
    }
}

fun SQLiteDatabase.query(
    tableName: String,
    columns: Array<String>,
    where: String?,
    args: Array<String>?
): Cursor = query(tableName, columns, where, args, null, null, null)

fun SQLiteDatabase.query(tableName: String, columns: Array<String>): Cursor =
    query(tableName, columns, null, null)

fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))

fun Cursor.getBoolean(columnName: String): Boolean =
    getInt(getColumnIndexOrThrow(columnName)) == 1

fun Cursor.getString(columnName: String): String? =
    getStringOrNull(getColumnIndexOrThrow(columnName))

fun Cursor.getBlob(columnName: String): ByteArray? =
    getBlobOrNull(getColumnIndexOrThrow(columnName))

inline fun <reified T> Collection<T?>?.safeAllIsNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@safeAllIsNullOrEmpty != null)
    }
    if (this == null) {
        return true
    }
    for (obj in this) {
        if (obj is CharSequence) {
            return obj.isEmpty()
        } else if (obj is Collection<*>) {
            return obj.isEmpty()
        }
        if (obj != null) {
            return false
        }
    }
    return true
}

inline fun <reified T> Collection<T>?.nonNullNoEmpty(): Boolean {
    contract {
        returns(true) implies (this@nonNullNoEmpty != null)
    }
    return !this.isNullOrEmpty()
}

inline fun <reified T : CharSequence> T?.nonNullNoEmpty(): Boolean {
    contract {
        returns(true) implies (this@nonNullNoEmpty != null)
    }
    return !this.isNullOrEmpty()
}

inline fun <reified T : CharSequence> T?.ifNonNullNoEmpty(yes: (T) -> Unit, no: () -> Unit) {
    if (this.nonNullNoEmpty()) yes.invoke(this) else no.invoke()
}

inline fun <reified T : CharSequence> T?.nonNullNoEmpty(yes: (T) -> T, no: () -> T): T {
    return if (this.nonNullNoEmpty()) yes.invoke(this) else no.invoke()
}

inline fun <reified T : Any, reified E : Collection<T>> E?.ifNonNullNoEmpty(
    yes: (E) -> Unit,
    no: () -> Unit
) {
    if (this.nonNullNoEmpty()) yes.invoke(this) else no.invoke()
}

inline fun <reified K, reified V> Map<out K, V>?.nonNullNoEmpty(): Boolean {
    contract {
        returns(true) implies (this@nonNullNoEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

fun IntArray?.nonNullNoEmpty(): Boolean {
    contract {
        returns(true) implies (this@nonNullNoEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

inline fun <reified T> Array<T>?.nonNullNoEmpty(): Boolean {
    contract {
        returns(true) implies (this@nonNullNoEmpty != null)
    }
    return !this.isNullOrEmpty()
}

fun ByteArray?.nonNullNoEmpty(): Boolean {
    contract {
        returns(true) implies (this@nonNullNoEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

fun LongArray?.nonNullNoEmpty(): Boolean {
    contract {
        returns(true) implies (this@nonNullNoEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

fun FloatArray?.nonNullNoEmpty(): Boolean {
    contract {
        returns(true) implies (this@nonNullNoEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

fun DoubleArray?.nonNullNoEmpty(): Boolean {
    contract {
        returns(true) implies (this@nonNullNoEmpty != null)
    }
    return this != null && this.isNotEmpty()
}

fun IntArray?.nullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@nullOrEmpty != null)
    }
    return this == null || this.isEmpty()
}

inline fun <reified T> Array<T>?.nullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@nullOrEmpty != null)
    }
    return this.isNullOrEmpty()
}

fun ByteArray?.nullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@nullOrEmpty != null)
    }
    return this == null || this.isEmpty()
}

fun LongArray?.nullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@nullOrEmpty != null)
    }
    return this == null || this.isEmpty()
}

fun FloatArray?.nullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@nullOrEmpty != null)
    }
    return this == null || this.isEmpty()
}

fun DoubleArray?.nullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@nullOrEmpty != null)
    }
    return this == null || this.isEmpty()
}

inline fun IntArray?.ifNonNullNoEmpty(yes: (IntArray) -> Unit, no: () -> Unit) {
    if (this.nonNullNoEmpty()) yes.invoke(this) else no.invoke()
}

inline fun <reified T> Array<T>?.ifNonNullNoEmpty(yes: (Array<T>) -> Unit, no: () -> Unit) {
    if (this.nonNullNoEmpty()) yes.invoke(this) else no.invoke()
}

inline fun ByteArray?.ifNonNullNoEmpty(yes: (ByteArray) -> Unit, no: () -> Unit) {
    if (this.nonNullNoEmpty()) yes.invoke(this) else no.invoke()
}

inline fun LongArray?.ifNonNullNoEmpty(yes: (LongArray) -> Unit, no: () -> Unit) {
    if (this.nonNullNoEmpty()) yes.invoke(this) else no.invoke()
}

inline fun FloatArray?.ifNonNullNoEmpty(yes: (FloatArray) -> Unit, no: () -> Unit) {
    if (this.nonNullNoEmpty()) yes.invoke(this) else no.invoke()
}

inline fun DoubleArray?.ifNonNullNoEmpty(yes: (DoubleArray) -> Unit, no: () -> Unit) {
    if (this.nonNullNoEmpty()) yes.invoke(this) else no.invoke()
}

inline fun <reified T : CharSequence> T?.trimmedIsNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@trimmedIsNullOrEmpty != null)
    }
    return this == null || this.trim().isEmpty()
}

inline fun <reified T : CharSequence> T?.trimmedNonNullNoEmpty(): Boolean {
    contract {
        returns(true) implies (this@trimmedNonNullNoEmpty != null)
    }
    return this != null && this.trim().isNotEmpty()
}

inline fun <reified T : CharSequence> T?.nonNullNoEmpty(block: (T) -> Unit) {
    if (!isNullOrEmpty()) apply(block)
}

inline fun ByteArray?.nonNullNoEmpty(block: (ByteArray) -> Unit) {
    if (this != null && this.isNotEmpty()) apply(block)
}

inline fun IntArray?.nonNullNoEmpty(block: (IntArray) -> Unit) {
    if (this != null && this.isNotEmpty()) apply(block)
}

inline fun <reified T> Array<T>?.nonNullNoEmpty(block: (Array<T>) -> Unit) {
    if (!this.isNullOrEmpty()) apply(block)
}

inline fun DoubleArray?.nonNullNoEmpty(block: (DoubleArray) -> Unit) {
    if (this != null && this.isNotEmpty()) apply(block)
}

inline fun FloatArray?.nonNullNoEmpty(block: (FloatArray) -> Unit) {
    if (this != null && this.isNotEmpty()) apply(block)
}

inline fun LongArray?.nonNullNoEmpty(block: (LongArray) -> Unit) {
    if (this != null && this.isNotEmpty()) apply(block)
}

inline fun <reified T, reified E : Collection<T>> E?.nonNullNoEmpty(block: (E) -> Unit) {
    if (!isNullOrEmpty()) apply(block)
}

inline fun <reified T, reified E : Collection<*>> E?.nonNullNoEmptyOrNullable(yes: (E) -> T): T? {
    return if (!isNullOrEmpty()) yes.invoke(this) else null
}

inline fun <reified T, reified E : Collection<*>> E?.nonNullNoEmptyOr(
    yes: (E) -> T,
    no: () -> T
): T {
    return if (!isNullOrEmpty()) yes.invoke(this) else no.invoke()
}

inline fun <reified T : CharSequence> T?.trimmedNonNullNoEmpty(block: (T) -> Unit) {
    this?.let {
        if (trim().isNotEmpty()) {
            apply(block)
        }
    }
}

inline fun <reified T : Any> T?.requireNonNull(block: (T) -> Unit) {
    this?.apply(block)
}

inline fun <reified T : Any, reified E : Any> T?.requireNonNull(yes: (T) -> E, no: () -> E): E {
    return this?.let { yes.invoke(it) } ?: no.invoke()
}

inline fun <reified T : Any> T?.ifNonNull(yes: (T) -> Unit, no: () -> Unit) {
    this?.let { yes.invoke(it) } ?: no.invoke()
}

inline fun <reified T : Any, reified E : Any> T?.transformNonNullNullable(
    yes: (T) -> E,
    no: () -> E?
): E? {
    return this?.let { yes.invoke(it) } ?: no.invoke()
}

fun Int?.orZero(): Int {
    return this ?: 0
}

fun Byte?.orZero(): Byte {
    return this ?: 0
}

fun Long?.orZero(): Long {
    return this ?: 0
}

inline fun <reified T, reified E : List<T>> E?.requireNonNull(block: (E) -> Unit) {
    this?.apply(block)
}

inline fun <reified T, reified E : MutableList<T>> E?.requireNonNullMutable(block: (E) -> Unit) {
    this?.apply(block)
}

inline fun View.fadeOut(duration: Long, crossinline onEnd: () -> Unit = {}) {
    ObjectAnimator.ofPropertyValuesHolder(
        this,
        PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f)
    ).apply {
        this.duration = duration
        addListener(object : StubAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd()
            }

            override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                onEnd()
            }
        })
        start()
    }
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

inline fun View.fadeIn(duration: Long, crossinline onEnd: () -> Unit = {}) {
    ObjectAnimator.ofPropertyValuesHolder(
        this,
        PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)
    ).apply {
        this.duration = duration
        addListener(object : StubAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd()
            }

            override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {
                onEnd()
            }
        })
        start()
    }
}

fun <T> MutableList<T>.insert(index: Int, element: T) {
    if (index <= size) {
        add(index, element)
    } else {
        add(element)
    }
}

fun <T> MutableList<T>.insertAfter(index: Int, element: T) {
    val cur = index + 1
    if (cur <= size) {
        add(cur, element)
    } else {
        add(element)
    }
}

open class StubAnimatorListener : Animator.AnimatorListener {
    override fun onAnimationRepeat(animation: Animator) {}

    override fun onAnimationEnd(animation: Animator, isReverse: Boolean) {}

    override fun onAnimationEnd(animation: Animator) {}

    override fun onAnimationCancel(animation: Animator) {}

    override fun onAnimationStart(animation: Animator) {}
}

fun ResponseBody.isMsgPack(): Boolean {
    return contentType()?.toString()?.contains("msgpack") == true
}

inline fun <reified T : Parcelable> Parcel.readTypedObjectCompat(c: Parcelable.Creator<T>): T? {
    return if (readInt() != 0) {
        c.createFromParcel(this)
    } else {
        null
    }
}

inline fun <reified T : Parcelable> Parcel.writeTypedObjectCompat(
    parcel: T?,
    parcelableFlags: Int
) {
    if (parcel != null) {
        writeInt(1)
        parcel.writeToParcel(this, parcelableFlags)
    } else {
        writeInt(0)
    }
}

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        @Suppress("deprecation")
        getParcelable(key)
    }
}

inline fun <reified T : Serializable> Bundle.getSerializableCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, T::class.java)
    } else {
        @Suppress("deprecation")
        getSerializable(key) as T
    }
}

inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String): ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(key, T::class.java)
    } else {
        @Suppress("deprecation")
        getParcelableArrayList(key)
    }
}

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("deprecation")
        getParcelableExtra(key)
    }
}

inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraCompat(key: String): ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(key, T::class.java)
    } else {
        @Suppress("deprecation")
        getParcelableArrayListExtra(key)
    }
}

fun Parcel.putBoolean(value: Boolean) {
    writeByte(if (value) 1 else 0)
}

fun Parcel.getBoolean(): Boolean {
    return readByte() != 0.toByte()
}

inline fun <reified T> MutableList<T>.swap(index1: Int, index2: Int) {
    val tmp = this[index1]
    this[index1] = this[index2]
    this[index2] = tmp
}

@ColorInt
fun String.toColor(): Int {
    try {
        return this.toColorInt()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return Color.RED
}
