package dev.ragnarok.fenrir.fragment.createpin

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import java.util.Arrays

class CreatePinPresenter(savedInstanceState: Bundle?) :
    RxSupportPresenter<ICreatePinView>(savedInstanceState) {
    private val mCreatedPin: IntArray?
    private val mRepeatedPin: IntArray?
    private val mHandler = Handler(Looper.getMainLooper())
    private var mCurrentStep = 0
    private val mOnFullyEnteredRunnable = Runnable {
        if (mCurrentStep == STEP_CREATE) {
            onCreatedPinFullyEntered()
        } else {
            onRepeatedPinFullyEntered()
        }
    }

    fun fireBackButtonClick(): Boolean {
        if (mCurrentStep == STEP_CREATE) {
            return true
        }
        mCurrentStep = STEP_CREATE
        if (mCreatedPin != null) {
            resetPin(mCreatedPin)
        }
        if (mRepeatedPin != null) {
            resetPin(mRepeatedPin)
        }
        resolveAllViews()
        return false
    }

    fun fireFingerPrintClick() {
        view?.showError(R.string.not_yet_implemented_message)
    }

    fun fireBackspaceClick() {
        val targetPin = if (mCurrentStep == STEP_CREATE) mCreatedPin else mRepeatedPin
        val currentIndex = targetPin?.let { getNextNoEnteredIndex(it) }
        if (currentIndex == -1) {
            targetPin[targetPin.size - 1] = NO_VALUE
        } else if (currentIndex != null && currentIndex > 0) {
            targetPin[currentIndex - 1] = NO_VALUE
        }
        refreshViewCirclesVisibility()
    }

    private fun resolveAllViews() {
        refreshViewCirclesVisibility()
        resolveTitles()
    }

    private fun resolveTitles() {
        if (mCurrentStep == STEP_CREATE) {
            view?.displayTitle(R.string.create_pin_code_title)
        } else {
            view?.displayTitle(R.string.repeat_pin_code_title)
        }
    }

    fun fireDigitClick(digit: Int) {
        val targetPin = if (mCurrentStep == STEP_CREATE) mCreatedPin else mRepeatedPin
        if (targetPin?.let { isFullyEntered(it) } == true) {
            return
        }
        if (targetPin != null) {
            appendDigit(targetPin, digit)
        }
        refreshViewCirclesVisibility()
        if (targetPin?.let { isFullyEntered(it) } == true) {
            mHandler.removeCallbacks(mOnFullyEnteredRunnable)
            mHandler.postDelayed(mOnFullyEnteredRunnable, LAST_CIRCLE_VISIBILITY_DELAY.toLong())
        }
    }

    private fun appendDigit(pin: IntArray, digit: Int) {
        pin[getNextNoEnteredIndex(pin)] = digit
    }

    private fun onRepeatedPinFullyEntered() {
        if (!isPinsMatch) {
            if (mRepeatedPin != null) {
                resetPin(mRepeatedPin)
            }
            if (mCreatedPin != null) {
                resetPin(mCreatedPin)
            }
            mCurrentStep = STEP_CREATE
            view?.showError(R.string.entered_pin_codes_do_not_match)
            resolveAllViews()
            view?.displayErrorAnimation()
            return
        }
        if (mCreatedPin != null) {
            view?.sendSuccessAndClose(mCreatedPin)
        }
    }

    private fun onCreatedPinFullyEntered() {
        if (mRepeatedPin != null) {
            resetPin(mRepeatedPin)
        }
        mCurrentStep = STEP_REPEAT
        resolveAllViews()
    }

    private val isPinsMatch: Boolean
        get() {
            for (i in 0 until Constants.PIN_DIGITS_COUNT) {
                if (mCreatedPin?.get(i) == NO_VALUE || mRepeatedPin?.get(i) == NO_VALUE || mCreatedPin?.get(
                        i
                    ) != mRepeatedPin?.get(i)
                ) {
                    return false
                }
            }
            return true
        }

    private fun getNextNoEnteredIndex(pin: IntArray): Int {
        for (i in pin.indices) {
            if (pin[i] == NO_VALUE) {
                return i
            }
        }
        return -1
    }

    private fun isFullyEntered(pin: IntArray): Boolean {
        for (value in pin) {
            if (value == NO_VALUE) {
                return false
            }
        }
        return true
    }

    private fun refreshViewCirclesVisibility() {
        if (mCurrentStep == STEP_CREATE) {
            if (mCreatedPin != null) {
                view?.displayPin(
                    mCreatedPin,
                    NO_VALUE
                )
            }
        } else {
            if (mRepeatedPin != null) {
                view?.displayPin(
                    mRepeatedPin,
                    NO_VALUE
                )
            }
        }
    }

    override fun onGuiCreated(viewHost: ICreatePinView) {
        super.onGuiCreated(viewHost)
        resolveTitles()
        refreshViewCirclesVisibility()
    }

    private fun resetPin(pin: IntArray) {
        Arrays.fill(pin, NO_VALUE)
    }

    companion object {
        private const val LAST_CIRCLE_VISIBILITY_DELAY = 200
        private const val NO_VALUE = -1
        private const val SAVE_STEP = "save_step"
        private const val SAVE_CREATED_PIN = "save_created_pin"
        private const val SAVE_REPEATED_PIN = "save_repeated_pin"
        private const val STEP_CREATE = 1
        private const val STEP_REPEAT = 2
    }

    init {
        if (savedInstanceState != null) {
            mCurrentStep = savedInstanceState.getInt(SAVE_STEP)
            mCreatedPin = savedInstanceState.getIntArray(SAVE_CREATED_PIN)
            mRepeatedPin = savedInstanceState.getIntArray(SAVE_REPEATED_PIN)
        } else {
            mCurrentStep = STEP_CREATE
            mCreatedPin = IntArray(Constants.PIN_DIGITS_COUNT)
            mRepeatedPin = IntArray(Constants.PIN_DIGITS_COUNT)
            resetPin(mCreatedPin)
            resetPin(mRepeatedPin)
        }
    }
}