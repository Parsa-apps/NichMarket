package com.parsaapps.nichmarket.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

sealed interface PurchaseState {
    data object Idle : PurchaseState
    data object Buying : PurchaseState
    data class Success(val sku: String) : PurchaseState
    data object Cancelled : PurchaseState
    data class Error(val message: String) : PurchaseState
}

interface StoreBillingManager {
    val billingReady: StateFlow<Boolean>
    val purchaseState: StateFlow<PurchaseState>
    fun start(onReady: ((Boolean) -> Unit)? = null)
    fun refreshPurchases(onDone: ((Boolean) -> Unit)? = null)
    fun purchase(activity: Activity, sku: String)
    fun resetState()
    fun dispose()
}
