package com.parsaapps.nichmarket.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parsaapps.nichmarket.Graph
import com.parsaapps.nichmarket.data.model.AppSettings
import com.parsaapps.nichmarket.data.model.AppTheme
import com.parsaapps.nichmarket.util.toPersian
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect

class SettingsViewModel : ViewModel() {
    val settings = Graph.userStore.settings
    val errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private fun save(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() } catch (e: CancellationException) { throw e } catch (e: Exception) {
                errors.emit("ذخیره تنظیمات انجام نشد؛ دوباره تلاش کنید.")
            }
        }
    }
    fun theme(value: AppTheme) = save { Graph.userStore.setTheme(value) }
    fun largeText(value: Boolean) = save { Graph.userStore.setLargeText(value) }
    fun autoAdvance(value: Boolean) = save { Graph.userStore.setAutoAdvance(value) }
    fun hideTimer(value: Boolean) = save { Graph.userStore.setHideTimer(value) }
    fun dailyGoal(value: Int) = save { Graph.userStore.setDailyGoal(value) }
    fun reset() = save { Graph.userStore.resetSettings() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val settings by vm.settings.collectAsState(initial = AppSettings())
    val snackbar = remember { SnackbarHostState() }
    var confirmReset by remember { mutableStateOf(false) }
    LaunchedEffect(vm) { vm.errors.collect { snackbar.showSnackbar(it) } }
    Scaffold(topBar = { TopAppBar(title = { Text("تنظیمات") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
    }) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ظاهر برنامه", style = MaterialTheme.typography.titleLarge)
            Text("تم به‌صورت محلی ذخیره می‌شود.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(AppTheme.SYSTEM to "مطابق سیستم", AppTheme.LIGHT to "روشن", AppTheme.DARK to "تیره").forEach { (mode, title) ->
                Row(Modifier.fillMaxWidth().clickable { vm.theme(mode) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = settings.theme == mode, onClick = null)
                    Text(title, Modifier.padding(horizontal = 8.dp))
                }
            }
            SettingSwitch("متن بزرگ‌تر", "اندازه متن برنامه را علاوه بر تنظیم دستگاه افزایش می‌دهد.",
                settings.largeText, vm::largeText)
            Text("رفتار آزمون", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
            SettingSwitch("رفتن خودکار به سؤال بعد", "پس از انتخاب گزینه؛ در سؤال آخر خودکار پایان نمی‌یابد.",
                settings.autoAdvance, vm::autoAdvance)
            SettingSwitch("پنهان کردن ساعت", "زمان همچنان می‌گذرد و در پایان آزمون به‌طور خودکار تمام می‌شود.",
                settings.hideTimer, vm::hideTimer)
            Text("هدف روزانه", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
            Text("تعداد پاسخ‌های ثبت‌شده در کارنامه‌های امروز؛ کاملاً محلی و بدون اعلان.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 20, 40).forEach { goal ->
                    FilterChip(selected = settings.dailyGoal == goal, onClick = { vm.dailyGoal(goal) },
                        label = { Text("${goal.toPersian()} سؤال") })
                }
            }
            TextButton(onClick = { confirmReset = true }) { Text("بازگرداندن تنظیمات پیش‌فرض") }
            Text("این کار پاسخ‌ها، خریدها و کارنامه‌ها را پاک نمی‌کند.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("بازگرداندن تنظیمات") },
        text = { Text("تنها تنظیمات ظاهر و آزمون به حالت اولیه بازمی‌گردند.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; vm.reset() }) { Text("بازگرداندن") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("انصراف") } })
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
