package com.parsaapps.nichmarket.ui.screens.category

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.billing.PurchaseState
import com.parsaapps.nichmarket.data.model.Category
import com.parsaapps.nichmarket.data.model.QuestionPack
import com.parsaapps.nichmarket.util.formatToman
import com.parsaapps.nichmarket.util.toPersian
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class CategoryViewModel : ViewModel() {
    val purchasedSkus = Graph.userStore.purchasedSkus
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val purchaseState = Graph.billing.purchaseState

    fun purchase(activity: Activity, sku: String) = Graph.billing.purchase(activity, sku)
    fun restorePurchases() = Graph.billing.refreshPurchases()
    fun resetPurchaseState() = Graph.billing.resetState()

    fun categoryOf(catId: String): Category? = Graph.repository.findCategory(catId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    catId: String,
    onBack: () -> Unit,
    onStartExam: (String) -> Unit
) {
    val vm: CategoryViewModel = viewModel()
    val category = remember(catId) { vm.categoryOf(catId) }
    val purchased by vm.purchasedSkus.collectAsState()
    val purchaseState by vm.purchaseState.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(purchaseState) {
        when (val s = purchaseState) {
            is PurchaseState.Success -> {
                snackbar.showSnackbar("خرید با موفقیت انجام شد. بسته فعال شد! 🎉")
                vm.resetPurchaseState()
            }
            is PurchaseState.Cancelled -> {
                snackbar.showSnackbar("خرید لغو شد.")
                vm.resetPurchaseState()
            }
            is PurchaseState.Error -> {
                snackbar.showSnackbar(s.message)
                vm.resetPurchaseState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(category?.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.restorePurchases() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "بازیابی خریدها")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (category == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("این دسته‌بندی پیدا نشد.")
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }
            items(category.packages, key = { it.id }) { pack ->
                val owned = pack.isFree || (pack.sku != null && pack.sku in purchased)
                PackCard(
                    pack = pack,
                    owned = owned,
                    buying = purchaseState is PurchaseState.Buying,
                    onBuy = {
                        val activity = context as? Activity
                        if (activity != null && pack.sku != null) {
                            vm.purchase(activity, pack.sku)
                        }
                    },
                    onStart = { onStartExam(pack.id) }
                )
            }
        }
    }
}

@Composable
private fun PackCard(
    pack: QuestionPack,
    owned: Boolean,
    buying: Boolean,
    onBuy: () -> Unit,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (owned) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pack.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (pack.isFree) {
                    Text(
                        text = "رایگان",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = pack.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${pack.questions.size.toPersian()} سوال • ${pack.durationMinutes.toPersian()} دقیقه",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (owned) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("شروع آزمون")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatToman(pack.priceToman),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (buying) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    } else {
                        Button(
                            onClick = onBuy,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text("خرید از مایکت")
                        }
                    }
                }
            }
        }
    }
}
