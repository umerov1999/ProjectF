package dev.ragnarok.fenrir.activity

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.addproxy.AddProxyFragment
import dev.ragnarok.fenrir.fragment.proxymanager.ProxyManagerFrgament
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider

class ProxyManagerActivity : NoMainActivity(), PlaceProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
                .replace(getMainContainerViewId(), ProxyManagerFrgament.newInstance())
                .addToBackStack("proxy-manager")
                .commit()
        }
    }

    override fun openPlace(place: Place) {
        if (place.type == Place.PROXY_ADD) {
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter_pop, R.anim.fragment_exit_pop)
                .replace(getMainContainerViewId(), AddProxyFragment.newInstance())
                .addToBackStack("proxy-add")
                .commit()
        }
    }
}