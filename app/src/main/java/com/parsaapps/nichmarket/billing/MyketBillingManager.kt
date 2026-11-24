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

    val billingReady = MutableStateFlow(false)
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    private val publicKeyConfigured: Boolean =
        BuildConfig.IAB_PUBLIC_KEY.isNotBlank() &&
            !BuildConfig.IAB_PUBLIC_KEY.startsWith("REPLACE")

    fun start() {
        if (helper != null) return
        if (!publicKeyConfigured) {
            // کلید عمومی مایکت هنوز در app/build.gradle.kts تنظیم نشده است
            return
        }
        val h = IabHelper(appContext, BuildConfig.IAB_PUBLIC_KEY)
        helper = h
        try {
            h.startSetup { result: IabResult ->
                if (result.isSuccess) {
                    billingReady.value = true
                    refreshPurchases()
                } else {
                    billingReady.value = false
                }
            }
        } catch (t: Throwable) {
            helper = null
        }
    }

    /** بازیابی/همگام‌سازی خریدهای قبلی کاربر از مایکت */
    fun refreshPurchases(onDone: (() -> Unit)? = null) {
        val h = helper
        if (h == null || !billingReady.value) {
            onDone?.invoke()
            return
        }
        try {
            h.queryInventoryAsync(true, repository.allSkus) { result, inventory ->
                if (result.isSuccess && inventory != null) {
                    val owned = repository.allSkus
                        .filter { inventory.getPurchase(it) != null }
                        .toSet()
                    scope.launch { userStore.setPurchases(owned) }
                }
                onDone?.invoke()
            }
        } catch (t: Throwable) {
            onDone?.invoke()
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
                    purchase.developerPayload != payload -> {
                        _purchaseState.value = PurchaseState.Error("خطا در تأیید صحت خرید.")
                    }
                    else -> {
                        scope.launch { userStore.addPurchase(sku) }
                        _purchaseState.value = PurchaseState.Success(sku)
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
    }
}
