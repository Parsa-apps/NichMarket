package com.parsaapps.nichmarket

import android.app.Application
import com.parsaapps.nichmarket.billing.StoreBillingManager
import com.parsaapps.nichmarket.billing.createStoreBillingManager
import com.parsaapps.nichmarket.data.QuestionRepository
import com.parsaapps.nichmarket.data.UserStore

/** وابستگی‌های سراسری اپ (بدون نیاز به DI فریم‌ورک) */
object Graph {
    lateinit var repository: QuestionRepository
        private set
    lateinit var userStore: UserStore
        private set
    lateinit var billing: StoreBillingManager
        private set

    fun init(app: Application, store: UserStore = UserStore(app), startBilling: Boolean = true) {
        repository = QuestionRepository(app)
        userStore = store
        billing = createStoreBillingManager(app, repository, userStore)
        if (startBilling) billing.start()
    }
}

class NichMarketApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
