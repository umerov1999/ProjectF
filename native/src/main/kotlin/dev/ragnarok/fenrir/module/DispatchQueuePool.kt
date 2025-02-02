package dev.ragnarok.fenrir.module

import android.os.SystemClock
import androidx.annotation.UiThread
import java.security.SecureRandom
import java.util.LinkedList

class DispatchQueuePool(private val maxCount: Int) {
    private val queues = LinkedList<DispatchQueue>()
    private val busyQueuesMap = HashMap<DispatchQueue, Int>()
    private val busyQueues = LinkedList<DispatchQueue>()
    private val guid: Int = random.nextInt()
    private var createdCount = 0
    private var totalTasksCount = 0
    private var cleanupScheduled = false
    private val cleanupRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!queues.isEmpty()) {
                val currentTime = SystemClock.elapsedRealtime()
                var a = 0
                var m = queues.size
                while (a < m) {
                    val queue = queues[a]
                    if (queue.lastTaskTime < currentTime - 30000) {
                        queue.recycle()
                        queues.removeAt(a)
                        createdCount--
                        a--
                        m--
                    }
                    a++
                }
            }
            cleanupScheduled = if (!queues.isEmpty() || !busyQueues.isEmpty()) {
                DispatchQueue.runOnUIThread(this, 30000)
                true
            } else {
                false
            }
        }
    }

    @UiThread
    fun execute(runnable: Runnable) {
        val queue: DispatchQueue
        if (!busyQueues.isEmpty() && (totalTasksCount / 2 <= busyQueues.size || queues.isEmpty() && createdCount >= maxCount)) {
            queue = busyQueues.removeAt(0)
        } else if (queues.isEmpty()) {
            queue = DispatchQueue("DispatchQueuePool" + guid + "_" + random.nextInt())
            queue.priority = Thread.MAX_PRIORITY
            createdCount++
        } else {
            queue = queues.removeAt(0)
        }
        if (!cleanupScheduled) {
            DispatchQueue.runOnUIThread(cleanupRunnable, 30000)
            cleanupScheduled = true
        }
        totalTasksCount++
        busyQueues.add(queue)
        var count = busyQueuesMap[queue]
        if (count == null) {
            count = 0
        }
        busyQueuesMap[queue] = count + 1
        queue.postRunnable {
            runnable.run()
            DispatchQueue.runOnUIThread({
                totalTasksCount--
                var remainingTasksCount = busyQueuesMap[queue]
                if (remainingTasksCount == null) {
                    remainingTasksCount = 0
                } else {
                    remainingTasksCount--
                }
                if (remainingTasksCount == 0) {
                    busyQueuesMap.remove(queue)
                    busyQueues.remove(queue)
                    queues.add(queue)
                } else {
                    busyQueuesMap[queue] = remainingTasksCount
                }
            })
        }
    }

    companion object {
        private val random = SecureRandom()
    }

}