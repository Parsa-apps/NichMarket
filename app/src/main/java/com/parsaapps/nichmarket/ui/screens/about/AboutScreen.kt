package com.parsaapps.nichmarket.ui.screens.about

import android.animation.ValueAnimator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.database.ContentObserver
import android.content.Context
import android.provider.Settings
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parsaapps.nichmarket.BuildConfig
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.data.model.AppSettings
import com.parsaapps.nichmarket.R
import com.parsaapps.nichmarket.ui.components.GradientHeader
import com.parsaapps.nichmarket.util.toPersianDigits
import kotlinx.coroutines.launch

private const val WEBSITE = "https://parsa-apps.github.io/"
private const val EMAIL = "farshadparsa2019@gmail.com"
private const val TELEGRAM = "https://t.me/Parsaappsadmin"
private const val INSTAGRAM = "https://instagram.com/parsa_apps"
private const val GITHUB = "https://github.com/Parsa-apps"
private const val JAZIRE_MYKET = "https://myket.ir/app/com.parsaapps.amoozesh_fandoghi"
private const val JAZIRE_BAZAAR = "https://github.com/Parsa-apps/Jazire-Fandoqi/releases/tag/v7.5.6-bazaar"
private const val KARTONIYA = "https://github.com/Parsa-apps/Kartoniya"
private const val NAQASHI = "https://github.com/Parsa-apps/Naqashi-Fandoqi"

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val clipboard: ClipboardManager = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showFontLicense by remember { mutableStateOf(false) }
    val settings by Graph.userStore.settings.collectAsState(initial = AppSettings())

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar("برنامه‌ای برای باز کردن این پیوند پیدا نشد.") }
        } catch (e: SecurityException) {
            scope.launch { snackbar.showSnackbar("امکان باز کردن این پیوند وجود ندارد.") }
        }
    }
    fun openWeb(url: String) = open(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    fun openEmail() = open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$EMAIL")).apply {
        putExtra(Intent.EXTRA_SUBJECT, "پشتیبانی نیچ مارکت")
    })

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GradientHeader("درباره ما", "نیچ مارکت، محصولی از پارسا اپس")
            }
            item {
                AboutCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PublisherIdentity(reduceMotion = settings.reduceMotion)
                        Text("ایده‌های بزرگ برای دنیای کوچک", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                        Text(
                            "پارسا اپس یک استودیوی مستقل ایرانی در زمینه طراحی و توسعه نرم‌افزار است. " +
                                "ما با عشق به فناوری، یادگیری و خلاقیت، تجربه‌های دیجیتال فارسی می‌سازیم؛ " +
                                "از اپلیکیشن‌های آموزشی و سرگرمی تا بازی‌ها و محصولات خلاقانه.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                        )
                        Button(onClick = { openWeb(WEBSITE) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Language, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("وب‌سایت رسمی پارسا اپس")
                        }
                        Text("parsa-apps.github.io", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                AboutCard {
                    SectionTitle("نیچ مارکت؛ همراه مسیر یادگیری")
                    BodyText("نیچ مارکت مجموعه‌ای از سؤال‌های تمرینی و تألیفی برای آمادگی آزمون‌های استخدامی است؛ " +
                        "با آزمون زمان‌دار، مرور تشریحی، علاقه‌مندی‌ها و کارنامه. سؤال‌ها دفترچه رسمی آزمون‌های گذشته نیستند.")
                    BodyText("آزمون‌ها بدون اینترنت اجرا می‌شوند؛ خرید و بازیابی بسته‌های پولی از طریق مایکت انجام می‌شود.")
                    Text("نسخه ${BuildConfig.VERSION_NAME.toPersianDigits()}",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    BodyText("فونت فارسی برنامه: وزیرمتن، تحت مجوز SIL Open Font License 1.1.")
                    TextButton(onClick = { showFontLicense = true }) { Text("مشاهده مجوز فونت") }
                }
            }
            item {
                AboutCard {
                    SectionTitle("محصولات و پروژه‌های پارسا اپس")
                    BodyText("از یادگیری و بازی کودکان تا ابزارهای خلاق؛ هر محصول برای یک تجربه کاربردی فارسی ساخته می‌شود.")
                    ProductLink("جزیره فندقی", "آموزش و بازی کودک • صفحه مایکت") { openWeb(JAZIRE_MYKET) }
                    ProductLink("جزیره فندقی", "نسخه ویژه کافه‌بازار • انتشار گیت‌هاب") { openWeb(JAZIRE_BAZAAR) }
                    ProductLink("کارتونیا", "پروژه در حال توسعه • گیت‌هاب") { openWeb(KARTONIYA) }
                    ProductLink("استودیو نقاشی فندوقی", "نقاشی و خلاقیت کودک • گیت‌هاب") { openWeb(NAQASHI) }
                }
            }
            item {
                AboutCard {
                    SectionTitle("ارتباط با ما")
                    BodyText("برای گزارش مشکل، پیشنهاد یا پیگیری خرید با پشتیبانی پارسا اپس در ارتباط باشید.")
                    Text(EMAIL, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { openEmail() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp)); Text("ارسال ایمیل")
                        }
                        OutlinedButton(onClick = {
                            clipboard.setText(AnnotatedString(EMAIL))
                            scope.launch { snackbar.showSnackbar("نشانی ایمیل کپی شد.") }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp)); Text("کپی ایمیل")
                        }
                    }
                    ProductLink("تلگرام", "@Parsaappsadmin") { openWeb(TELEGRAM) }
                    ProductLink("اینستاگرام", "@parsa_apps") { openWeb(INSTAGRAM) }
                    ProductLink("گیت‌هاب", "github.com/Parsa-apps") { openWeb(GITHUB) }
                }
            }
            item {
                AboutCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        SectionTitle("داده‌های شما")
                    }
                    BodyText("کارنامه‌ها و علاقه‌مندی‌ها روی همین دستگاه ذخیره می‌شوند. " +
                        "با حذف برنامه این داده‌های محلی پاک می‌شوند؛ برای بازیابی خرید باید با حساب خریدار به مایکت وارد شوید. " +
                        "بازکردن پیوندهای بیرونی و استفاده از مایکت تابع شرایط و سیاست‌های همان سرویس‌هاست.")
                }
            }
            item {
                Text("ساخته‌شده با دقت در پارسا اپس", modifier = Modifier.fillMaxWidth().padding(8.dp),
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showFontLicense) {
        val license = remember {
            runCatching { context.assets.open("licenses/Vazirmatn-OFL.txt").bufferedReader().use { it.readText() } }
                .getOrDefault("متن مجوز در دسترس نیست.")
        }
        AlertDialog(onDismissRequest = { showFontLicense = false },
            title = { Text("مجوز فونت وزیرمتن") },
            text = { Text(license, modifier = Modifier.verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = { showFontLicense = false }) { Text("بستن") } })
    }
}

/** Animation is scoped to the visible identity item and respects reduced-motion preferences. */
@Composable
internal fun PublisherIdentity(reduceMotion: Boolean) {
    val systemAllowsMotion = rememberSystemMotionEnabled()
    val glow: State<Float> = if (!reduceMotion && systemAllowsMotion) {
        val transition = rememberInfiniteTransition(label = "publisherGlow")
        transition.animateFloat(initialValue = 0.20f, targetValue = 0.46f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "publisherGlowAlpha")
    } else rememberUpdatedState(0.26f)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.goldenAura(glow).padding(14.dp), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(R.drawable.parsa_apps_logo), contentDescription = "لوگوی رسمی پارسا اپس",
                modifier = Modifier.size(140.dp))
        }
        Text("پارسا اپس", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Box(Modifier.goldenAura(glow).padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text("فرشاد پارسا • مدیریت و توسعه اصلی", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

private fun systemMotionEnabled(context: Context): Boolean = runCatching {
    if (Build.VERSION.SDK_INT >= 26) ValueAnimator.areAnimatorsEnabled()
    else Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
}.getOrDefault(true)

@Composable
private fun rememberSystemMotionEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember(context) { mutableStateOf(systemMotionEnabled(context)) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { enabled = systemMotionEnabled(context) }
        }
        val registered = runCatching {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer
            )
        }.isSuccess
        onDispose { if (registered) context.contentResolver.unregisterContentObserver(observer) }
    }
    return enabled
}

private fun Modifier.goldenAura(glow: State<Float>): Modifier = drawWithCache {
    val radius = CornerRadius(size.minDimension / 2f)
    val gradient = Brush.radialGradient(listOf(Color(0xFFD8A73E), Color(0x00D8A73E)), radius = size.maxDimension * .75f)
    val stroke = Stroke(width = 1.dp.toPx())
    onDrawBehind {
        drawRoundRect(brush = gradient, alpha = glow.value, cornerRadius = radius)
        drawRoundRect(color = Color(0xFFD2A54F).copy(alpha = glow.value), cornerRadius = radius, style = stroke)
    }
}

@Composable
private fun AboutCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ProductLink(title: String, subtitle: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelMedium)
        }
        Icon(Icons.Filled.OpenInNew, contentDescription = "باز کردن پیوند", modifier = Modifier.size(18.dp))
    }
}
