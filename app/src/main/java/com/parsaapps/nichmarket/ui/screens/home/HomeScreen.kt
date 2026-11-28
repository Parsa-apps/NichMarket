package com.parsaapps.nichmarket.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.R
import com.parsaapps.nichmarket.data.model.Category
import com.parsaapps.nichmarket.data.model.AppSettings
import com.parsaapps.nichmarket.data.analysis.recentResults
import com.parsaapps.nichmarket.data.analysis.answeredQuestions
import com.parsaapps.nichmarket.data.analysis.subjectPerformance
import com.parsaapps.nichmarket.data.analysis.matchesPersianSearch
import com.parsaapps.nichmarket.ui.components.categoryIcon
import com.parsaapps.nichmarket.util.formatShamsi
import com.parsaapps.nichmarket.util.toPersian
import com.parsaapps.nichmarket.util.toPersianDigits
import kotlinx.coroutines.delay

class HomeViewModel : ViewModel() {
    val categories = Graph.repository.categories
    val purchasedSkus = Graph.userStore.purchasedSkus
    val results = Graph.userStore.results
    val bookmarks = Graph.userStore.bookmarks
    val activeExam = Graph.userStore.activeExam
    val settings = Graph.userStore.settings
}

@Composable
fun HomeScreen(
    onCategoryClick: (String) -> Unit,
    onOpenResult: (String) -> Unit,
    onResults: () -> Unit,
    onBookmarks: () -> Unit,
    onResumeExam: (String) -> Unit,
    onReview: () -> Unit,
    onSettings: () -> Unit
) {
    val vm: HomeViewModel = viewModel()
    val purchased by vm.purchasedSkus.collectAsState(initial = emptySet())
    val results by vm.results.collectAsState(initial = emptyList())
    val bookmarks by vm.bookmarks.collectAsState(initial = emptyList())
    val activeExam by vm.activeExam.collectAsState(initial = null)
    val settings by vm.settings.collectAsState(initial = AppSettings())
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); now = System.currentTimeMillis() } }
    val answeredToday = remember(results, now) { answeredQuestions(recentResults(results, 1, now)) }
    val available = vm.categories.sumOf { category ->
        category.packages.count { it.isFree || it.sku in purchased }
    }
    var categoryQuery by rememberSaveable { mutableStateOf("") }
    val visibleCategories = remember(vm.categories, categoryQuery) {
        vm.categories.filter { category ->
            matchesPersianSearch(categoryQuery, category.title, category.description,
                category.packages.joinToString(" ") { it.title })
        }
    }
    val latest = results.firstOrNull()
    val weakest = remember(latest) { latest?.questionSnapshot?.let { subjectPerformance(it, latest.answers).firstOrNull { stat -> stat.correct < stat.total } } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("home-list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { BrandBar(onSettings) }
        item { StudyHero() }
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f))) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    StudyMetric(available.toPersian(), "بسته فعال", Modifier.weight(1f))
                    StudyMetric(results.size.toPersian(), "آزمون", Modifier.weight(1f))
                    StudyMetric(bookmarks.size.toPersian(), "نشان‌شده", Modifier.weight(1f))
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("هدف امروز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onSettings) { Text("تغییر هدف") }
                    }
                    Text("${answeredToday.toString().toPersianDigits()} از ${settings.dailyGoal.toPersian()} پاسخ",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(progress = { (answeredToday.toFloat() / settings.dailyGoal).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth())
                    Text(if (answeredToday >= settings.dailyGoal) "به هدف امروز رسیدی!" else "پاسخ‌های کارنامه‌های امروز روی این دستگاه شمرده می‌شوند.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("میز مطالعه", "همه‌چیز برای یک تمرین هدفمند")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("کارنامه‌ها", "نتایج و تحلیل", Icons.Rounded.Insights, Modifier.weight(1f), onResults)
                    QuickAction("علاقه‌مندی‌ها", "سؤال‌های ذخیره‌شده", Icons.Rounded.FavoriteBorder, Modifier.weight(1f), onBookmarks)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction("مرور هوشمند", "رفع اشکال با فاصله‌گذاری", Icons.Rounded.Refresh, Modifier.weight(1f), onReview)
                    Spacer(Modifier.weight(1f))
                }
                activeExam?.let { active ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onResumeExam(active.pack.id) },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("آزمون ناتمام", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text(active.pack.title, style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("${active.answers.size.toPersian()} پاسخ ثبت‌شده • زمان متوقف نمی‌شود",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "ادامه آزمون",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                if (latest != null) {
                    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenResult(latest.id) },
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("آخرین کارنامه", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(latest.packTitle, style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 2)
                                Text(formatShamsi(latest.date) + "  •  مشاهده پاسخ‌ها",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Text("${latest.percent.toPersian()}٪", style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                    weakest?.let { subject ->
                        Text("پیشنهاد مرور: ${subject.subject} • ${subject.correct.toPersian()} پاسخ درست از ${subject.total.toPersian()}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onOpenResult(latest.id) }.padding(horizontal = 4.dp, vertical = 6.dp))
                    }
                } else {
                    Text("هنوز آزمونی نداده‌ای؛ از یکی از بسته‌های رایگان شروع کن.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
        item { SectionHeading("مسیرهای یادگیری", "حوزه موردنظرت را برای شروع انتخاب کن") }
        item {
            OutlinedTextField(value = categoryQuery, onValueChange = { categoryQuery = it },
                modifier = Modifier.fillMaxWidth(), label = { Text("جست‌وجوی مسیر یا بسته") }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = { if (categoryQuery.isNotEmpty()) IconButton(onClick = { categoryQuery = "" }) {
                    Icon(Icons.Rounded.Close, contentDescription = "پاک کردن جست‌وجو")
                } }, shape = RoundedCornerShape(18.dp))
        }
        if (visibleCategories.isEmpty()) item {
            Text("مسیری پیدا نشد؛ عبارت دیگری را امتحان کنید.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
        itemsIndexed(visibleCategories, key = { _, category -> category.id }) { index, category ->
            CategoryCard(category, index) { onCategoryClick(category.id) }
        }
        item {
            Text("سؤال‌ها تمرینی و تألیفی‌اند و دفترچه رسمی آزمون‌های گذشته نیستند.",
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BrandBar(onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Column(Modifier.weight(1f)) {
            Text("نیچ مارکت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("فضای تمرین و یادگیری", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "تنظیمات", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
            Text("پارسا اپس", Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun StudyHero() {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xFF0A2527))) {
        Image(painterResource(R.drawable.study_hero), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize().graphicsLayer { scaleX = -1f })
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            listOf(Color(0x330A2527), Color(0xED0A2527)))))
        Column(Modifier.fillMaxWidth(.78f).heightIn(min = 220.dp).padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center) {
            Text("یادگیری، با هدف", color = Color(0xFFF3D59B),
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("هر روز، یک قدم جلوتر", color = Color.White,
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("با تمرین هدفمند و مرور پاسخ‌ها، برای آزمون بعدی آماده‌تر شو.",
                color = Color(0xFFE4EEE9), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StudyMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickAction(title: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryCard(category: Category, index: Int, onClick: () -> Unit) {
    val icon = categoryIcon(category.id)
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp))
                .background(if (index % 2 == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp),
                    tint = if (index % 2 == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${category.totalQuestions.toPersian()} سؤال • ${category.packages.size.toPersian()} بسته",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
