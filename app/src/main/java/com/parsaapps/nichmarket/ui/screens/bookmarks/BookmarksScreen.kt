package com.parsaapps.nichmarket.ui.screens.bookmarks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.parsaapps.nichmarket.data.analysis.matchesPersianSearch
import com.parsaapps.nichmarket.data.model.Question
import com.parsaapps.nichmarket.ui.components.GradientHeader
import com.parsaapps.nichmarket.util.toPersian
import kotlinx.coroutines.launch

private data class BookmarkItem(
    val key: String,
    val legacy: Boolean,
    val packId: String,
    val packTitle: String,
    val question: Question
)

class BookmarksViewModel : ViewModel() {
    val bookmarks = Graph.userStore.bookmarks

    fun toggle(packId: String, questionId: Int, legacy: Boolean) {
        viewModelScope.launch { Graph.userStore.toggleBookmark(packId, questionId, legacy) }
    }
}

@Composable
fun BookmarksScreen() {
    val vm: BookmarksViewModel = viewModel()
    val bookmarks by vm.bookmarks.collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var selectedSubject by rememberSaveable { mutableStateOf("") }
    var selectedPack by rememberSaveable { mutableStateOf("") }
    var selectedDifficulty by rememberSaveable { mutableStateOf("") }
    var cards by remember { mutableStateOf<List<BookmarkItem>?>(null) }

    val savedItems = remember(bookmarks) {
        bookmarks.mapNotNull { key ->
            val parts = key.split(":")
            val legacy = parts.size == 2
            if (!legacy && (parts.size != 3 || parts[0] != "v2")) return@mapNotNull null
            val packId = parts[if (legacy) 0 else 1]
            val qId = parts[if (legacy) 1 else 2].toIntOrNull() ?: return@mapNotNull null
            val pack = (if (legacy) Graph.repository.findLegacyPack(packId)
                        else Graph.repository.findPack(packId)) ?: return@mapNotNull null
            val q = pack.questions.firstOrNull { it.id == qId } ?: return@mapNotNull null
            BookmarkItem(key, legacy, pack.id, pack.title, q)
        }
    }
    val subjects = remember(savedItems) { savedItems.map { it.question.subject }.filter { it.isNotBlank() }.distinct().sorted() }
    val packs = remember(savedItems) { savedItems.map { it.packId to it.packTitle }.distinctBy { it.first }.sortedBy { it.second } }
    val difficulties = remember(savedItems) { savedItems.map { it.question.difficulty }.filter { it.isNotBlank() }.distinct().sorted() }
    val visibleItems = remember(savedItems, query, selectedSubject, selectedPack, selectedDifficulty) {
        savedItems.filter { item ->
            (selectedSubject.isEmpty() || item.question.subject == selectedSubject) &&
                (selectedPack.isEmpty() || item.packId == selectedPack) &&
                (selectedDifficulty.isEmpty() || item.question.difficulty == selectedDifficulty) &&
                matchesPersianSearch(query, item.packTitle, item.question.subject,
                    item.question.text, item.question.options.joinToString(" "), item.question.explanation.orEmpty())
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            GradientHeader("علاقه‌مندی‌ها", "مرور سؤال‌های ذخیره‌شده، حتی بدون اینترنت")
            Spacer(Modifier.height(12.dp))
        }
        if (savedItems.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    label = { Text("جست‌وجو در سؤال‌ها و درس‌ها") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "پاک کردن جست‌وجو")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedSubject.isEmpty(), onClick = { selectedSubject = "" },
                        label = { Text("همه درس‌ها") })
                    subjects.forEach { subject ->
                        FilterChip(selected = selectedSubject == subject, onClick = { selectedSubject = subject },
                            label = { Text(subject) })
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedPack.isEmpty(), onClick = { selectedPack = "" }, label = { Text("همه بسته‌ها") })
                    packs.forEach { (id, title) ->
                        FilterChip(selected = selectedPack == id, onClick = { selectedPack = id }, label = { Text(title) })
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedDifficulty.isEmpty(), onClick = { selectedDifficulty = "" }, label = { Text("همه سطح‌ها") })
                    difficulties.forEach { difficulty ->
                        FilterChip(selected = selectedDifficulty == difficulty, onClick = { selectedDifficulty = difficulty },
                            label = { Text(difficulty) })
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { cards = visibleItems.toList() }, enabled = visibleItems.isNotEmpty()) { Text("مرور فلش‌کارتی") }
                    TextButton(onClick = { query = ""; selectedSubject = ""; selectedPack = ""; selectedDifficulty = "" }) {
                        Text("پاک کردن فیلترها")
                    }
                }
                Text(
                    text = "${visibleItems.size.toPersian()} از ${savedItems.size.toPersian()} سؤال ذخیره‌شده",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (savedItems.isEmpty()) {
            item {
                EmptyState("هنوز سؤالی ذخیره نکرده‌اید.\nهنگام آزمون با آیکن قلب، سؤال‌های مهم را ذخیره کنید.")
            }
        } else if (visibleItems.isEmpty()) {
            item { EmptyState("نتیجه‌ای پیدا نشد. عبارت یا درس دیگری را انتخاب کنید.") }
        } else {
            items(visibleItems, key = { it.key }) { item ->
                BookmarkCard(item = item, onRemove = { vm.toggle(item.packId, item.question.id, item.legacy) })
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    cards?.let { FlashcardDialog(it, onClose = { cards = null }) }
}

/** A local, temporary review session; answers never alter the exam history. */
@Composable
private fun FlashcardDialog(initial: List<BookmarkItem>, onClose: () -> Unit) {
    var order by remember(initial) { mutableStateOf(initial) }
    var position by remember(initial) { mutableStateOf(0) }
    var revealed by remember(initial) { mutableStateOf(false) }
    val item = order.getOrNull(position) ?: return
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("مرور فلش‌کارتی • ${(position + 1).toPersian()} از ${order.size.toPersian()}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.packTitle + if (item.legacy) " • نسخه قبلی" else "", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                if (item.question.subject.isNotBlank()) Text(item.question.subject, style = MaterialTheme.typography.labelMedium)
                Text(item.question.text, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { revealed = !revealed }) { Text(if (revealed) "پنهان کردن پاسخ" else "نمایش پاسخ") }
                if (revealed) {
                    item.question.options.forEachIndexed { index, option ->
                        Text("${(index + 1).toPersian()}. $option" + if (index == item.question.answerIndex) " ✓" else "",
                            color = if (index == item.question.answerIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    item.question.explanation?.takeIf { it.isNotBlank() }?.let { Text("توضیح: $it") }
                    if (item.legacy) Text("محتوای نسخه قبلی است و ممکن است اصلاح شده باشد.", style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { position--; revealed = false }, enabled = position > 0, modifier = Modifier.weight(1f)) { Text("قبلی") }
                    Button(onClick = { position++; revealed = false }, enabled = position < order.lastIndex, modifier = Modifier.weight(1f)) { Text("بعدی") }
                }
                TextButton(onClick = { order = order.shuffled(); position = 0; revealed = false }, enabled = order.size > 1) { Text("ترتیب تصادفی") }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("پایان مرور") } }
    )
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.FavoriteBorder, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BookmarkCard(item: BookmarkItem, onRemove: () -> Unit) {
    var expanded by rememberSaveable(item.key) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.packTitle + if (item.legacy) " (نسخه قبلی)" else "",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (item.question.subject.isNotBlank()) {
                        Text(item.question.subject, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Favorite, contentDescription = "حذف از علاقه‌مندی‌ها",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
            Text(item.question.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "پنهان کردن پاسخ" else "نمایش پاسخ و توضیح")
            }
            if (expanded) {
                Text("پاسخ صحیح: ${item.question.options[item.question.answerIndex]}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                item.question.options.forEachIndexed { index, option ->
                    Text("${(index + 1).toPersian()}. $option", style = MaterialTheme.typography.bodyMedium,
                        color = if (index == item.question.answerIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item.question.explanation?.takeIf { it.isNotBlank() }?.let {
                    Text("توضیح: $it", style = MaterialTheme.typography.bodyMedium)
                }
                if (item.legacy) {
                    Text("محتوای نسخه قبلی است و ممکن است در بانک جدید اصلاح شده باشد.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
