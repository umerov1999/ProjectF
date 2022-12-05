package dev.ragnarok.fenrir.activity

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.createpin.CreatePinFragment

class CreatePinActivity : NoMainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, CreatePinFragment.newInstance())
                .commit()
        }
    }
}