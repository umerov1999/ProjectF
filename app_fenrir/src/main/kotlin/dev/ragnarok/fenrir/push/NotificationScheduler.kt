package dev.ragnarok.fenrir.push

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

object NotificationScheduler {
    val INSTANCE = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
}