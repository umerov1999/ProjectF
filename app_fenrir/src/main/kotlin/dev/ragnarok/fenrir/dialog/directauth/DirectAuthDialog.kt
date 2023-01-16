package dev.ragnarok.fenrir.dialog.directauth

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpDialogFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.TextWatcherAdapter
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with

class DirectAuthDialog : BaseMvpDialogFragment<DirectAuthPresenter, IDirectAuthView>(),
    IDirectAuthView {
    private var mLogin: TextInputEditText? = null
    private var mPassword: TextInputEditText? = null
    private var mCaptcha: TextInputEditText? = null
    private var mSmsCode: TextInputEditText? = null
    private var mSavePassword: MaterialSwitch? = null
    private var mSmsCodeRoot: View? = null
    private var mContentRoot: View? = null
    private var mLoadingRoot: View? = null
    private var mCaptchaRoot: View? = null
    private var mCaptchaImage: ImageView? = null
    private var mEnterAppCodeRoot: View? = null
    private var mAppCode: TextInputEditText? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val view = View.inflate(requireActivity(), R.layout.dialog_direct_auth, null)
        mLogin = view.findViewById(R.id.field_username)
        mLogin?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireLoginEdit(s)
            }
        })
        mPassword = view.findViewById(R.id.field_password)
        mPassword?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.firePasswordEdit(s)
            }
        })
        mEnterAppCodeRoot = view.findViewById(R.id.field_app_code_root)
        mAppCode = view.findViewById(R.id.field_app_code)
        mAppCode?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireAppCodeEdit(s)
            }
        })
        view.findViewById<View>(R.id.button_send_code_via_sms).setOnClickListener {
            presenter?.fireButtonSendCodeViaSmsClick()
        }
        mSmsCodeRoot = view.findViewById(R.id.field_sms_code_root)
        mSmsCode = view.findViewById(R.id.field_sms_code)
        mSmsCode?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireSmsCodeEdit(s)
            }
        })
        mContentRoot = view.findViewById(R.id.content_root)
        mLoadingRoot = view.findViewById(R.id.loading_root)
        mCaptchaRoot = view.findViewById(R.id.captcha_root)
        mCaptcha = view.findViewById(R.id.field_captcha)
        mCaptcha?.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireCaptchaEdit(s)
            }
        })
        mCaptchaImage = view.findViewById(R.id.captcha_img)
        mSavePassword = view.findViewById(R.id.save_password)
        mSavePassword?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            presenter?.fireSaveEdit(
                isChecked
            )
        }
        builder.setView(view)
        builder.setPositiveButton(R.string.button_login, null)
        if (Constants.DEFAULT_ACCOUNT_TYPE == AccountType.KATE) {
            builder.setNeutralButton(R.string.button_login_via_web) { _: DialogInterface?, _: Int ->
                presenter?.fireLoginViaWebClick()
            }
        }
        builder.setTitle(R.string.login_title)
        builder.setIcon(R.drawable.logo_vk)
        val dialog = builder.create()
        dialog.setCancelable(true)
        fireViewCreated()
        return dialog
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<DirectAuthPresenter> {
        return object : IPresenterFactory<DirectAuthPresenter> {
            override fun create(): DirectAuthPresenter {
                return DirectAuthPresenter(saveInstanceState)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val buttonLogin = (dialog as AlertDialog?)?.getButton(DialogInterface.BUTTON_POSITIVE)
        buttonLogin?.setOnClickListener {
            presenter?.fireLoginClick()
        }
    }

    override fun setLoginButtonEnabled(enabled: Boolean) {
        val buttonLogin = (dialog as AlertDialog?)?.getButton(DialogInterface.BUTTON_POSITIVE)
        buttonLogin?.isEnabled = enabled
        mSavePassword?.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    override fun setSmsRootVisible(visible: Boolean) {
        mSmsCodeRoot?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setAppCodeRootVisible(visible: Boolean) {
        mEnterAppCodeRoot?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun moveFocusToSmsCode() {
        mSmsCode?.requestFocus()
    }

    override fun moveFocusToAppCode() {
        mAppCode?.requestFocus()
    }

    override fun displayLoading(loading: Boolean) {
        mLoadingRoot?.visibility = if (loading) View.VISIBLE else View.GONE
        mContentRoot?.visibility = if (loading) View.INVISIBLE else View.VISIBLE
    }

    override fun setCaptchaRootVisible(visible: Boolean) {
        mCaptchaRoot?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun displayCaptchaImage(img: String?) {
        if (mCaptchaImage != null) {
            with()
                .load(img)
                .placeholder(R.drawable.background_gray)
                .into(mCaptchaImage ?: return)
        }
    }

    override fun moveFocusToCaptcha() {
        mCaptcha?.requestFocus()
    }

    override fun hideKeyboard() {
        try {
            val im =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            im?.hideSoftInputFromWindow(mLogin?.windowToken, 0)
            im?.hideSoftInputFromWindow(mPassword?.windowToken, 0)
            im?.hideSoftInputFromWindow(mCaptcha?.windowToken, 0)
            im?.hideSoftInputFromWindow(mSmsCode?.windowToken, 0)
        } catch (ignored: Exception) {
        }
    }

    override fun returnSuccessToParent(
        userId: Long,
        accessToken: String?,
        Login: String?,
        Password: String?,
        twoFA: String?,
        isSave: Boolean
    ) {
        val data = Bundle()
        data.putString(Extra.TOKEN, accessToken)
        data.putLong(Extra.USER_ID, userId)
        data.putString(Extra.LOGIN, Login)
        data.putString(Extra.PASSWORD, Password)
        data.putString(Extra.TWO_FA, twoFA)
        data.putBoolean(Extra.SAVE, isSave)
        returnResultAndDismiss(ACTION_LOGIN_COMPLETE, data)
    }

    override fun returnSuccessValidation(
        url: String?,
        Login: String?,
        Password: String?,
        twoFA: String?,
        isSave: Boolean
    ) {
        val data = Bundle()
        data.putString(Extra.URL, url)
        data.putString(Extra.LOGIN, Login)
        data.putString(Extra.PASSWORD, Password)
        data.putString(Extra.TWO_FA, twoFA)
        data.putBoolean(Extra.SAVE, isSave)
        returnResultAndDismiss(ACTION_VALIDATE_VIA_WEB, data)
    }

    private fun returnResultAndDismiss(key: String, data: Bundle) {
        parentFragmentManager.setFragmentResult(key, data)
        dismiss()
    }

    override fun returnLoginViaWebAction() {
        returnResultAndDismiss(ACTION_LOGIN_VIA_WEB, Bundle())
    }

    companion object {
        const val ACTION_LOGIN_COMPLETE = "ACTION_LOGIN_COMPLETE"
        const val ACTION_LOGIN_VIA_WEB = "ACTION_LOGIN_VIA_WEB"
        const val ACTION_VALIDATE_VIA_WEB = "ACTION_VALIDATE_VIA_WEB"


        fun newInstance(): DirectAuthDialog {
            val args = Bundle()
            val fragment = DirectAuthDialog()
            fragment.arguments = args
            return fragment
        }
    }
}