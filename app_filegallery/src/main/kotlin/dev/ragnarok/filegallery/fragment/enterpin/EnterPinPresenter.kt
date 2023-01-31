package dev.ragnarok.filegallery.fragment.enterpin

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.fragment.base.RxSupportPresenter
import dev.ragnarok.filegallery.settings.Settings
import java.util.Arrays

class EnterPinPresenter(savedState: Bundle?) : RxSupportPresenter<IEnterPinView>(savedState) {
    private val mValues: IntArray?
    private val mHandler = Handler(Looper.getMainLooper())
    private val mOnFullyEnteredRunnable = Runnable { onFullyEntered() }
    private var onShowedFirstFingerPrint = false

    fun onFingerprintClicked() {
        if (!Settings.get().security().isEntranceByFingerprintAllowed) {
            view?.customToast?.showToastError(
                R.string.error_login_by_fingerprint_not_allowed
            )
            return
        }
        view?.showBiometricPrompt()
    }

    private val nextPinAttemptTimeout: Long
        get() {
            val history = Settings.get()
                .security()
                .pinEnterHistory
            if (history.size < Settings.get().security().pinHistoryDepthValue()) {
                return 0
            }
            val howLongAgoWasFirstAttempt = System.currentTimeMillis() - history[0]
            return if (howLongAgoWasFirstAttempt < MAX_ATTEMPT_DELAY) MAX_ATTEMPT_DELAY - howLongAgoWasFirstAttempt else 0
        }

    private fun refreshViewCirclesVisibility() {
        if (mValues != null) {
            view?.displayPin(
                mValues,
                NO_VALUE
            )
        }
    }

    fun onBackspaceClicked() {
        val currentIndex = currentIndex
        if (currentIndex == -1) {
            mValues?.set(mValues.size - 1, NO_VALUE)
        } else if (currentIndex > 0) {
            mValues?.set(currentIndex - 1, NO_VALUE)
        }
        refreshViewCirclesVisibility()
    }

    private fun onFullyEntered() {
        if (!isFullyEntered) return
        val timeout = nextPinAttemptTimeout
        if (timeout > 0) {
            view?.showError(
                R.string.limit_exceeded_number_of_attempts_message,
                timeout / 1000
            )
            resetPin()
            refreshViewCirclesVisibility()
            return
        }
        Settings.get()
            .security()
            .firePinAttemptNow()
        if (mValues?.let { Settings.get().security().isPinValid(it) } == true) {
            onEnteredRightPin()
        } else {
            onEnteredWrongPin()
        }
    }

    private fun onEnteredRightPin() {
        Settings.get()
            .security()
            .clearPinHistory()
        view?.sendSuccessAndClose()
    }

    private fun onEnteredWrongPin() {
        resetPin()
        refreshViewCirclesVisibility()
        view?.let {
            it.showError(R.string.pin_is_invalid_message)
            it.displayErrorAnimation()
        }
    }

    fun onNumberClicked(value: Int) {
        if (isFullyEntered) return
        mValues?.set(currentIndex, value)
        refreshViewCirclesVisibility()
        if (isFullyEntered) {
            mHandler.removeCallbacks(mOnFullyEnteredRunnable)
            mHandler.postDelayed(mOnFullyEnteredRunnable, LAST_CIRCLE_VISIBILITY_DELAY.toLong())
        }
    }

    private val isFullyEntered: Boolean
        get() {
            if (mValues != null) {
                for (value in mValues) {
                    if (value == NO_VALUE) {
                        return false
                    }
                }
            }
            return true
        }
    private val currentIndex: Int
        get() {
            mValues?.let {
                for (i in mValues.indices) {
                    if (mValues[i] == NO_VALUE) {
                        return i
                    }
                }
            }
            return -1
        }

    override fun onGuiCreated(viewHost: IEnterPinView) {
        super.onGuiCreated(viewHost)
        if (!onShowedFirstFingerPrint) {
            onShowedFirstFingerPrint = true
            if (Settings.get().security().isEntranceByFingerprintAllowed) {
                viewHost.showBiometricPrompt()
            }
        }
        refreshViewCirclesVisibility()
    }

    private fun resetPin() {
        if (mValues != null) {
            Arrays.fill(mValues, NO_VALUE)
        }
    }

    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putIntArray(SAVE_VALUE, mValues)
    }

    companion object {
        private const val SAVE_VALUE = "save_value"
        private const val LAST_CIRCLE_VISIBILITY_DELAY = 200
        private const val NO_VALUE = -1
        private const val MAX_ATTEMPT_DELAY = 3 * 60 * 1000
    }

    init {
        if (savedState != null) {
            mValues = savedState.getIntArray(SAVE_VALUE)
        } else {
            mValues = IntArray(Constants.PIN_DIGITS_COUNT)
            resetPin()
        }
    }
}