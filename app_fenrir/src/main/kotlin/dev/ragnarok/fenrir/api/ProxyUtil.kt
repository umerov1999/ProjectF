package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.util.ValidationUtil
import okhttp3.Authenticator
import okhttp3.Credentials.basic
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Route
import java.net.InetSocketAddress
import java.net.Proxy

object ProxyUtil {
    private fun obtainAddress(config: ProxyConfig): InetSocketAddress {
        return if (ValidationUtil.isValidIpAddress(config.getAddress())) {
            InetSocketAddress(config.getAddress(), config.getPort())
        } else {
            InetSocketAddress.createUnresolved(config.getAddress(), config.getPort())
        }
    }

    fun applyProxyConfig(builder: OkHttpClient.Builder, config: ProxyConfig?) {
        if (config != null) {
            val proxy = Proxy(
                Proxy.Type.HTTP, obtainAddress(
                    config
                )
            )
            builder.proxy(proxy)
            if (config.isAuthEnabled()) {
                val authenticator = Authenticator { _: Route?, response: Response ->
                    val credential = config.getUser()?.let {
                        config.getPass()?.let { it1 ->
                            basic(
                                it, it1
                            )
                        }
                    }
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credential.orEmpty())
                        .build()
                }
                builder.proxyAuthenticator(authenticator)
            }
        }
    }
}