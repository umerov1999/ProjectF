package dev.ragnarok.fenrir.view.steppers.base

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import dev.ragnarok.fenrir.view.steppers.base.AbsStepsHost.AbsState

abstract class AbsStepsHost<T : AbsState>(var state: T) {
    var currentStep = 0
    abstract val stepsCount: Int

    abstract fun readParcelState(saveInstanceState: Bundle, key: String): T?

    @StringRes
    abstract fun getStepTitle(index: Int): Int
    abstract fun canMoveNext(index: Int, state: T): Boolean
    fun canMoveNext(index: Int): Boolean {
        return canMoveNext(index, state)
    }

    @StringRes
    abstract fun getNextButtonText(index: Int): Int

    @StringRes
    abstract fun getCancelButtonText(index: Int): Int
    fun saveState(bundle: Bundle) {
        bundle.putInt("host_current", currentStep)
        bundle.putParcelable("host_state", state)
    }

    fun restoreState(saveInstanceState: Bundle) {
        currentStep = saveInstanceState.getInt("host_current")
        readParcelState(saveInstanceState, "host_state")?.let {
            state = it
        }
    }

    abstract class AbsState : Parcelable {
        fun reset() {}
        override fun describeContents(): Int {
            return 0
        }
    }
}