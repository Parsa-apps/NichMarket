package com.parsaapps.nichmarket

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class ReleaseConfigurationTest {
    @Test fun embeddedMyketKeyIsTheRequestedKey() {
        val bytes = Base64.decode(BuildConfig.IAB_PUBLIC_KEY, Base64.DEFAULT)
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertEquals("f0794f3f489406021f1d82dc774080907a64abc2e7e2cc9362756baefba4a3c8", fingerprint)
    }
}
