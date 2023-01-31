package dev.ragnarok.fenrir.fragment.createpin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.annotation.StringRes
import com.google.android.material.materialswitch.MaterialSwitch
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.view.KeyboardView
import dev.ragnarok.fenrir.view.KeyboardView.OnKeyboardClickListener

class CreatePinFragment : BaseMvpFragment<CreatePinPresenter, ICreatePinView>(), ICreatePinView,
    OnKeyboardClickListener, BackPressCallback {
    private var mTitle: TextView? = null
    private var mValuesRoot: View? = null
    private var mAllowFingerprint: MaterialSwitch? = null
    private var mPinCodeOnStart: MaterialSwitch? = null
    private var mDontAskEveryTime: MaterialSwitch? = null
    private var mValuesCircles: Array<View?> = arrayOfNulls(Constants.PIN_DIGITS_COUNT)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_create_pin, container, false)
        val keyboardView: KeyboardView = root.findViewById(R.id.keyboard)
        keyboardView.setOnKeyboardClickListener(this)
        mTitle = root.findViewById(R.id.pin_title_text)
        mValuesRoot = root.findViewById(R.id.value_root)
        mValuesCircles = arrayOfNulls(Constants.PIN_DIGITS_COUNT)
        mValuesCircles[0] = root.findViewById(R.id.pincode_digit_0)
        mValuesCircles[1] = root.findViewById(R.id.pincode_digit_1)
        mValuesCircles[2] = root.findViewById(R.id.pincode_digit_2)
        mValuesCircles[3] = root.findViewById(R.id.pincode_digit_3)

        mAllowFingerprint = root.findViewById(R.id.fingerprint_sw)
        mPinCodeOnStart = root.findViewById(R.id.pin_entrance_sw)
        mDontAskEveryTime = root.findViewById(R.id.pin_delayed_pin_for_entrance_sw)

        mAllowFingerprint?.setOnCheckedChangeListener { _, isChecked ->
            Settings.get().security().isEntranceByFingerprintAllowed = isChecked
        }

        mPinCodeOnStart?.setOnCheckedChangeListener { _, isChecked ->
            Settings.get().security().isUsePinForEntrance = isChecked
        }

        mDontAskEveryTime?.setOnCheckedChangeListener { _, isChecked ->
            Settings.get().security().isDelayedAllow = isChecked
        }

        if (requireActivity().intent?.extras?.containsKey(
                EXTRA_PREF_KEY
            ) != true
        ) {
            mAllowFingerprint?.visibility = View.VISIBLE
            mPinCodeOnStart?.visibility = View.VISIBLE
            mDontAskEveryTime?.visibility = View.VISIBLE
            Settings.get().security().isUsePinForEntrance = false
            Settings.get().security().isEntranceByFingerprintAllowed = false
            Settings.get().security().isDelayedAllow = false
        } else {
            mAllowFingerprint?.visibility = View.GONE
            mPinCodeOnStart?.visibility = View.GONE
            mDontAskEveryTime?.visibility = View.GONE
        }
        return root
    }

    override fun displayTitle(@StringRes titleRes: Int) {
        mTitle?.setText(titleRes)
    }

    override fun displayErrorAnimation() {
        if (mValuesRoot != null) {
            val animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.anim_invalid_pin)
            mValuesRoot?.startAnimation(animation)
        }
    }

    override fun displayPin(value: IntArray, noValue: Int) {
        check(value.size == mValuesCircles.size) { "Invalid pin length, view: " + mValuesCircles.size + ", target: " + value.size }
        for (i in mValuesCircles.indices) {
            val visible = value[i] != noValue
            mValuesCircles[i]?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun sendSuccessAndClose(values: IntArray) {
        val data = Intent()
        data.putExtra(EXTRA_PIN_VALUE, values)
        if (requireActivity().intent?.extras?.containsKey(
                EXTRA_PREF_KEY
            ) == true
        ) {
            data.putExtra(
                EXTRA_PREF_SCREEN, requireActivity().intent.extras?.getString(EXTRA_PREF_SCREEN)
            )
            data.putExtra(
                EXTRA_PREF_KEY, requireActivity().intent.extras?.getString(EXTRA_PREF_KEY)
            )
        }
        requireActivity().setResult(Activity.RESULT_OK, data)
        requireActivity().finish()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<CreatePinPresenter> {
        return object : IPresenterFactory<CreatePinPresenter> {
            override fun create(): CreatePinPresenter {
                return CreatePinPresenter(saveInstanceState)
            }
        }
    }

    override fun onButtonClick(number: Int) {
        presenter?.fireDigitClick(
            number
        )
    }

    override fun onBackspaceClick() {
        presenter?.fireBackspaceClick()
    }

    override fun onFingerPrintClick() {
        presenter?.fireFingerPrintClick()
    }

    override fun onBackPressed(): Boolean {
        return presenter?.fireBackButtonClick() ?: false
    }

    companion object {
        const val EXTRA_PREF_SCREEN = "pref_screen"
        const val EXTRA_PREF_KEY = "pref_key"
        private const val EXTRA_PIN_VALUE = "pin_value"
        fun newInstance(): CreatePinFragment {
            return CreatePinFragment()
        }

        fun extractValueFromIntent(intent: Intent?): IntArray? {
            return intent?.getIntArrayExtra(EXTRA_PIN_VALUE)
        }
    }
}