package com.parsaapps.nichmarket.billing

import android.app.Activity
import android.content.Context
import com.parsaapps.nichmarket.BuildConfig
import com.parsaapps.nichmarket.data.QuestionRepository
import com.parsaapps.nichmarket.data.UserStore
import ir.myket.billingclient.IabHelper
import ir.myket.billingclient.util.IabResult
import ir.myket.billingclient.util.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/** وضعیت جریان خرید برای نمایش در UI */
sealed interface PurchaseState {
    data object Idle : PurchaseState
    data object Buying : PurchaseState
    data class Success(val sku: String) : PurchaseState
    data object Cancelled : PurchaseState
    data class Error(val message: String) : PurchaseState
}

/**
 * مدیریت پرداخت درون‌برنامه‌ای مایکت
 * اسناد رسمی SDK: https://github.com/myketstore/billing-sample-android
 */
class MyketBillingManager(
    private val appContext: Context,
    private val repository: QuestionRepository,
    private val userStore: UserStore
) {
    private var helper: IabHelper? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val setupWaiters = mutableListOf<(Boolean) -> Unit>()
    private val inventoryWaiters = mutableListOf<(Boolean) -> Unit>()
    private var queryingInventory = false

    val billingReady = MutableStateFlow(false)
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    private val publicKeyConfigured: Boolean =
        BuildConfig.IAB_PUBLIC_KEY.isNotBlank() &&
            !BuildConfig.IAB_PUBLIC_KEY.startsWith("REPLACE")

    fun start(onReady: ((Boolean) -> Unit)? = null) {
        if (billingReady.value && helper != null) {
            onReady?.invoke(true)
            return
        }
        if (!publicKeyConfigured) {
            onReady?.invoke(false)
            return
        }
        if (onReady != null) setupWaiters.add(onReady)
        if (helper != null) return // Setup is still in progress.
        val h = try {
            IabHelper(appContext, BuildConfig.IAB_PUBLIC_KEY)
        } catch (e: Exception) {
            val waiters = setupWaiters.toList()
            setupWaiters.clear()
            waiters.forEach { it(false) }
            return
        }
        helper = h
        var timeout: Job? = null
        fun finishSetup(success: Boolean) {
            if (helper !== h) return
            timeout?.cancel()
            billingReady.value = success
            if (!success) {
                runCatching { h.dispose() }
                helper = null
            }
            val waiters = setupWaiters.toList()
            setupWaiters.clear()
            waiters.forEach { it(success) }
            if (success && waiters.isEmpty()) refreshPurchases()
        }
        timeout = scope.launch {
            delay(20_000)
            finishSetup(false)
        }
        try {
            h.startSetup { result: IabResult -> finishSetup(result.isSuccess) }
        } catch (e: Exception) {
            finishSetup(false)
        }
    }

    /** Queries are shared while setup or an inventory request is already in flight. */
    fun refreshPurchases(onDone: ((Boolean) -> Unit)? = null) {
        if (!billingReady.value || helper == null) {
            start { ready ->
                if (ready) refreshPurchases(onDone) else onDone?.invoke(false)
            }
            return
        }
        if (onDone != null) inventoryWaiters.add(onDone)
        if (queryingInventory) return
        queryingInventory = true
        val h = helper ?: return
        var timeout: Job? = null
        fun finishQuery(success: Boolean, resetConnection: Boolean = false) {
            if (!queryingInventory || helper !== h) return
            timeout?.cancel()
            queryingInventory = false
            if (resetConnection) {
                billingReady.value = false
                helper = null
                runCatching { h.dispose() }
            }
            val waiters = inventoryWaiters.toList()
            inventoryWaiters.clear()
            waiters.forEach { it(success) }
        }
        timeout = scope.launch {
            delay(20_000)
            finishQuery(false, resetConnection = true)
        }
        try {
            h.queryInventoryAsync(true, repository.allSkus) { result, inventory ->
                if (result.isSuccess && inventory != null) {
                    val owned = repository.allSkus.filter { inventory.getPurchase(it) != null }.toSet()
                    scope.launch {
                        try {
                            userStore.addPurchases(owned)
                            finishQuery(true)
                        } catch (e: Exception) {
                            finishQuery(false)
                        }
                    }
                } else {
                    finishQuery(false)
                }
            }
        } catch (e: Exception) {
            finishQuery(false)
        }
    }

    /** شروع فرایند خرید یک بسته */
    fun purchase(activity: Activity, sku: String) {
        val h = helper
        if (!publicKeyConfigured) {
            _purchaseState.value = PurchaseState.Error(
                "کلید عمومی مایکت تنظیم نشده است. راهنما در README پروژه."
            )
            return
        }
        if (h == null || !billingReady.value) {
            _purchaseState.value = PurchaseState.Error(
                "اتصال به مایکت برقرار نشد. مطمئن شوید مایکت روی گوشی نصب و به حساب خود وارد شده‌اید."
            )
            return
        }
        _purchaseState.value = PurchaseState.Buying
        val payload = UUID.randomUUID().toString()
        try {
            h.launchPurchaseFlow(activity, sku, { result, purchase: Purchase? ->
                when {
                    result.isFailure || purchase == null -> {
                        val msg = result.message.orEmpty()
                        _purchaseState.value =
                            if (msg.contains("cancel", ignoreCase = true)) PurchaseState.Cancelled
                            else PurchaseState.Error(msg.ifBlank { "خرید ناموفق بود." })
                    }
                    purchase.sku != sku || purchase.developerPayload != payload -> {
                        _purchaseState.value = PurchaseState.Error("خطا در تأیید صحت خرید.")
                    }
                    else -> {
                        scope.launch {
                            try {
                                userStore.addPurchase(sku)
                                _purchaseState.value = PurchaseState.Success(sku)
                            } catch (e: Exception) {
                                _purchaseState.value = PurchaseState.Error("ذخیره خرید ناموفق بود؛ لطفاً خریدها را بازیابی کنید.")
                            }
                        }
                    }
                }
            }, payload)
        } catch (t: Throwable) {
            _purchaseState.value = PurchaseState.Error("خطا در اجرای فرایند خرید: ${t.message}")
        }
    }

    fun resetState() {
        _purchaseState.value = PurchaseState.Idle
    }

    fun dispose() {
        runCatching { helper?.dispose() }
        helper = null
        billingReady.value = false
        queryingInventory = false
        val waiters = setupWaiters.toList() + inventoryWaiters.toList()
        setupWaiters.clear()
        inventoryWaiters.clear()
        waiters.forEach { it(false) }
    }
}
