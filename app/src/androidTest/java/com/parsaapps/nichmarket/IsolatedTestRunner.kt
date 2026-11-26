package com.parsaapps.nichmarket

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.runner.AndroidJUnitRunner
import com.parsaapps.nichmarket.data.UserStore
import java.io.File
import java.util.UUID

/** Instrumentation never writes to the user's production DataStore or connects to billing. */
class IsolatedTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, IsolatedTestApplication::class.java.name, context)
}

class IsolatedTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val file = File(cacheDir, "instrumentation-${UUID.randomUUID()}.preferences_pb")
        val store = UserStore(this, PreferenceDataStoreFactory.create { file })
        Graph.init(this, store = store, startBilling = false)
    }
}
