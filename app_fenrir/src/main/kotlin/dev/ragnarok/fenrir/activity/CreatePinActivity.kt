package dev.ragnarok.fenrir.activity

import android.os.Bundle
import dev.ragnarok.fenrir.fragment.pin.createpin.CreatePinFragment

class CreatePinActivity : NoMainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(noMainContainerViewId, CreatePinFragment.newInstance())
                .commit()
        }
    }
}