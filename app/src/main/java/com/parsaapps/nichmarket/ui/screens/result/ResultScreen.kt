package com.parsaapps.nichmarket.ui.screens.result

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.data.model.Question
import com.parsaapps.nichmarket.data.model.bookmarkKey
import com.parsaapps.nichmarket.ui.components.StatCard
import com.parsaapps.nichmarket.ui.theme.CorrectGreen
import com.parsaapps.nichmarket.ui.theme.WrongRed
import com.parsaapps.nichmarket.util.formatClock
import com.parsaapps.nichmarket.util.formatShamsi
import com.parsaapps.nichmarket.util.toPersian
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResultViewModel : ViewModel() {
    val results = Graph.userStore.results
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val bookmarks = Graph.userStore.bookmarks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleBookmark(packId: String, questionId: Int) {
        viewModelScope.launch { Graph.userStore.toggleBookmark(packId, questionId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(resultId: String, onBack: () -> Unit) {
    val vm: ResultViewModel = viewModel()
    val results by vm.results.collectAsState()
    val bookmarks by vm.bookmarks.collectAsState()
    val result = remember(resultId, results) { results.firstOrNull { it.id == resultId } }

    LaunchedEffect(resultId) {
        if (results.isNotEmpty() && results.none { it.id == resultId }) onBack()
    }

    if (result == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("در حال بارگذاری نتیجه…")
        }
        return
    }

    val pack = remember(result.packId) { Graph.repository.findPack(result.packId) }
    val answers = remember(result.id) { result.answers }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("کارنامه آزمون") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScoreCard(result) }
            item {
                Text(
                    text = "مرور سوالات",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (pack != null) {
                items(pack.questions, key = { it.id }) { question ->
                    val userAnswer = answers[question.id]
                    ReviewCard(
                        number = pack.questions.indexOf(question) + 1,
                        question = question,
                        userAnswer = userAnswer,
                        bookmarked = bookmarkKey(pack.id, question.id) in bookmarks,
                        onBookmark = { vm.toggleBookmark(pack.id, question.id) }
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ScoreCard(result: ExamResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${result.percent.toPersian()}٪",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${result.correct.toPersian()} از ${result.total.toPersian()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
            Text(
                text = result.packTitle,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = "تاریخ: ${formatShamsi(result.date)} • مدت: ${formatClock(result.spentSeconds)}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("درست", result.correct, CorrectGreen, Modifier.weight(1f))
                StatCard("غلط", result.wrong, WrongRed, Modifier.weight(1f))
                StatCard("بی‌جواب", result.blank, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReviewCard(
    number: Int,
    question: Question,
    userAnswer: Int?,
    bookmarked: Boolean,
    onBookmark: () -> Unit
) {
    var expanded by remember(question.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${number.toPersian()}. ${question.text}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onBookmark) {
                    Icon(
                        imageVector = if (bookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "ذخیره",
                        tint = if (bookmarked) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                question.options.forEachIndexed { i, option ->
                    val isCorrect = i == question.answerIndex
                    val isUserPick = userAnswer == i
                    ReviewOptionRow(
                        option = option,
                        isCorrect = isCorrect,
                        isUserPick = isUserPick,
                        answered = userAnswer != null
                    )
                }
                if (question.explanation != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "پاسخ تشریحی: ${question.explanation}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewOptionRow(
    option: String,
    isCorrect: Boolean,
    isUserPick: Boolean,
    answered: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                when {
                    isCorrect -> CorrectGreen.copy(alpha = 0.12f)
                    isUserPick -> WrongRed.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                RoundedCornerShape(10.dp)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isCorrect) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = CorrectGreen,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Box(modifier = Modifier.size(18.dp))
        }
        Text(
            text = option,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isCorrect -> CorrectGreen
                isUserPick && !isCorrect -> WrongRed
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isCorrect || isUserPick) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
