package com.parsaapps.nichmarket.ui.screens.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.data.analysis.studyStatistics
import com.parsaapps.nichmarket.data.analysis.recentResults
import com.parsaapps.nichmarket.data.analysis.answeredQuestions
import com.parsaapps.nichmarket.data.analysis.matchesPersianSearch
import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.ui.components.GradientHeader
import com.parsaapps.nichmarket.util.formatClock
import com.parsaapps.nichmarket.util.formatShamsi
import com.parsaapps.nichmarket.util.toPersian
import com.parsaapps.nichmarket.util.toPersianDigits
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow

class ResultsViewModel : ViewModel() {
    val results = Graph.userStore.results
    val errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private fun save(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() } catch (e: CancellationException) { throw e } catch (e: Exception) {
                errors.emit("حذف کارنامه انجام نشد؛ دوباره تلاش کنید.")
            }
        }
    }
    fun clearAll() = save { Graph.userStore.clearResults() }
    fun delete(id: String) = save { Graph.userStore.deleteResult(id) }
}

@Composable
fun ResultsScreen(onOpenResult: (String) -> Unit) {
    val vm: ResultsViewModel = viewModel()
    val results by vm.results.collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }
    var resultToDelete by remember { mutableStateOf<ExamResult?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(0) }
    var order by rememberSaveable { mutableStateOf("جدیدترین") }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.errors.collect { snackbar.showSnackbar(it) } }
    val visibleResults = remember(results, query, period, order) {
        val inPeriod = if (period == 0) results else recentResults(results, period, System.currentTimeMillis())
        val matched = inPeriod.filter { matchesPersianSearch(query, it.packTitle) }
        when (order) {
            "قدیمی‌ترین" -> matched.sortedBy { it.date }
            "بیشترین درصد" -> matched.sortedWith(compareByDescending<ExamResult> { it.percent }.thenByDescending { it.date })
            else -> matched.sortedByDescending { it.date }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
        item {
            GradientHeader(
                title = "کارنامه‌ها",
                subtitle = "تاریخچه آزمون‌های شما"
            )
            Spacer(Modifier.height(8.dp))
        }
        if (results.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "هنوز آزمونی نداده‌اید.\nاولین بسته را از صفحه خانه شروع کنید!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item { StudySummary(results) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.padding(start = 4.dp))
                        Text("پاک کردن تاریخچه")
                    }
                }
            }
            item {
                OutlinedTextField(value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    label = { Text("جست‌وجو در نام بسته‌ها") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "پاک کردن جست‌وجو")
                    } }, singleLine = true, shape = RoundedCornerShape(16.dp))
                Text("${visibleResults.size.toPersian()} از ${results.size.toPersian()} کارنامه",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "همه زمان‌ها", 7 to "۷ روز", 30 to "۳۰ روز").forEach { (days, title) ->
                        FilterChip(selected = period == days, onClick = { period = days }, label = { Text(title) })
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("جدیدترین", "قدیمی‌ترین", "بیشترین درصد").forEach { title ->
                        FilterChip(selected = order == title, onClick = { order = title }, label = { Text(title) })
                    }
                }
                TextButton(onClick = { query = ""; period = 0; order = "جدیدترین" }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("بازنشانی فیلترها")
                }
            }
            if (visibleResults.isEmpty()) {
                item { Text("کارنامه‌ای با این فیلترها پیدا نشد.", modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(visibleResults, key = { it.id }) { result ->
                ResultCard(result = result, onClick = { onOpenResult(result.id) }, onDelete = { resultToDelete = result })
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    }

    resultToDelete?.let { result ->
        AlertDialog(onDismissRequest = { resultToDelete = null }, title = { Text("حذف این کارنامه؟") },
            text = { Text("کارنامه «${result.packTitle}» و پاسخ‌های ذخیره‌شده آن حذف می‌شوند. علاقه‌مندی‌ها و خریدها تغییری نمی‌کنند.") },
            confirmButton = { TextButton(onClick = { vm.delete(result.id); resultToDelete = null }) { Text("حذف کارنامه") } },
            dismissButton = { TextButton(onClick = { resultToDelete = null }) { Text("انصراف") } })
    }
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("پاک کردن تاریخچه") },
            text = { Text("تمام کارنامه‌های قبلی حذف می‌شود. مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    vm.clearAll()
                }) { Text("حذف همه") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun StudySummary(results: List<ExamResult>) {
    val stats = remember(results) { studyStatistics(results) }
    val week = remember(results) { recentResults(results, 7, System.currentTimeMillis()) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("مسیر مطالعه شما", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("بر اساس کارنامه‌های ذخیره‌شده روی این دستگاه", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryMetric(stats.exams.toPersian(), "آزمون", Modifier.weight(1f))
                SummaryMetric(stats.questions.toString().toPersianDigits(), "سؤال", Modifier.weight(1f))
                SummaryMetric("${stats.accuracyPercent.toPersian()}٪", "پاسخ درست", Modifier.weight(1f))
            }
            Text("۷ روز اخیر: ${week.size.toPersian()} آزمون • ${answeredQuestions(week).toString().toPersianDigits()} پاسخ",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("${answeredQuestions(results).toString().toPersianDigits()} پاسخ ثبت‌شده • زمان کل ${formatClock(stats.durationSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("درصد، مجموع پاسخ‌های درست تقسیم بر کل سؤال‌هاست؛ بی‌پاسخ‌ها هم در کل حساب می‌شوند.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun ResultCard(result: ExamResult, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.packTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatShamsi(result.date)} • ${formatClock(result.spentSeconds)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "درست: ${result.correct.toPersian()} | غلط: ${result.wrong.toPersian()} | بی‌جواب: ${result.blank.toPersian()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "حذف کارنامه ${result.packTitle}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(
                text = "${result.percent.toPersian()}٪",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (result.percent >= 50) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}
