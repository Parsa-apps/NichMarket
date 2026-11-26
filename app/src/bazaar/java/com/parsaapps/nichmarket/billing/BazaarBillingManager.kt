package com.parsaapps.nichmarket.billing

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultRegistryOwner
import com.parsaapps.nichmarket.BuildConfig
import com.parsaapps.nichmarket.data.QuestionRepository
import com.parsaapps.nichmarket.data.UserStore
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseState as BazaarPurchaseState
import ir.cafebazaar.poolakey.request.PurchaseRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** Bazaar uses its own SDK, RSA key, account and application sandbox. */
class BazaarBillingManager(
    private val context: Context,
    private val repository: QuestionRepository,
    private val userStore: UserStore,
) : StoreBillingManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _ready = MutableStateFlow(false)
    override val billingReady: StateFlow<Boolean> = _ready
    private val _state = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    override val purchaseState: StateFlow<PurchaseState> = _state
    private val setupWaiters = mutableListOf<(Boolean) -> Unit>()
    private val inventoryWaiters = mutableListOf<(Boolean) -> Unit>()
    private var payment: Payment? = null
    private var connection: Connection? = null
    private var connecting = false
    private var querying = false
    private var generation = 0

    override fun start(onReady: ((Boolean) -> Unit)?) {
        if (_ready.value && payment != null) { onReady?.invoke(true); return }
        if (BuildConfig.IAB_PUBLIC_KEY.isBlank()) { onReady?.invoke(false); return }
        if (onReady != null) setupWaiters.add(onReady)
        if (connecting) return
        connecting = true
        val id = ++generation
        var timeout: Job? = null
        fun finish(success: Boolean) {
            if (id != generation || !connecting) return
            connecting = false
            timeout?.cancel()
            _ready.value = success
            if (!success) {
                runCatching { connection?.disconnect() }
                connection = null
                payment = null
            }
            val waiters = setupWaiters.toList()
            setupWaiters.clear()
            waiters.forEach { it(success) }
            if (success && waiters.isEmpty()) refreshPurchases()
        }
        timeout = scope.launch { delay(20_000); finish(false) }
        try {
            val client = Payment(context, PaymentConfiguration(SecurityCheck.Enable(BuildConfig.IAB_PUBLIC_KEY)))
            payment = client
            val newConnection = client.connect {
                connectionSucceed { finish(true) }
                connectionFailed { finish(false) }
                disconnected {
                    if (id == generation) {
                        if (connecting) finish(false) else {
                            ++generation
                            _ready.value = false
                            querying = false
                            val waiters = inventoryWaiters.toList()
                            inventoryWaiters.clear()
                            waiters.forEach { it(false) }
                            payment = null
                            connection = null
                        }
                    }
                }
            }
            if (payment === client && id == generation) connection = newConnection
            else runCatching { newConnection.disconnect() }
        } catch (e: Exception) { finish(false) }
    }

    override fun refreshPurchases(onDone: ((Boolean) -> Unit)?) {
        if (!_ready.value || payment == null) {
            start { ready -> if (ready) refreshPurchases(onDone) else onDone?.invoke(false) }
            return
        }
        if (onDone != null) inventoryWaiters.add(onDone)
        if (querying) return
        querying = true
        val id = generation
        val client = payment ?: return
        var timeout: Job? = null
        fun finish(success: Boolean) {
            if (!querying || id != generation) return
            timeout?.cancel()
            querying = false
            val waiters = inventoryWaiters.toList()
            inventoryWaiters.clear()
            waiters.forEach { it(success) }
        }
        timeout = scope.launch { delay(20_000); finish(false) }
        try {
            client.getPurchasedProducts {
                querySucceed { purchases ->
                    if (querying && id == generation) {
                        val owned = purchases.filter {
                            it.packageName == context.packageName &&
                                it.purchaseState == BazaarPurchaseState.PURCHASED &&
                                it.productId in repository.allSkus
                        }.map { it.productId }.toSet()
                        scope.launch {
                            try { userStore.addPurchases(owned); finish(true) }
                            catch (e: Exception) { finish(false) }
                        }
                    }
                }
                queryFailed { finish(false) }
            }
        } catch (e: Exception) { finish(false) }
    }

    override fun purchase(activity: Activity, sku: String) {
        if (BuildConfig.IAB_PUBLIC_KEY.isBlank()) {
            _state.value = PurchaseState.Error("کلید عمومی پرداخت کافه‌بازار هنوز تنظیم نشده است.")
            return
        }
        if (sku !in repository.allSkus) {
            _state.value = PurchaseState.Error("شناسه محصول معتبر نیست.")
            return
        }
        val registry = (activity as? ActivityResultRegistryOwner)?.activityResultRegistry
        val client = payment
        if (registry == null || client == null || !_ready.value) {
            start()
            _state.value = PurchaseState.Error("اتصال به کافه‌بازار آماده نیست؛ پس از چند لحظه دوباره تلاش کنید.")
            return
        }
        if (_state.value is PurchaseState.Buying) return
        _state.value = PurchaseState.Buying
        val payload = UUID.randomUUID().toString()
        try {
            client.purchaseProduct(registry, PurchaseRequest(productId = sku, payload = payload)) {
                purchaseSucceed { purchase ->
                    if (purchase.productId != sku || purchase.payload != payload ||
                        purchase.packageName != context.packageName ||
                        purchase.purchaseState != BazaarPurchaseState.PURCHASED) {
                        _state.value = PurchaseState.Error("خطا در تأیید صحت خرید.")
                    } else scope.launch {
                        try { userStore.addPurchase(sku); _state.value = PurchaseState.Success(sku) }
                        catch (e: Exception) { _state.value = PurchaseState.Error("ذخیره خرید ناموفق بود؛ خریدها را بازیابی کنید.") }
                    }
                }
                purchaseCanceled { _state.value = PurchaseState.Cancelled }
                purchaseFailed { _state.value = PurchaseState.Error("خرید از کافه‌بازار ناموفق بود؛ دوباره تلاش کنید.") }
                failedToBeginFlow { _state.value = PurchaseState.Error("باز کردن صفحه خرید کافه‌بازار ناموفق بود.") }
            }
        } catch (e: Exception) { _state.value = PurchaseState.Error("شروع خرید کافه‌بازار ناموفق بود.") }
    }

    override fun resetState() { _state.value = PurchaseState.Idle }
    override fun dispose() {
        ++generation
        connecting = false
        querying = false
        _ready.value = false
        runCatching { connection?.disconnect() }
        connection = null
        payment = null
        val waiters = setupWaiters.toList() + inventoryWaiters.toList()
        setupWaiters.clear()
        inventoryWaiters.clear()
        waiters.forEach { it(false) }
    }
}

fun createStoreBillingManager(context: Context, repository: QuestionRepository, userStore: UserStore): StoreBillingManager =
    BazaarBillingManager(context, repository, userStore)
