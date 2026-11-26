package com.parsaapps.nichmarket.ui.screens.result

import android.content.Intent
import android.widget.Toast

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.data.analysis.AnswerStatus
import com.parsaapps.nichmarket.data.analysis.answerStatus
import com.parsaapps.nichmarket.data.analysis.matchesReviewQuestion
import com.parsaapps.nichmarket.data.analysis.comparablePrevious
import com.parsaapps.nichmarket.data.analysis.subjectPerformance
import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.data.model.Question
import com.parsaapps.nichmarket.data.model.bookmarkKey
import com.parsaapps.nichmarket.ui.components.StatCard
import com.parsaapps.nichmarket.util.formatClock
import com.parsaapps.nichmarket.util.formatShamsi
import com.parsaapps.nichmarket.util.toPersian
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResultViewModel : ViewModel() {
    val results = Graph.userStore.results
    val bookmarks = Graph.userStore.bookmarks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleBookmark(packId: String, questionId: Int, legacy: Boolean) {
        viewModelScope.launch { Graph.userStore.toggleBookmark(packId, questionId, legacy) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(resultId: String, onBack: () -> Unit) {
    val vm: ResultViewModel = viewModel()
    val context = LocalContext.current
    val results by vm.results.collectAsState(initial = null)
    val bookmarks by vm.bookmarks.collectAsState()
    val result = remember(resultId, results) { results?.firstOrNull { it.id == resultId } }

    if (result == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (results == null) "در حال بارگذاری نتیجه…" else "این کارنامه پیدا نشد یا حذف شده است.")
            if (results != null) TextButton(onClick = onBack) { Text("بازگشت") }
        }
        return
    }

    val legacy = result.questionSnapshot == null
    val pack = remember(result.packId, legacy) {
        if (legacy) Graph.repository.findLegacyPack(result.packId)
        else Graph.repository.findPack(result.packId)
    }
    val reviewQuestions = result.questionSnapshot ?: pack?.questions.orEmpty()
    val answers = remember(result.id) { result.answers }
    val subjectStats = remember(reviewQuestions, answers) { subjectPerformance(reviewQuestions, answers) }
    val previous = remember(result, results) { comparablePrevious(result, results.orEmpty()) }
    var reviewFilter by rememberSaveable(result.id) { mutableStateOf("همه") }
    var selectedSubject by rememberSaveable(result.id) { mutableStateOf("") }
    var query by rememberSaveable(result.id) { mutableStateOf("") }
    var difficulty by rememberSaveable(result.id) { mutableStateOf("") }
    var onlyBookmarks by rememberSaveable(result.id) { mutableStateOf(false) }
    val difficulties = remember(reviewQuestions) { reviewQuestions.map { it.difficulty }.filter { it.isNotBlank() }.distinct().sorted() }
    val filteredQuestions = remember(reviewQuestions, answers, reviewFilter, selectedSubject, query, difficulty, onlyBookmarks, bookmarks, pack) {
        val status = when (reviewFilter) {
            "نادرست" -> AnswerStatus.WRONG
            "بی‌پاسخ" -> AnswerStatus.BLANK
            "درست" -> AnswerStatus.CORRECT
            else -> null
        }
        reviewQuestions.filter { question ->
            val bookmarkAvailable = legacy || pack?.questions?.any { it == question } == true
            val key = if (legacy) "${result.packId}:${question.id}" else bookmarkKey(result.packId, question.id)
            matchesReviewQuestion(question, answers, status, selectedSubject, query) &&
                (reviewFilter != "نیاز به مرور" || answerStatus(question, answers) != AnswerStatus.CORRECT) &&
                (difficulty.isEmpty() || question.difficulty == difficulty) &&
                (!onlyBookmarks || (bookmarkAvailable && key in bookmarks))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("کارنامه آزمون") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val summary = "نتیجه تمرین در نیچ مارکت\n${result.packTitle}\n" +
                            "درست: ${result.correct.toPersian()} از ${result.total.toPersian()} • ${result.percent.toPersian()}٪\n" +
                            "بدون نمره منفی"
                        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, summary)
                        try {
                            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری خلاصه کارنامه"))
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, "برنامه‌ای برای اشتراک‌گذاری پیدا نشد.", Toast.LENGTH_SHORT).show()
                        }
                    }) { Icon(Icons.Filled.Share, contentDescription = "اشتراک‌گذاری خلاصه کارنامه") }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScoreCard(result) }
            item {
                val answered = (result.correct + result.wrong).coerceAtLeast(0)
                Text(if (answered > 0) "میانگین زمان هر پاسخ: ${formatClock(result.spentSeconds.coerceAtLeast(0) / answered)} • زمان کل تقسیم بر تعداد پاسخ‌ها"
                    else "در این آزمون پاسخی ثبت نشده است.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            previous?.let { older ->
                item {
                    val delta = result.percent - older.percent
                    val description = when {
                        delta > 0 -> "${delta.toPersian()} واحد درصد بیشتر"
                        delta < 0 -> "${(-delta).toPersian()} واحد درصد کمتر"
                        else -> "بدون تغییر"
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("مقایسه با آزمون مشابه", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("$description نسبت به ${formatShamsi(older.date)}؛ مجموعه سؤال‌ها یکسان است.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
            if (subjectStats.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("عملکرد به تفکیک درس", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("درس‌های با نسبت پاسخ درست کمتر در ابتدا هستند. برای فیلتر مرور روی یک درس بزنید؛ سؤال بی‌پاسخ هم در تعداد کل است.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            subjectStats.forEach { (subject, correct, total) ->
                                Row(Modifier.fillMaxWidth().clickable {
                                    selectedSubject = if (selectedSubject == subject) "" else subject
                                }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(subject + if (selectedSubject == subject) " ✓" else "", style = MaterialTheme.typography.labelLarge)
                                    Text("${correct.toPersian()} از ${total.toPersian()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                LinearProgressIndicator(progress = { correct.toFloat() / total },
                                    modifier = Modifier.fillMaxWidth(),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }
            if (legacy) {
                item {
                    Text(
                        "این کارنامه از بانک نسخه قبلی استفاده می‌کند؛ محتوای تاریخی ممکن است در نسخه جدید اصلاح شده باشد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Text(
                    text = "مرور سوالات",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("همه", "نیاز به مرور", "نادرست", "بی‌پاسخ", "درست").forEach { filter ->
                        FilterChip(
                            selected = reviewFilter == filter,
                            onClick = { reviewFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }
            item {
                OutlinedTextField(value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("جست‌وجو در این کارنامه") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "پاک کردن جست‌وجو")
                    } }, singleLine = true, shape = RoundedCornerShape(16.dp))
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedSubject.isEmpty(), onClick = { selectedSubject = "" },
                        label = { Text("همه درس‌ها") })
                    subjectStats.forEach { stat ->
                        FilterChip(selected = selectedSubject == stat.subject, onClick = { selectedSubject = stat.subject },
                            label = { Text(stat.subject) })
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = difficulty.isEmpty(), onClick = { difficulty = "" }, label = { Text("همه سطح‌ها") })
                    difficulties.forEach { level ->
                        FilterChip(selected = difficulty == level, onClick = { difficulty = level }, label = { Text(level) })
                    }
                    FilterChip(selected = onlyBookmarks, onClick = { onlyBookmarks = !onlyBookmarks }, label = { Text("فقط علاقه‌مندی‌ها") })
                }
                TextButton(onClick = { reviewFilter = "همه"; selectedSubject = ""; query = ""; difficulty = ""; onlyBookmarks = false }) {
                    Text("پاک کردن فیلترها")
                }
                Text("${filteredQuestions.size.toPersian()} از ${reviewQuestions.size.toPersian()} سؤال",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (filteredQuestions.isEmpty()) {
                item {
                    Text("سؤالی با این فیلترها پیدا نشد.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
            } else {
                items(filteredQuestions, key = { it.id }) { question ->
                    val userAnswer = answers[question.id]
                    ReviewCard(
                        number = reviewQuestions.indexOf(question) + 1,
                        question = question,
                        userAnswer = userAnswer,
                        bookmarked = (legacy || pack?.questions?.any { it == question } == true) &&
                            (if (legacy) "${result.packId}:${question.id}" else bookmarkKey(result.packId, question.id)) in bookmarks,
                        canBookmark = legacy || pack?.questions?.any { it == question } == true,
                        onBookmark = { vm.toggleBookmark(result.packId, question.id, legacy) }
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
            containerColor = Color(0xFF164F50)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("خلاصه عملکرد", style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFEFD09A), fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier.size(126.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { (result.percent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(), color = Color(0xFFE9CC91),
                    trackColor = Color.White.copy(alpha = .18f), strokeWidth = 8.dp
                )
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
                StatCard("درست", result.correct, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StatCard("غلط", result.wrong, MaterialTheme.colorScheme.error, Modifier.weight(1f))
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
    canBookmark: Boolean,
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (question.subject.isNotBlank()) {
                Text(
                    text = if (question.difficulty.isNotBlank()) {
                        "${question.subject} • ${question.difficulty}"
                    } else {
                        question.subject
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
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
                IconButton(onClick = onBookmark, enabled = canBookmark) {
                    Icon(
                        imageVector = if (bookmarked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (bookmarked) "حذف از علاقه‌مندی‌ها" else "ذخیره در علاقه‌مندی‌ها",
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
            Text(
                text = when {
                    userAnswer == null -> "بی‌پاسخ"
                    userAnswer == question.answerIndex -> "پاسخ درست"
                    else -> "پاسخ نادرست"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    userAnswer == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    userAnswer == question.answerIndex -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(top = 6.dp)
            )
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
                    isCorrect -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    isUserPick -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Box(modifier = Modifier.size(18.dp))
        }
        Text(
            text = option,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isCorrect -> MaterialTheme.colorScheme.primary
                isUserPick && !isCorrect -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isCorrect || isUserPick) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
