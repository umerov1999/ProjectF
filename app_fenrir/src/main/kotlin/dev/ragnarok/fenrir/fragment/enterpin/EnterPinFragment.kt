package dev.ragnarok.fenrir.fragment.enterpin

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.view.KeyboardView
import dev.ragnarok.fenrir.view.KeyboardView.OnKeyboardClickListener

class EnterPinFragment : BaseMvpFragment<EnterPinPresenter, IEnterPinView>(), IEnterPinView,
    OnKeyboardClickListener {
    private var mAvatar: ImageView? = null
    private var mValuesRoot: View? = null
    private var mValuesCircles: Array<View?> = arrayOfNulls(Constants.PIN_DIGITS_COUNT)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_enter_pin, container, false)
        val keyboardView: KeyboardView = root.findViewById(R.id.keyboard)
        keyboardView.setOnKeyboardClickListener(this)
        mAvatar = root.findViewById(R.id.avatar)
        mValuesRoot = root.findViewById(R.id.value_root)
        mValuesCircles[0] = root.findViewById(R.id.pincode_digit_0)
        mValuesCircles[1] = root.findViewById(R.id.pincode_digit_1)
        mValuesCircles[2] = root.findViewById(R.id.pincode_digit_2)
        mValuesCircles[3] = root.findViewById(R.id.pincode_digit_3)
        return root
    }

    private val authenticationCallback: BiometricPrompt.AuthenticationCallback
        get() = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                sendSuccessAndClose()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                customToast.showToastError(errString.toString())
            }
        }

    override fun showBiometricPrompt() {
        if (BiometricManager.from(requireActivity())
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            customToast.showToastError(R.string.biometric_not_support)
            return
        }
        val authenticationCallback = authenticationCallback
        val mBiometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireActivity()),
            authenticationCallback
        )
        val promptInfo = PromptInfo.Builder()
            .setTitle(getString(R.string.biometric))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()
        mBiometricPrompt.authenticate(promptInfo)
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<EnterPinPresenter> {
        return object : IPresenterFactory<EnterPinPresenter> {
            override fun create(): EnterPinPresenter {
                return EnterPinPresenter(saveInstanceState)
            }
        }
    }

    override fun displayPin(value: IntArray, noValue: Int) {
        check(value.size == mValuesCircles.size) { "Invalid pin length, view: " + mValuesCircles.size + ", target: " + value.size }
        for (i in mValuesCircles.indices) {
            val visible = value[i] != noValue
            mValuesCircles[i]?.visibility =
                if (visible) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun sendSuccessAndClose() {
        if (isAdded) {
            Settings.get().security().updateLastPinTime()
            requireActivity().setResult(Activity.RESULT_OK)
            requireActivity().finish()
        }
    }

    override fun displayErrorAnimation() {
        if (mValuesRoot != null) {
            val animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.anim_invalid_pin)
            mValuesRoot?.startAnimation(animation)
        }
    }

    override fun displayAvatarFromUrl(url: String?) {
        if (mAvatar != null) {
            with()
                .load(url)
                .error(R.drawable.ic_avatar_unknown)
                .transform(CurrentTheme.createTransformationForAvatar())
                .into(mAvatar ?: return)
        }
    }

    override fun displayDefaultAvatar() {
        if (mAvatar != null) {
            with()
                .load(R.drawable.ic_avatar_unknown)
                .transform(CurrentTheme.createTransformationForAvatar())
                .into(mAvatar ?: return)
        }
    }

    override fun onButtonClick(number: Int) {
        presenter?.onNumberClicked(
            number
        )
    }

    override fun onBackspaceClick() {
        presenter?.onBackspaceClicked()
    }

    override fun onFingerPrintClick() {
        presenter?.onFingerprintClicked()
    }

    companion object {
        fun newInstance(): EnterPinFragment {
            val bundle = Bundle()
            val fragment = EnterPinFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}