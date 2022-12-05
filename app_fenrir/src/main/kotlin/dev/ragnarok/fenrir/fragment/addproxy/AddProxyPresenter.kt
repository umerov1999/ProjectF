package dev.ragnarok.fenrir.fragment.addproxy

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.proxySettings
import dev.ragnarok.fenrir.fragment.base.RxSupportPresenter
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.trimmedIsNullOrEmpty
import dev.ragnarok.fenrir.util.ValidationUtil.isValidIpAddress
import dev.ragnarok.fenrir.util.ValidationUtil.isValidURL

class AddProxyPresenter(savedInstanceState: Bundle?) :
    RxSupportPresenter<IAddProxyView>(savedInstanceState) {
    private val settings: IProxySettings = proxySettings
    private var authEnabled = false
    private var address: String? = null
    private var port: String? = null
    private var userName: String? = null
    private var pass: String? = null
    override fun onGuiCreated(viewHost: IAddProxyView) {
        super.onGuiCreated(viewHost)
        viewHost.setAuthFieldsEnabled(authEnabled)
        viewHost.setAuthChecked(authEnabled)
    }

    fun fireAddressEdit(s: CharSequence?) {
        address = s.toString()
    }

    fun firePortEdit(s: CharSequence?) {
        port = s.toString()
    }

    fun fireAuthChecked(isChecked: Boolean) {
        if (authEnabled == isChecked) {
            return
        }
        authEnabled = isChecked
        view?.setAuthFieldsEnabled(isChecked)
    }

    fun fireUsernameEdit(s: CharSequence?) {
        userName = s.toString()
    }

    fun firePassEdit(s: CharSequence?) {
        pass = s.toString()
    }

    private fun validateData(): Boolean {
        try {
            try {
                val portInt = port?.toInt() ?: 0
                if (portInt <= 0) {
                    throw Exception("Invalid port")
                }
            } catch (e: NumberFormatException) {
                throw Exception("Invalid port")
            }
            if (!isValidIpAddress(address) && !isValidURL(address)) {
                throw Exception("Invalid address")
            }
            if (authEnabled && userName.trimmedIsNullOrEmpty()) {
                throw Exception("Invalid username")
            }
            if (authEnabled && pass.trimmedIsNullOrEmpty()) {
                throw Exception("Invalid password")
            }
        } catch (e: Exception) {
            showError(e)
            return false
        }
        return true
    }

    fun fireSaveClick() {
        if (!validateData()) {
            return
        }
        val finalAddress = address?.trim { it <= ' ' }
        val finalPort = port?.trim { it <= ' ' }?.toInt() ?: 0
        if (authEnabled) {
            if (finalAddress != null) {
                userName?.let {
                    pass?.let { it1 ->
                        settings.put(
                            finalAddress,
                            finalPort,
                            it,
                            it1
                        )
                    }
                }
            }
        } else {
            if (finalAddress != null) {
                settings.put(finalAddress, finalPort)
            }
        }
        view?.goBack()
    }

}