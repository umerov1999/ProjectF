package dev.ragnarok.fenrir.activity.captcha.web

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

internal class NetworkConnectionObserver(
    private val context: Context
) {

    fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager?
            ?: return false

        @Suppress("deprecation")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.activeNetwork?.let {
                connectivityManager.getNetworkCapabilities(it)?.isNetworkCapabilitiesValid()
            } ?: false
        } else {
            connectivityManager.activeNetworkInfo != null
                    && connectivityManager.activeNetworkInfo!!.isConnected
        }
    }

    private fun NetworkCapabilities.isNetworkCapabilitiesValid(): Boolean = when {
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) -> true

        else -> false
    }
}