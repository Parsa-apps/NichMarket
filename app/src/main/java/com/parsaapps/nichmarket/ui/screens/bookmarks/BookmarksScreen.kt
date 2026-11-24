package com.parsaapps.nichmarket.ui.screens.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.data.model.Question
import com.parsaapps.nichmarket.ui.components.GradientHeader
import kotlinx.coroutines.launch

private data class BookmarkItem(
    val packId: String,
    val packTitle: String,
    val question: Question
)

class BookmarksViewModel : ViewModel() {
    val bookmarks = Graph.userStore.bookmarks

    fun toggle(packId: String, questionId: Int) {
        viewModelScope.launch { Graph.userStore.toggleBookmark(packId, questionId) }
    }
}

@Composable
fun BookmarksScreen() {
    val vm: BookmarksViewModel = viewModel()
    val bookmarks by vm.bookmarks.collectAsState(initial = emptyList())

    val items = remember(bookmarks) {
        bookmarks.mapNotNull { key ->
            val parts = key.split(":")
            if (parts.size != 2) return@mapNotNull null
            val pack = Graph.repository.findPack(parts[0]) ?: return@mapNotNull null
            val qId = parts[1].toIntOrNull() ?: return@mapNotNull null
            val q = pack.questions.firstOrNull { it.id == qId } ?: return@mapNotNull null
            BookmarkItem(pack.id, pack.title, q)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            GradientHeader(
                title = "علاقه‌مندی‌ها",
                subtitle = "سوال‌هایی که هنگام آزمون ذخیره کرده‌اید"
            )
            Spacer(Modifier.height(8.dp))
        }
        if (items.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "هنوز سوالی ذخیره نکرده‌اید.\nهنگام آزمون با آیکن قلب، سوال‌های مهم را ذخیره کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(items, key = { "${it.packId}:${it.question.id}" }) { item ->
                BookmarkCard(item = item, onRemove = { vm.toggle(item.packId, item.question.id) })
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun BookmarkCard(item: BookmarkItem, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.packTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.question.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "پاسخ صحیح: ${item.question.options[item.question.answerIndex]}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
