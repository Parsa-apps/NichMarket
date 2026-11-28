package com.parsaapps.nichmarket.ui.screens.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.data.analysis.*
import kotlinx.coroutines.launch

class ReviewViewModel : ViewModel() {
    val items = Graph.userStore.reviewItems
    fun grade(item: ReviewItem, grade: ReviewGrade) {
        viewModelScope.launch { Graph.userStore.saveReviewItem(scheduleReview(item, grade, System.currentTimeMillis())) }
    }
    fun remove(item: ReviewItem) {
        viewModelScope.launch { Graph.userStore.removeReviewItem(item.key) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(onBack: () -> Unit) {
    val vm: ReviewViewModel = viewModel()
    val all by vm.items.collectAsState(initial = emptyList())
    val due = dueReviewItems(all, System.currentTimeMillis())
    Scaffold(topBar = {
        TopAppBar(title = { Text("مرور هوشمند") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("بازگشت") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(due.size.toString() + " سؤال آماده مرور", style = MaterialTheme.typography.titleLarge)
                        Text("سؤال‌های غلط و بی‌پاسخ آزمون‌ها خودکار ذخیره می‌شوند و بر اساس زمان مرور مرتب هستند.",
                            style = MaterialTheme.typography.bodySmall)
                        Text(all.size.toString() + " سؤال در صف مرور", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (due.isEmpty()) item {
                Text(if (all.isEmpty()) "بعد از پایان آزمون، سؤال‌های اشتباه و بی‌پاسخ اینجا قرار می‌گیرند."
                    else "عالیه! فعلاً سؤال سررسیدشده‌ای نداری.", modifier = Modifier.padding(vertical = 24.dp))
            }
            items(due, key = { it.key }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(item.packTitle, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        if (item.question.subject.isNotBlank()) Text(item.question.subject, style = MaterialTheme.typography.labelSmall)
                        Text(item.question.text, style = MaterialTheme.typography.titleMedium)
                        item.question.options.forEachIndexed { index, option ->
                            Text((index + 1).toString() + ". " + option,
                                color = if (index == item.question.answerIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        Text("پاسخ صحیح: " + item.question.options.getOrNull(item.question.answerIndex).orEmpty(),
                            color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        item.question.explanation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Text("تعداد خطا: " + item.lapses, style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { vm.grade(item, ReviewGrade.AGAIN) }) { Text("دوباره") }
                            OutlinedButton(onClick = { vm.grade(item, ReviewGrade.HARD) }) { Text("سخت") }
                            Button(onClick = { vm.grade(item, ReviewGrade.GOOD) }) { Text("بلدم") }
                        }
                        TextButton(onClick = { vm.remove(item) }) { Text("حذف از صف مرور") }
                    }
                }
            }
        }
    }
}
