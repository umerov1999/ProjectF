package dev.ragnarok.fenrir.util.coroutines

import android.os.Handler
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.service.ErrorLocalizer
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

object CoroutinesUtils {
    val coroutineExceptionHandlerWithToast = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        val ctx = Includes.provideApplicationContext()
        Handler(ctx.mainLooper).post {
            if (Settings.get().main().isDeveloper_mode) {
                createCustomToast(ctx).showToastError(
                    ErrorLocalizer.localizeThrowable(
                        ctx,
                        throwable
                    )
                )
            }
        }
    }

    val coroutineExceptionHandlerEmpty = CoroutineExceptionHandler { _, throwable ->
        if (Constants.IS_DEBUG) {
            throwable.printStackTrace()
        }
    }

    inline fun <reified T> Flow<T>.fromIOToMain(
        crossinline onSuccess: (result: T) -> Unit,
        crossinline onError: (exception: Throwable) -> Unit
    ): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            catch {
                if (isActive) {
                    launch(Dispatchers.Main) {
                        onError(it)
                    }
                }
            }.collect {
                if (isActive) {
                    launch(Dispatchers.Main) {
                        onSuccess(it)
                    }
                }
            }
        }
    }

    inline fun <reified T> Flow<T>.fromIOToMain(
        crossinline onSuccess: (result: T) -> Unit,
    ): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            catch {
                if (Constants.IS_DEBUG) {
                    it.printStackTrace()
                }
            }.collect {
                if (isActive) {
                    launch(Dispatchers.Main) {
                        onSuccess(it)
                    }
                }
            }
        }
    }

    inline fun <reified T> Flow<T>.fromIO(crossinline onSuccess: (result: T) -> Unit): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            catch {
                if (Constants.IS_DEBUG) {
                    it.printStackTrace()
                }
            }.collect {
                onSuccess(it)
            }
        }
    }

    inline fun <reified T> Flow<T>.hiddenIO(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            catch {
                if (Constants.IS_DEBUG) {
                    it.printStackTrace()
                }
            }.collect {}
        }
    }

    inline fun <reified T> Flow<T>.toMain(
        crossinline onSuccess: (result: T) -> Unit
    ): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            catch {
                if (Constants.IS_DEBUG) {
                    it.printStackTrace()
                }
            }.collect {
                onSuccess(it)
            }
        }
    }

    inline fun inMainThread(crossinline function: () -> Unit): Job {
        return CoroutineScope(Dispatchers.Main + coroutineExceptionHandlerEmpty).launch {
            function.invoke()
        }
    }

    suspend inline fun isActive(): Boolean = currentCoroutineContext().isActive

    inline fun <reified T> emptyListFlow(): Flow<List<T>> {
        return flow {
            emit(emptyList())
        }
    }

    inline fun <reified T, reified R> emptyMapFlow(): Flow<Map<T, R>> {
        return flow {
            emit(emptyMap())
        }
    }

    fun emptyTaskFlow(): Flow<Boolean> {
        return flow {
            emit(false)
        }
    }

    fun delayTaskFlow(timeMillis: Long): Flow<Boolean> {
        return flow {
            delay(timeMillis)
            emit(true)
        }
    }

    inline fun <reified T> Flow<T>.delayedFlow(timeMillis: Long): Flow<T> {
        return map {
            delay(timeMillis)
            it
        }
    }

    inline fun <reified T> Flow<T>.timeOutFlow(timeMillis: Long): Flow<T> {
        return map {
            withTimeout(timeMillis) {
                it
            }
        }
    }

    inline fun <reified T, reified R> Flow<T>.andThen(flow: Flow<R>): Flow<R> {
        return flatMapConcat {
            flow
        }
    }

    inline fun <reified T> dummy(): (T) -> Unit = {}
    fun ignore(): (Throwable) -> Unit = {
        if (Constants.IS_DEBUG) {
            it.printStackTrace()
        }
    }

    fun Flow<Int>.checkInt(): Flow<Boolean> {
        return map { it == 1 }
    }

    fun Flow<Int>.checkIntOverZero(): Flow<Boolean> {
        return map { it > 0 }
    }

    inline fun <reified T> Flow<T>.ignoreElement(): Flow<Boolean> {
        return map { true }
    }

    inline fun <reified T> Flow<T>.repeatUntil(
        crossinline needRepeat: () -> Boolean,
        delayMS: Long
    ): Flow<Boolean> {
        return map {
            while (needRepeat() && isActive()) {
                single()
                if (delayMS > 0) {
                    delay(delayMS)
                }
            }
            true
        }
    }

    inline fun <reified T> toFlow(p: T): Flow<T> {
        return flow {
            emit(p)
        }
    }

    inline fun <reified T> toFlowThrowable(p: Throwable): Flow<T> {
        return flow {
            throw p
        }
    }

    inline fun <reified T> List<Flow<T>>.mergeFlows(): Flow<Boolean> {
        return flow {
            for (i in this@mergeFlows) {
                i.single()
            }
            emit(true)
        }
    }

    inline fun <reified T> Flow<T>.fromScopeToMain(
        scope: CoroutineScope,
        crossinline onSuccess: (result: T) -> Unit,
        crossinline onError: (exception: Throwable) -> Unit
    ): Job {
        return scope.launch {
            catch {
                if (isActive) {
                    launch(Dispatchers.Main) {
                        onError(it)
                    }
                }
            }.collect {
                if (isActive) {
                    launch(Dispatchers.Main) {
                        onSuccess(it)
                    }
                }
            }
        }
    }

    inline fun <reified T> Flow<T>.fromScopeToMain(
        scope: CoroutineScope,
        crossinline onSuccess: (result: T) -> Unit,
    ): Job {
        return scope.launch {
            catch {
                if (Constants.IS_DEBUG) {
                    it.printStackTrace()
                }
            }.collect {
                if (isActive) {
                    launch(Dispatchers.Main) {
                        onSuccess(it)
                    }
                }
            }
        }
    }

    inline fun <reified T> Flow<T>.syncSingle(): T {
        return runBlocking(Dispatchers.IO) {
            single()
        }
    }

    inline fun <reified T> Flow<T>.syncSingleSafe(): T? {
        try {
            return runBlocking(Dispatchers.IO) {
                single()
            }
        } catch (e: Exception) {
            if (Constants.IS_DEBUG) {
                e.printStackTrace()
            }
            return null
        }
    }

    inline fun <reified T> Flow<T>.syncSingleSafe(default: T): T {
        return try {
            runBlocking(Dispatchers.IO) {
                single()
            }
        } catch (e: Exception) {
            if (Constants.IS_DEBUG) {
                e.printStackTrace()
            }
            default
        }
    }

    inline fun <reified T> Flow<T>.sharedFlowToMain(
        crossinline onSuccess: (result: T) -> Unit
    ): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            collect {
                if (isActive) {
                    launch(Dispatchers.Main) {
                        onSuccess(it)
                    }
                }
            }
        }
    }

    inline fun <reified T> MutableSharedFlow<T>.myEmit(value: T): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            emit(value)
        }
    }

    inline fun <reified T> createPublishSubject(): MutableSharedFlow<T> {
        return MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 0, BufferOverflow.SUSPEND
        )
    }
}
