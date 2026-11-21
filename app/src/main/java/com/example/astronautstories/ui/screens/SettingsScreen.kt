package com.example.astronautstories.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.astronautstories.assets_loader.NativeBitmapRegistry
import com.example.astronautstories.data.local.AppSettings
import com.example.astronautstories.domain.model.GraphicsQuality
import com.example.astronautstories.localization.AppLanguage
import com.example.astronautstories.localization.GameStrings
import com.example.astronautstories.ui.components.UniformAssetBadge
import com.example.astronautstories.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    strings: GameStrings,
    bitmapRegistry: NativeBitmapRegistry,
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var nameDraft by remember(settings.playerDisplayName) { mutableStateOf(settings.playerDisplayName) }
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            mainViewModel.setRemindersEnabled(true)
        } else {
            mainViewModel.setRemindersEnabled(false)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (settings.language == AppLanguage.ENGLISH) "Notification permission denied"
                    else "مجوز ارسال نوتیفیکیشن داده نشد"
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backBtn)
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Column: Commander Callsign + Language & Theme
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = cardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.95f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UniformAssetBadge(
                            bitmapRegistry = bitmapRegistry,
                            assetPath = "images/astronaut.png",
                            sizeDp = 54.dp,
                            cornerRadius = 27.dp,
                            innerPadding = 4.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = nameDraft,
                                onValueChange = { nameDraft = it.take(40) },
                                singleLine = true,
                                label = { Text(if (settings.language == AppLanguage.ENGLISH) "Commander Name" else "نام فرمانده") },
                                modifier = Modifier.fillMaxWidth().testTag("settings_name_input")
                            )
                            Button(
                                onClick = {
                                    mainViewModel.setDisplayName(nameDraft)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (settings.language == AppLanguage.ENGLISH) "Saved" else "ذخیره شد"
                                        )
                                    }
                                },
                                modifier = Modifier.height(40.dp).testTag("settings_save_name_button")
                            ) {
                                Text(if (settings.language == AppLanguage.ENGLISH) "Save Callsign" else "ذخیره نام")
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = cardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.05f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                SettingsOptionPill(
                                    label = lang.label,
                                    selected = settings.language == lang,
                                    tag = "settings_lang_${lang.code}",
                                    modifier = Modifier.weight(1f),
                                    onClick = { mainViewModel.setLanguage(lang) }
                                )
                            }
                        }
                        SettingSwitchRow(
                            label = strings.darkThemeLabel,
                            checked = settings.isDarkTheme,
                            tag = "settings_dark_theme_switch",
                            onCheckedChange = { mainViewModel.setDarkTheme(it) }
                        )
                    }
                }
            }

            // Right Column: Audio & Graphics + Controls & Accessibility
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = cardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.05f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${strings.musicVolumeLabel}: ${(settings.musicVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp, lineHeight = 14.sp),
                                modifier = Modifier.weight(0.46f),
                                maxLines = 2
                            )
                            Slider(
                                value = settings.musicVolume,
                                onValueChange = { mainViewModel.setMusicVolume(it) },
                                modifier = Modifier.weight(0.54f).height(28.dp).testTag("settings_music_slider")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${strings.sfxVolumeLabel}: ${(settings.sfxVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp, lineHeight = 14.sp),
                                modifier = Modifier.weight(0.46f),
                                maxLines = 2
                            )
                            Slider(
                                value = settings.sfxVolume,
                                onValueChange = { mainViewModel.setSfxVolume(it) },
                                modifier = Modifier.weight(0.54f).height(28.dp).testTag("settings_sfx_slider")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                strings.graphicsQualityLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                                fontWeight = FontWeight.Bold
                            )
                            GraphicsQuality.entries.forEach { q ->
                                SettingsOptionPill(
                                    label = q.name,
                                    selected = settings.graphicsQuality == q,
                                    tag = "settings_gfx_${q.name}",
                                    modifier = Modifier.weight(1f),
                                    onClick = { mainViewModel.setGraphicsQuality(q) }
                                )
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = cardBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SettingSwitchRow(strings.hapticsLabel, settings.hapticsEnabled, "settings_haptics_switch") {
                            mainViewModel.setHapticsEnabled(it)
                        }
                        SettingSwitchRow(strings.leftHandedLabel, settings.leftHandedControls, "settings_lefthand_switch") {
                            mainViewModel.setLeftHandedControls(it)
                        }
                        SettingSwitchRow(strings.accessibilityLabel, settings.accessibilityMode, "settings_a11y_switch") {
                            mainViewModel.setAccessibilityMode(it)
                        }
                        SettingSwitchRow(strings.reduceMotionLabel, settings.reduceMotion, "settings_motion_switch") {
                            mainViewModel.setReduceMotion(it)
                        }
                        SettingSwitchRow(strings.reminderNotifLabel, settings.remindersEnabled, "settings_reminder_switch") { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    mainViewModel.setRemindersEnabled(true)
                                } else {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                mainViewModel.setRemindersEnabled(enabled)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsOptionPill(
    label: String,
    selected: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .height(36.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp, lineHeight = 14.sp),
            maxLines = 2,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
