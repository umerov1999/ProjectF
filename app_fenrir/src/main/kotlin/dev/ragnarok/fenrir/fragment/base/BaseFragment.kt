package dev.ragnarok.fenrir.fragment.base

import androidx.fragment.app.Fragment
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import kotlinx.coroutines.Job

open class BaseFragment : Fragment() {
    private val mCompositeJob = CompositeJob()
    protected fun appendJob(job: Job) {
        mCompositeJob.add(job)
    }

    override fun onDestroy() {
        mCompositeJob.cancel()
        super.onDestroy()
    }
}
