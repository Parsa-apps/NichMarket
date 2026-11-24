package com.parsaapps.nichmarket.ui.screens.results

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.ui.components.GradientHeader
import com.parsaapps.nichmarket.util.formatClock
import com.parsaapps.nichmarket.util.formatShamsi
import com.parsaapps.nichmarket.util.toPersian
import kotlinx.coroutines.launch

class ResultsViewModel : ViewModel() {
    val results = Graph.userStore.results

    fun clearAll() {
        viewModelScope.launch { Graph.userStore.clearResults() }
    }
}

@Composable
fun ResultsScreen(onOpenResult: (String) -> Unit) {
    val vm: ResultsViewModel = viewModel()
    val results by vm.results.collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
            items(results, key = { it.id }) { result ->
                ResultCard(result = result, onClick = { onOpenResult(result.id) })
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
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
private fun ResultCard(result: ExamResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
