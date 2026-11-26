package com.parsaapps.nichmarket.ui.screens.category

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.billing.PurchaseState
import com.parsaapps.nichmarket.data.model.Category
import com.parsaapps.nichmarket.data.model.QuestionPack
import com.parsaapps.nichmarket.ui.components.categoryIcon
import com.parsaapps.nichmarket.util.formatToman
import com.parsaapps.nichmarket.util.toPersian
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class CategoryViewModel : ViewModel() {
    val purchasedSkus = Graph.userStore.purchasedSkus
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val purchaseState = Graph.billing.purchaseState
    val restoring = MutableStateFlow(false)

    fun purchase(activity: Activity, sku: String) = Graph.billing.purchase(activity, sku)
    fun restorePurchases(onDone: (Boolean) -> Unit) {
        if (restoring.value) return
        restoring.value = true
        Graph.billing.refreshPurchases { success ->
            restoring.value = false
            onDone(success)
        }
    }
    fun resetPurchaseState() = Graph.billing.resetState()

    fun categoryOf(catId: String): Category? = Graph.repository.findCategory(catId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    catId: String,
    onBack: () -> Unit,
    onStartExam: (String, Int) -> Unit
) {
    val vm: CategoryViewModel = viewModel()
    val category = remember(catId) { vm.categoryOf(catId) }
    val purchased by vm.purchasedSkus.collectAsState()
    val purchaseState by vm.purchaseState.collectAsState()
    val restoring by vm.restoring.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedPack by remember { mutableStateOf<QuestionPack?>(null) }

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
                    IconButton(enabled = !restoring, onClick = {
                        vm.restorePurchases { success ->
                            scope.launch {
                                snackbar.showSnackbar(if (success) "همگام‌سازی خریدها انجام شد." else "بازیابی انجام نشد؛ اتصال مایکت را بررسی و دوباره تلاش کنید.")
                            }
                        }
                    }) {
                        if (restoring) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                        else Icon(Icons.Filled.Refresh, contentDescription = "بازیابی خریدها")
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
                .padding(padding).testTag("category-list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF102D2E), Color(0xFF1D6659))))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                        Icon(categoryIcon(category.id), contentDescription = null, tint = Color(0xFFF1D39A),
                            modifier = Modifier.size(30.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("مسیر یادگیری", style = MaterialTheme.typography.labelMedium, color = Color(0xFFECCF9A))
                        Text(category.title, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${category.totalQuestions.toPersian()} سؤال تمرینی • ${category.packages.size.toPersian()} بسته",
                            style = MaterialTheme.typography.labelMedium, color = Color(0xFFDCECE3))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "سوال‌ها تمرینی و تألیفی‌اند، نه سوالات رسمی آزمون‌های گذشته. درصد آزمون بدون نمره منفی محاسبه می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                Text("بسته‌های این مسیر", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
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
                    onStart = {
                        selectedPack = pack
                    }
                )
            }
        }
    }
    selectedPack?.let { pack ->
        AlertDialog(
            onDismissRequest = { selectedPack = null },
            title = { Text("انتخاب نوع آزمون") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("آزمون کوتاه از سؤال‌های این بسته به‌صورت تصادفی و بدون تکرار انتخاب می‌شود. زمان متناسب با تعداد سؤال‌هاست و نمره منفی ندارد.")
                    listOf(5, 10, 20, 40).filter { it < pack.questions.size }.forEach { count ->
                        val minutes = ((pack.durationMinutes.toLong() * count + pack.questions.size - 1) / pack.questions.size).toInt()
                        OutlinedButton(onClick = {
                            selectedPack = null
                            onStartExam(pack.id, count)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("${count.toPersian()} سوال تصادفی • ${minutes.toPersian()} دقیقه")
                        }
                    }
                    OutlinedButton(onClick = {
                        selectedPack = null
                        onStartExam(pack.id, 0)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("همه ${pack.questions.size.toPersian()} سوال • ${pack.durationMinutes.toPersian()} دقیقه")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { selectedPack = null }) { Text("انصراف") } }
        )
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                if (pack.isFree || owned) {
                    Text(
                        text = if (pack.isFree) "رایگان" else "خریداری‌شده",
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
            val subjects = pack.questions.map { it.subject }.filter { it.isNotBlank() }.distinct()
            if (subjects.isNotEmpty()) {
                Text("سرفصل‌ها: ${subjects.take(4).joinToString("، ")}" +
                    if (subjects.size > 4) " و ${ (subjects.size - 4).toPersian() } درس دیگر" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (owned) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().testTag("start-${pack.id}"),
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
