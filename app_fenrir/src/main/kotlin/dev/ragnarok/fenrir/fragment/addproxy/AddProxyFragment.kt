package dev.ragnarok.fenrir.fragment.addproxy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.BaseMvpFragment
import dev.ragnarok.fenrir.fragment.base.core.IPresenterFactory
import dev.ragnarok.fenrir.listener.TextWatcherAdapter

class AddProxyFragment : BaseMvpFragment<AddProxyPresenter, IAddProxyView>(), IAddProxyView {
    private var mAuth: MaterialCheckBox? = null
    private var mAuthFieldsRoot: View? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_proxy_add, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(root.findViewById(R.id.toolbar))
        mAuthFieldsRoot = root.findViewById(R.id.auth_fields_root)
        val mAddress: TextInputEditText = root.findViewById(R.id.address)
        mAddress.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireAddressEdit(
                    s
                )
            }
        })
        val mPort: TextInputEditText = root.findViewById(R.id.port)
        mPort.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.firePortEdit(
                    s
                )
            }
        })
        mAuth = root.findViewById(R.id.authorization)
        mAuth?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            presenter?.fireAuthChecked(
                isChecked
            )
        }
        val mUsername: TextInputEditText = root.findViewById(R.id.username)
        mUsername.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.fireUsernameEdit(
                    s
                )
            }
        })
        val mPassword: TextInputEditText = root.findViewById(R.id.password)
        mPassword.addTextChangedListener(object : TextWatcherAdapter() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                presenter?.firePassEdit(
                    s
                )
            }
        })
        root.findViewById<View>(R.id.button_save).setOnClickListener {
            presenter?.fireSaveClick()
        }
        return root
    }

    override fun setAuthFieldsEnabled(enabled: Boolean) {
        mAuthFieldsRoot?.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    override fun setAuthChecked(checked: Boolean) {
        mAuth?.isChecked = checked
    }

    override fun goBack() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<AddProxyPresenter> {
        return object : IPresenterFactory<AddProxyPresenter> {
            override fun create(): AddProxyPresenter {
                return AddProxyPresenter(saveInstanceState)
            }
        }
    }

    companion object {
        fun newInstance(): AddProxyFragment {
            val args = Bundle()
            val fragment = AddProxyFragment()
            fragment.arguments = args
            return fragment
        }
    }
}