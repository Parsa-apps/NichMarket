package com.parsaapps.nichmarket.ui.screens.exam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.data.model.Question
import com.parsaapps.nichmarket.data.model.QuestionPack
import com.parsaapps.nichmarket.data.model.bookmarkKey
import com.parsaapps.nichmarket.util.formatClock
import com.parsaapps.nichmarket.util.toPersian
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ExamViewModel : ViewModel() {

    private var pack: QuestionPack? = null
    private var startedAt: Long = 0
    private var navigated = false

    val questions: List<Question> get() = pack?.questions.orEmpty()
    val packTitle: String get() = pack?.title.orEmpty()

    val index = MutableStateFlow(0)
    val answers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val remainingSeconds = MutableStateFlow(0)
    val finishedResult = MutableStateFlow<ExamResult?>(null)

    private val _navigateToResult = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateToResult: SharedFlow<String> = _navigateToResult

    val bookmarks = Graph.userStore.bookmarks

    fun load(packId: String) {
        if (pack != null) return
        val p = Graph.repository.findPack(packId) ?: return
        pack = p
        startedAt = System.currentTimeMillis()
        remainingSeconds.value = p.durationMinutes * 60
        viewModelScope.launch {
            while (remainingSeconds.value > 0 && finishedResult.value == null) {
                delay(1_000)
                if (finishedResult.value == null) {
                    remainingSeconds.value = remainingSeconds.value - 1
                    if (remainingSeconds.value <= 0) finish()
                }
            }
        }
    }

    fun select(questionId: Int, optionIndex: Int) {
        answers.value = answers.value + (questionId to optionIndex)
    }

    fun next() {
        if (index.value < questions.lastIndex) index.value = index.value + 1
    }

    fun previous() {
        if (index.value > 0) index.value = index.value - 1
    }

    fun toggleBookmark(questionId: Int) {
        viewModelScope.launch {
            pack?.let { Graph.userStore.toggleBookmark(it.id, questionId) }
        }
    }

    fun finish() {
        val p = pack ?: return
        if (finishedResult.value != null) return
        var correct = 0
        var wrong = 0
        var blank = 0
        p.questions.forEach { q ->
            val a = answers.value[q.id]
            when {
                a == null -> blank++
                a == q.answerIndex -> correct++
                else -> wrong++
            }
        }
        val spent = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
            .coerceAtLeast(0)
        val result = ExamResult(
            id = UUID.randomUUID().toString(),
            packId = p.id,
            packTitle = p.title,
            date = System.currentTimeMillis(),
            correct = correct,
            wrong = wrong,
            blank = blank,
            total = p.questions.size,
            spentSeconds = spent,
            answers = answers.value
        )
        finishedResult.value = result
        viewModelScope.launch {
            Graph.userStore.addResult(result)
            if (!navigated) {
                navigated = true
                _navigateToResult.tryEmit(result.id)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    packId: String,
    onExit: () -> Unit,
    onFinished: (String) -> Unit
) {
    val vm: ExamViewModel = viewModel()
    LaunchedEffect(packId) { vm.load(packId) }

    val questions = vm.questions
    val index by vm.index.collectAsState()
    val answers by vm.answers.collectAsState()
    val remaining by vm.remainingSeconds.collectAsState()
    val bookmarks by vm.bookmarks.collectAsState(initial = emptyList())
    var showQuitDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.navigateToResult.collect { resultId -> onFinished(resultId) }
    }

    if (questions.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("این بسته آزمون پیدا نشد.")
        }
        return
    }

    val question = questions[index.coerceIn(0, questions.lastIndex)]
    val answeredCount = answers.size
    val isLast = index == questions.lastIndex
    val timeCritical = remaining in 1..60

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = vm.packTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "سوال ${(index + 1).toPersian()} از ${questions.size.toPersian()}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showQuitDialog = true }) {
                        Icon(Icons.Filled.Close, contentDescription = "خروج")
                    }
                },
                actions = {
                    Text(
                        text = formatClock(remaining),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (timeCritical) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            LinearProgressIndicator(
                progress = { questions.take(index + 1).count { it.id in answers }.toFloat() / questions.size },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                QuestionCard(
                    question = question,
                    selected = answers[question.id],
                    bookmarked = bookmarkKey(packId, question.id) in bookmarks,
                    onBookmark = { vm.toggleBookmark(question.id) },
                    onSelect = { option -> vm.select(question.id, option) }
                )
                Spacer(Modifier.height(24.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = vm::previous,
                    enabled = index > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("قبلی")
                }
                Button(
                    onClick = { if (isLast) showFinishDialog = true else vm.next() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (isLast) "اتمام آزمون" else "بعدی")
                }
            }
            Text(
                text = "$answeredCount سوال پاسخ داده شد",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
            )
        }
    }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("خروج از آزمون") },
            text = { Text("با خروج از آزمون، پاسخ‌های شما ذخیره نمی‌شود. مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = { onExit() }) { Text("بله، خروج") }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) { Text("ادامه آزمون") }
            }
        )
    }
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("اتمام آزمون") },
            text = {
                val blank = questions.count { it.id !in answers }
                Text(
                    if (blank > 0)
                        "به $blank سوال پاسخ نداده‌اید. آزمون را پایان می‌دهید؟"
                    else "آزمون را پایان می‌دهید؟"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    vm.finish()
                }) { Text("پایان و مشاهده نتیجه") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("ادامه آزمون") }
            }
        )
    }
}

@Composable
private fun QuestionCard(
    question: Question,
    selected: Int?,
    bookmarked: Boolean,
    onBookmark: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onBookmark) {
                    Icon(
                        imageVector = if (bookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "ذخیره در علاقه‌مندی‌ها",
                        tint = if (bookmarked) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            question.options.forEachIndexed { i, option ->
                val isSelected = selected == i
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { onSelect(i) })
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
