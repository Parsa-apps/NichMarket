package com.parsaapps.nichmarket

import android.app.Application
import com.parsaapps.nichmarket.billing.MyketBillingManager
import com.parsaapps.nichmarket.data.QuestionRepository
import com.parsaapps.nichmarket.data.UserStore

/** وابستگی‌های سراسری اپ (بدون نیاز به DI فریم‌ورک) */
object Graph {
    lateinit var repository: QuestionRepository
        private set
    lateinit var userStore: UserStore
        private set
    lateinit var billing: MyketBillingManager
        private set

    fun init(app: Application) {
        repository = QuestionRepository(app)
        userStore = UserStore(app)
        billing = MyketBillingManager(app, repository, userStore)
        billing.start()
    }
}

class NichMarketApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
