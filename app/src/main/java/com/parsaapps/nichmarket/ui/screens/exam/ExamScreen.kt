package com.parsaapps.nichmarket.ui.screens.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.data.analysis.examSession
import com.parsaapps.nichmarket.data.analysis.elapsedExamSeconds
import com.parsaapps.nichmarket.data.analysis.nextQuestionIndex
import com.parsaapps.nichmarket.data.model.ActiveExam
import com.parsaapps.nichmarket.data.model.AppSettings
import com.parsaapps.nichmarket.data.model.ExamResult
import com.parsaapps.nichmarket.data.model.Question
import com.parsaapps.nichmarket.data.model.QuestionPack
import com.parsaapps.nichmarket.data.model.bookmarkKey
import com.parsaapps.nichmarket.util.formatClock
import com.parsaapps.nichmarket.util.toPersian
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class ExamViewModel : ViewModel() {
    private var draft: ActiveExam? = null
    private var requestedPackId = ""
    private var requestedLimit = 0
    private var navigated = false
    private var saving = false
    private var discarding = false

    val questions: List<Question> get() = draft?.pack?.questions.orEmpty()
    val packTitle: String get() = draft?.pack?.title.orEmpty()
    val activePackId: String get() = draft?.pack?.id.orEmpty()
    val index = MutableStateFlow(0)
    val answers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val markedQuestionIds = MutableStateFlow<Set<Int>>(emptySet())
    val remainingSeconds = MutableStateFlow(0)
    val finishedResult = MutableStateFlow<ExamResult?>(null)
    val saveFailed = MutableStateFlow(false)
    val draftSaveFailed = MutableStateFlow(false)
    val loading = MutableStateFlow(true)
    val busy = MutableStateFlow(false)
    val loadError = MutableStateFlow<String?>(null)
    val pendingExam = MutableStateFlow<ActiveExam?>(null)
    private val _navigateToResult = MutableStateFlow<String?>(null)
    val navigateToResult = _navigateToResult
    val bookmarks = Graph.userStore.bookmarks
    val settings = Graph.userStore.settings

    fun load(packId: String, limit: Int) {
        if (draft != null || pendingExam.value != null) return
        requestedPackId = packId
        requestedLimit = limit
        viewModelScope.launch {
            loading.value = true
            loadError.value = null
            try {
                val existing = Graph.userStore.activeExam.first()
                if (existing != null) {
                    if (limit == -1) activate(existing) else pendingExam.value = existing
                } else if (limit == -1) {
                    loadError.value = "آزمون ناتمامی برای ادامه وجود ندارد."
                } else {
                    createExam(null)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SecurityException) {
                loadError.value = "این بسته فعال نیست. خریدها را بازیابی کنید یا از بسته رایگان شروع کنید."
            } catch (e: Exception) {
                loadError.value = "بارگذاری آزمون ناموفق بود. فضای ذخیره‌سازی را بررسی و دوباره تلاش کنید."
            } finally {
                loading.value = false
            }
        }
    }

    fun continuePending() {
        pendingExam.value?.let {
            pendingExam.value = null
            activate(it)
        }
    }

    fun replacePending() {
        val existing = pendingExam.value ?: return
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try {
                createExam(existing.sessionId)
                pendingExam.value = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SecurityException) {
                loadError.value = "این بسته فعال نیست. آزمون قبلی همچنان روی دستگاه است."
            } catch (e: Exception) {
                loadError.value = "شروع آزمون جدید ناموفق بود. آزمون قبلی همچنان روی دستگاه است."
            } finally {
                busy.value = false
            }
        }
    }

    private suspend fun createExam(expectedSessionId: String?) {
        val source = Graph.repository.findPack(requestedPackId)
            ?: throw IllegalStateException("Question pack not found")
        if (source.sku != null && source.sku !in Graph.userStore.purchasedSkus.first()) {
            throw SecurityException("Package is not purchased")
        }
        val p = examSession(source, requestedLimit)
        require(p.questions.isNotEmpty() && p.durationMinutes > 0)
        val newDraft = ActiveExam(
            sessionId = UUID.randomUUID().toString(), pack = p,
            startedAtEpochMs = System.currentTimeMillis(),
            startedAtElapsedMs = android.os.SystemClock.elapsedRealtime(),
            bootCount = Graph.userStore.bootCount()
        )
        check(Graph.userStore.beginExam(newDraft, expectedSessionId))
        activate(newDraft)
    }

    private fun activate(value: ActiveExam) {
        if (value.pack.questions.isEmpty() || value.pack.durationMinutes <= 0) {
            loadError.value = "آزمون ذخیره‌شده معتبر نیست."
            return
        }
        draft = value
        index.value = value.index.coerceIn(0, value.pack.questions.lastIndex)
        answers.value = value.answers
        markedQuestionIds.value = value.markedQuestionIds
        draftSaveFailed.value = false
        refreshClock()
        viewModelScope.launch {
            while (remainingSeconds.value > 0 && finishedResult.value == null) {
                delay(1_000)
                refreshClock()
            }
        }
    }

    private fun refreshClock() {
        if (discarding) return
        val current = draft ?: return
        val spent = elapsedExamSeconds(current, System.currentTimeMillis(), android.os.SystemClock.elapsedRealtime(), Graph.userStore.bootCount())
        remainingSeconds.value = (current.pack.durationMinutes * 60 - spent).coerceAtLeast(0)
        if (remainingSeconds.value == 0 && finishedResult.value == null) finish()
    }

    private fun acceptAnswerChanges(): Boolean {
        if (finishedResult.value != null || draft == null || busy.value) return false
        refreshClock()
        return finishedResult.value == null
    }

    private fun saveProgress() {
        val old = draft ?: return
        val next = old.copy(answers = answers.value, index = index.value, revision = old.revision + 1,
            markedQuestionIds = markedQuestionIds.value)
        draft = next
        viewModelScope.launch {
            try {
                check(Graph.userStore.updateExam(next))
                if (draft?.revision == next.revision) draftSaveFailed.value = false
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (draft?.revision == next.revision && finishedResult.value == null) draftSaveFailed.value = true
            }
        }
    }

    fun retryProgress() = saveProgress()

    fun select(questionId: Int, optionIndex: Int, autoAdvance: Boolean) {
        if (!acceptAnswerChanges()) return
        val question = questions.firstOrNull { it.id == questionId } ?: return
        if (optionIndex in question.options.indices) {
            answers.value = answers.value + (questionId to optionIndex)
            if (autoAdvance && index.value < questions.lastIndex && questions[index.value].id == questionId) {
                index.value += 1
            }
            saveProgress()
        }
    }

    fun clearAnswer(questionId: Int) {
        if (acceptAnswerChanges()) {
            answers.value = answers.value - questionId
            saveProgress()
        }
    }

    fun goTo(position: Int) {
        if (position in questions.indices && position != index.value && acceptAnswerChanges()) {
            index.value = position
            saveProgress()
        }
    }
    fun next() = goTo(index.value + 1)
    fun previous() = goTo(index.value - 1)

    fun toggleMark(questionId: Int) {
        if (!acceptAnswerChanges() || questions.none { it.id == questionId }) return
        markedQuestionIds.value = if (questionId in markedQuestionIds.value)
            markedQuestionIds.value - questionId else markedQuestionIds.value + questionId
        saveProgress()
    }

    fun goToNextUnanswered() {
        nextQuestionIndex(questions, index.value) { it.id !in answers.value }?.let(::goTo)
    }

    fun goToNextMarked() {
        nextQuestionIndex(questions, index.value) { it.id in markedQuestionIds.value }?.let(::goTo)
    }

    fun toggleBookmark(questionId: Int) {
        viewModelScope.launch { draft?.let { Graph.userStore.toggleBookmark(it.pack.id, questionId) } }
    }

    fun exitKeepingDraft(onExit: () -> Unit) {
        if (busy.value || finishedResult.value != null) return
        val current = draft ?: return
        busy.value = true
        viewModelScope.launch {
            try {
                check(Graph.userStore.updateExam(current))
                draftSaveFailed.value = false
                refreshClock()
                if (finishedResult.value == null) onExit()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                draftSaveFailed.value = true
            } finally {
                busy.value = false
            }
        }
    }

    fun discardAndExit(onExit: () -> Unit) {
        if (busy.value || finishedResult.value != null) return
        val current = draft ?: return
        busy.value = true
        discarding = true
        viewModelScope.launch {
            try {
                Graph.userStore.discardExam(current.sessionId)
                if (finishedResult.value == null) onExit()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                draftSaveFailed.value = true
                discarding = false
                refreshClock()
            } finally {
                discarding = false
                busy.value = false
            }
        }
    }

    fun finish() {
        val current = draft ?: return
        if (finishedResult.value != null) return
        val p = current.pack
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
        val result = ExamResult(
            id = current.sessionId, packId = p.id, packTitle = p.title,
            date = System.currentTimeMillis(), correct = correct, wrong = wrong,
            blank = blank, total = p.questions.size,
            spentSeconds = elapsedExamSeconds(current, System.currentTimeMillis(), android.os.SystemClock.elapsedRealtime(), Graph.userStore.bootCount()),
            answers = answers.value, questionSnapshot = p.questions
        )
        finishedResult.value = result
        retrySaveResult()
    }

    fun retrySaveResult() {
        val result = finishedResult.value ?: return
        val sessionId = draft?.sessionId ?: return
        if (saving || navigated) return
        saving = true
        saveFailed.value = false
        viewModelScope.launch {
            try {
                Graph.userStore.completeExam(result, sessionId)
                navigated = true
                _navigateToResult.value = result.id
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                saveFailed.value = true
            } finally {
                saving = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    packId: String,
    limit: Int,
    onExit: () -> Unit,
    onFinished: (String) -> Unit
) {
    val vm: ExamViewModel = viewModel()
    LaunchedEffect(packId, limit) { vm.load(packId, limit) }

    val questions = vm.questions
    val index by vm.index.collectAsState()
    val answers by vm.answers.collectAsState()
    val marked by vm.markedQuestionIds.collectAsState()
    val remaining by vm.remainingSeconds.collectAsState()
    val saveFailed by vm.saveFailed.collectAsState()
    val finishedResult by vm.finishedResult.collectAsState()
    val loading by vm.loading.collectAsState()
    val busy by vm.busy.collectAsState()
    val pendingExam by vm.pendingExam.collectAsState()
    val loadError by vm.loadError.collectAsState()
    val draftSaveFailed by vm.draftSaveFailed.collectAsState()
    val bookmarks by vm.bookmarks.collectAsState(initial = emptyList())
    val settings by vm.settings.collectAsState(initial = AppSettings())
    var showQuitDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = questions.isNotEmpty()) { if (finishedResult == null && !busy) showQuitDialog = true }

    LaunchedEffect(Unit) {
        vm.navigateToResult.collect { resultId -> if (resultId != null) onFinished(resultId) }
    }

    if (questions.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (loading) {
                CircularProgressIndicator()
                Text("در حال آماده‌سازی آزمون…", modifier = Modifier.padding(12.dp))
            } else {
                Text(loadError ?: "برای شروع، وضعیت آزمون قبلی را مشخص کنید.")
                if (loadError != null && pendingExam == null) {
                    TextButton(onClick = { vm.load(packId, limit) }) { Text("تلاش دوباره") }
                }
                TextButton(onClick = onExit, enabled = !busy) { Text("بازگشت") }
            }
        }
        pendingExam?.let { previous ->
            AlertDialog(
                onDismissRequest = { if (!busy) onExit() },
                title = { Text("آزمون ناتمام دارید") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${previous.pack.title} • ${previous.pack.questions.size.toPersian()} سؤال")
                        Text("زمان آزمون حتی پس از خروج ادامه دارد. با شروع آزمون جدید، آزمون ناتمام قبلی حذف می‌شود.")
                        if (loadError != null) Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    TextButton(onClick = vm::continuePending, enabled = !busy) { Text("ادامه آزمون قبلی") }
                },
                dismissButton = {
                    TextButton(onClick = vm::replacePending, enabled = !busy) { Text("حذف قبلی و شروع جدید") }
                }
            )
        }
        return
    }

    val question = questions[index.coerceIn(0, questions.lastIndex)]
    val answeredCount = answers.size
    val isLast = index == questions.lastIndex
    val questionListState = rememberLazyListState()
    val questionScrollState = rememberScrollState()
    LaunchedEffect(index) {
        questionListState.animateScrollToItem(index)
        questionScrollState.scrollTo(0)
    }
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
                    IconButton(onClick = { showQuitDialog = true }, enabled = finishedResult == null && !busy) {
                        Icon(Icons.Filled.Close, contentDescription = "خروج")
                    }
                },
                actions = {
                    Text(
                        text = if (settings.hideTimer) (if (timeCritical) "زمان کم" else "زمان پنهان") else formatClock(remaining),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (timeCritical) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(end = 16.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (timeCritical) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.onPrimary.copy(alpha = .14f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
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
            if (draftSaveFailed) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("ذخیره پیشرفت انجام نشد.", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::retryProgress, enabled = !busy) { Text("تلاش دوباره") }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("پیشرفت آزمون", style = MaterialTheme.typography.labelLarge)
                Text("${answeredCount.toPersian()} از ${questions.size.toPersian()} پاسخ داده شده",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { answeredCount.toFloat() / questions.size },
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text("انتخاب سریع سوال", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                state = questionListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(questions, key = { _, item -> item.id }) { position, item ->
                    val isCurrent = position == index
                    val isAnswered = item.id in answers
                    val background = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isAnswered -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val foreground = when {
                        isCurrent -> MaterialTheme.colorScheme.onPrimary
                        isAnswered -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(background)
                            .semantics { contentDescription = "سوال ${position + 1}، " +
                                (if (isAnswered) "پاسخ داده شده" else "بی‌پاسخ") +
                                (if (item.id in marked) "، نشان‌شده برای مرور" else "") }
                            .clickable { vm.goTo(position) }, contentAlignment = Alignment.Center
                    ) {
                        Text((position + 1).toPersian() + if (item.id in marked) " •" else "", color = foreground,
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { vm.toggleMark(question.id) }, enabled = finishedResult == null && !busy) {
                    Text(if (question.id in marked) "برداشتن نشان مرور" else "نشان برای مرور")
                }
                TextButton(onClick = vm::goToNextUnanswered,
                    enabled = finishedResult == null && !busy && questions.any { it.id != question.id && it.id !in answers }) {
                    Text("بی‌پاسخ بعدی")
                }
                TextButton(onClick = vm::goToNextMarked,
                    enabled = finishedResult == null && !busy && questions.any { it.id != question.id && it.id in marked }) {
                    Text("نشان بعدی (${marked.size.toPersian()})")
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(questionScrollState)
            ) {
                QuestionCard(
                    question = question,
                    selected = answers[question.id],
                    bookmarked = bookmarkKey(vm.activePackId, question.id) in bookmarks,
                    onBookmark = { vm.toggleBookmark(question.id) },
                    onSelect = { option -> vm.select(question.id, option, settings.autoAdvance) },
                    onClear = { vm.clearAnswer(question.id) }
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
                text = "${answeredCount.toPersian()} سوال پاسخ داده شد",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
            )
        }
    }

    if (finishedResult != null && !saveFailed) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("در حال ذخیره کارنامه") },
            text = { Text("لطفاً چند لحظه صبر کنید.") },
            confirmButton = {}
        )
    }
    if (saveFailed) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("ذخیره کارنامه انجام نشد") },
            text = { Text("فضای ذخیره‌سازی را بررسی کنید و دوباره تلاش کنید. با خروج، کارنامه ذخیره نمی‌شود؛ نسخه ناتمام ممکن است برای بازیابی باقی بماند.") },
            confirmButton = { TextButton(onClick = vm::retrySaveResult) { Text("تلاش دوباره") } },
            dismissButton = { TextButton(onClick = onExit) { Text("خروج بدون ذخیره") } }
        )
    }
    if (showQuitDialog && finishedResult == null) {
        AlertDialog(
            onDismissRequest = { if (!busy) showQuitDialog = false },
            title = { Text("خروج از آزمون") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("می‌توانید پاسخ‌ها را نگه دارید و بعداً ادامه دهید. زمان آزمون متوقف نمی‌شود.")
                    if (draftSaveFailed) Text("ذخیره انجام نشد؛ فضای دستگاه را بررسی و دوباره تلاش کنید.",
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.exitKeepingDraft(onExit) }, enabled = !busy) { Text("ذخیره و خروج") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.discardAndExit(onExit) }, enabled = !busy) { Text("حذف آزمون") }
                    TextButton(onClick = { showQuitDialog = false }, enabled = !busy) { Text("ادامه") }
                }
            }
        )
    }
    if (showFinishDialog && finishedResult == null) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("اتمام آزمون") },
            text = {
                val blank = questions.count { it.id !in answers }
                Text("${blank.toPersian()} سؤال بی‌پاسخ و ${marked.size.toPersian()} سؤال نشان‌شده برای مرور دارید. آزمون را پایان می‌دهید؟")
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
    onSelect: (Int) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (question.subject.isNotBlank()) {
                Text(
                    text = if (question.difficulty.isNotBlank()) {
                        "${question.subject} • ${question.difficulty}"
                    } else {
                        question.subject
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp).clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
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
                        contentDescription = if (bookmarked) "حذف از علاقه‌مندی‌ها" else "ذخیره در علاقه‌مندی‌ها",
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
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f))
                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f), RoundedCornerShape(14.dp))
                        .testTag("answer-$i")
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(i) })
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
            if (selected != null) {
                TextButton(onClick = onClear, modifier = Modifier.align(Alignment.End)) {
                    Text("پاک کردن پاسخ")
                }
            }
        }
    }
}
