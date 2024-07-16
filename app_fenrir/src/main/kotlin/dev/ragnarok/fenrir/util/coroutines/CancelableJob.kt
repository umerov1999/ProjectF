package dev.ragnarok.fenrir.util.coroutines

import dev.ragnarok.fenrir.Constants
import kotlinx.coroutines.Job
import java.util.concurrent.CancellationException

class CancelableJob {
    private var job: Job? = null
    fun set(job: Job) {
        synchronized(this) {
            this.job?.cancel(if (Constants.IS_DEBUG) CancellationException("Canceling job!") else null)
            this.job = job
        }
    }

    fun cancel() {
        if (job != null) {
            synchronized(this) {
                this.job?.cancel(if (Constants.IS_DEBUG) CancellationException("Canceling job!") else null)
                this.job = null
            }
        }
    }

    operator fun plusAssign(job: Job) {
        set(job)
    }
}