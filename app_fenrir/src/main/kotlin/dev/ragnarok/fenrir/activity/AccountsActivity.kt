package dev.ragnarok.fenrir.activity

import android.graphics.Color
import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.PreferencesFragment.Companion.newInstance
import dev.ragnarok.fenrir.fragment.accounts.AccountsFragment
import dev.ragnarok.fenrir.fragment.shortcutsview.ShortcutsViewFragment
import dev.ragnarok.fenrir.fragment.theme.ThemeFragment
import dev.ragnarok.fenrir.place.Place
import dev.ragnarok.fenrir.place.PlaceProvider
import dev.ragnarok.fenrir.util.toast.CustomSnackbars

class AccountsActivity : NoMainActivity(), PlaceProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(noMainContainerViewId, AccountsFragment())
                .addToBackStack("accounts")
                .commit()
        }
    }

    override fun openPlace(place: Place) {
        when (place.type) {
            Place.PREFERENCES -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(noMainContainerViewId, newInstance(place.safeArguments()))
                    .addToBackStack("preferences")
                    .commit()
            }

            Place.SETTINGS_THEME -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(noMainContainerViewId, ThemeFragment())
                    .addToBackStack("preferences_themes")
                    .commit()
            }

            Place.SHORTCUTS -> {
                supportFragmentManager
                    .beginTransaction()
                    .replace(noMainContainerViewId, ShortcutsViewFragment())
                    .addToBackStack("shortcuts")
                    .commit()
            }

            else -> {
                CustomSnackbars.createCustomSnackbars(findViewById(noMainContainerViewId))
                    ?.coloredSnack(R.string.not_available, Color.RED, false)?.show()
            }
        }
    }
}