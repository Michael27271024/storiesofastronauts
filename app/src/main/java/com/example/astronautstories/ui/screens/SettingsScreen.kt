package com.example.astronautstories.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.astronautstories.assets_loader.NativeBitmapRegistry
import com.example.astronautstories.data.local.AppSettings
import com.example.astronautstories.domain.model.GraphicsQuality
import com.example.astronautstories.localization.AppLanguage
import com.example.astronautstories.localization.GameStrings
import com.example.astronautstories.ui.components.CopyrightNoticeCard
import com.example.astronautstories.ui.components.CreditsLinksCard
import com.example.astronautstories.ui.components.UniformAssetBadge
import com.example.astronautstories.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private data class SettingsSectionTab(
    val index: Int,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

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
    var selectedSection by remember { mutableIntStateOf(0) }
    var narrowSubPage by remember(selectedSection) { mutableIntStateOf(0) }
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
                    when (settings.language) {
                        AppLanguage.ENGLISH -> "Notification permission denied"
                        AppLanguage.ARABIC -> "تم رفض إذن الإشعارات"
                        AppLanguage.PERSIAN -> "مجوز ارسال نوتیفیکیشن داده نشد"
                    }
                )
            }
        }
    }

    val sectionTabs = remember(settings.language) {
        listOf(
            SettingsSectionTab(
                index = 0,
                label = when (settings.language) {
                    AppLanguage.ENGLISH -> "Profile & Lang"
                    AppLanguage.ARABIC -> "الملف واللغة"
                    AppLanguage.PERSIAN -> "پروفایل و زبان"
                },
                icon = Icons.Default.Person,
                tag = "settings_tab_general"
            ),
            SettingsSectionTab(
                index = 1,
                label = when (settings.language) {
                    AppLanguage.ENGLISH -> "Audio & GFX"
                    AppLanguage.ARABIC -> "الصوت والجرافيك"
                    AppLanguage.PERSIAN -> "صدا و گرافیک"
                },
                icon = Icons.Default.GraphicEq,
                tag = "settings_tab_audio"
            ),
            SettingsSectionTab(
                index = 2,
                label = when (settings.language) {
                    AppLanguage.ENGLISH -> "Controls"
                    AppLanguage.ARABIC -> "التحكم"
                    AppLanguage.PERSIAN -> "کنترل و دسترسی"
                },
                icon = Icons.Default.Tune,
                tag = "settings_tab_controls"
            ),
            SettingsSectionTab(
                index = 3,
                label = when (settings.language) {
                    AppLanguage.ENGLISH -> "Credits"
                    AppLanguage.ARABIC -> "المطورون"
                    AppLanguage.PERSIAN -> "سازندگان"
                },
                icon = Icons.Default.Copyright,
                tag = "settings_tab_credits"
            )
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.settingsTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backBtn)
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val isTwoColumn = maxWidth >= 540.dp
            val isCompactHeight = maxHeight < 280.dp
            val showIconsInTabs = maxWidth >= 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1080.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button-Driven Top Category Bar (Zero Scroll)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sectionTabs.forEach { tab ->
                        val selected = selectedSection == tab.index
                        Surface(
                            onClick = { selectedSection = tab.index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag(tab.tag)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showIconsInTabs) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Active Section Content (100% Zero-Scroll, Button-Navigated Cards)
                val profileCard: @Composable (Modifier) -> Unit = { cardMod ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = cardBorder,
                        shape = RoundedCornerShape(16.dp),
                        modifier = cardMod
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (isCompactHeight) 10.dp else 14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                UniformAssetBadge(
                                    bitmapRegistry = bitmapRegistry,
                                    assetPath = "images/astronaut.png",
                                    contentDescription = settings.playerDisplayName,
                                    sizeDp = if (isCompactHeight) 40.dp else 48.dp,
                                    cornerRadius = 24.dp,
                                    innerPadding = 4.dp
                                )
                                OutlinedTextField(
                                    value = nameDraft,
                                    onValueChange = { nameDraft = it.take(40) },
                                    singleLine = true,
                                    label = {
                                        Text(
                                            text = when (settings.language) {
                                                AppLanguage.ENGLISH -> "Commander Name"
                                                AppLanguage.ARABIC -> "اسم القائد"
                                                AppLanguage.PERSIAN -> "نام فرمانده"
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_name_input")
                                )
                            }

                            Button(
                                onClick = {
                                    mainViewModel.setDisplayName(nameDraft)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            when (settings.language) {
                                                AppLanguage.ENGLISH -> "Saved"
                                                AppLanguage.ARABIC -> "تم الحفظ"
                                                AppLanguage.PERSIAN -> "ذخیره شد"
                                            }
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_save_name_button")
                            ) {
                                Text(
                                    text = when (settings.language) {
                                        AppLanguage.ENGLISH -> "Save Callsign"
                                        AppLanguage.ARABIC -> "حفظ الاسم"
                                        AppLanguage.PERSIAN -> "ذخیره نام"
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                val languageAndThemeCard: @Composable (Modifier) -> Unit = { cardMod ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = cardBorder,
                        shape = RoundedCornerShape(16.dp),
                        modifier = cardMod
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (isCompactHeight) 10.dp else 14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = strings.languageLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                            SettingSwitchRow(
                                label = strings.darkThemeLabel,
                                checked = settings.isDarkTheme,
                                tag = "settings_dark_theme_switch",
                                onCheckedChange = { mainViewModel.setDarkTheme(it) }
                            )
                        }
                    }
                }

                val audioCard: @Composable (Modifier) -> Unit = { cardMod ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = cardBorder,
                        shape = RoundedCornerShape(16.dp),
                        modifier = cardMod
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (isCompactHeight) 10.dp else 14.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SettingsSliderItem(
                                label = strings.musicVolumeLabel,
                                value = settings.musicVolume,
                                tag = "settings_music_slider",
                                onValueChange = { mainViewModel.setMusicVolume(it) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            SettingsSliderItem(
                                label = strings.sfxVolumeLabel,
                                value = settings.sfxVolume,
                                tag = "settings_sfx_slider",
                                onValueChange = { mainViewModel.setSfxVolume(it) }
                            )
                        }
                    }
                }

                val graphicsCard: @Composable (Modifier) -> Unit = { cardMod ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = cardBorder,
                        shape = RoundedCornerShape(16.dp),
                        modifier = cardMod
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (isCompactHeight) 10.dp else 14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.graphicsQualityLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val gfxHint = when (settings.graphicsQuality) {
                                    GraphicsQuality.LOW -> when (settings.language) {
                                        AppLanguage.ENGLISH -> "Battery-saving mode with lighter particle effects."
                                        AppLanguage.ARABIC -> "وضع توفير البطارية مع مؤثرات أخف."
                                        AppLanguage.PERSIAN -> "حالت بهینه مصرف باتری با افکت‌های سبک‌تر."
                                    }
                                    GraphicsQuality.MEDIUM -> when (settings.language) {
                                        AppLanguage.ENGLISH -> "Balanced 60 FPS visuals for all planets and 3D bosses."
                                        AppLanguage.ARABIC -> "رسوم متوازنة ٦٠ إطاراً للكواكب والزعماء ثلاثيي الأبعاد."
                                        AppLanguage.PERSIAN -> "تعادل ۶۰ فریم برای تمام سیاره‌ها و باس‌های سه‌بعدی."
                                    }
                                    GraphicsQuality.HIGH -> when (settings.language) {
                                        AppLanguage.ENGLISH -> "Maximum shaders, lighting beams, and 3D boss details."
                                        AppLanguage.ARABIC -> "أعلى جودة للإضاءة والمؤثرات وتفاصيل الزعماء."
                                        AppLanguage.PERSIAN -> "حداکثر جزئیات نورپردازی، ذرات و جلوه‌های باس‌های سه‌بعدی."
                                    }
                                }
                                Text(
                                    text = gfxHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                val controlsLeftCard: @Composable (Modifier) -> Unit = { cardMod ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = cardBorder,
                        shape = RoundedCornerShape(16.dp),
                        modifier = cardMod
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SettingSwitchRow(
                                label = strings.hapticsLabel,
                                checked = settings.hapticsEnabled,
                                tag = "settings_haptics_switch",
                                onCheckedChange = { mainViewModel.setHapticsEnabled(it) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            SettingSwitchRow(
                                label = strings.leftHandedLabel,
                                checked = settings.leftHandedControls,
                                tag = "settings_lefthand_switch",
                                onCheckedChange = { mainViewModel.setLeftHandedControls(it) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            SettingSwitchRow(
                                label = strings.accessibilityLabel,
                                checked = settings.accessibilityMode,
                                tag = "settings_a11y_switch",
                                onCheckedChange = { mainViewModel.setAccessibilityMode(it) }
                            )
                        }
                    }
                }

                val controlsRightCard: @Composable (Modifier) -> Unit = { cardMod ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = cardBorder,
                        shape = RoundedCornerShape(16.dp),
                        modifier = cardMod
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SettingSwitchRow(
                                label = strings.reduceMotionLabel,
                                checked = settings.reduceMotion,
                                tag = "settings_motion_switch",
                                onCheckedChange = { mainViewModel.setReduceMotion(it) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            SettingSwitchRow(
                                label = strings.reminderNotifLabel,
                                checked = settings.remindersEnabled,
                                tag = "settings_reminder_switch",
                                onCheckedChange = { enabled ->
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
                            )
                        }
                    }
                }

                val creditsLeftCard: @Composable (Modifier) -> Unit = { cardMod ->
                    CreditsLinksCard(
                        strings = strings,
                        compact = isCompactHeight,
                        modifier = cardMod
                    )
                }

                val creditsRightCard: @Composable (Modifier) -> Unit = { cardMod ->
                    CopyrightNoticeCard(
                        strings = strings,
                        compact = isCompactHeight,
                        modifier = cardMod
                    )
                }

                val pair: Pair<@Composable (Modifier) -> Unit, @Composable (Modifier) -> Unit> = when (selectedSection) {
                    0 -> profileCard to languageAndThemeCard
                    1 -> audioCard to graphicsCard
                    2 -> controlsLeftCard to controlsRightCard
                    else -> creditsLeftCard to creditsRightCard
                }

                if (isTwoColumn) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        pair.first(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        pair.second(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (narrowSubPage == 0) {
                            pair.first(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        } else {
                            pair.second(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { narrowSubPage = 0 },
                                enabled = narrowSubPage != 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_subpage_prev")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(strings.backBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(
                                text = "${narrowSubPage + 1} / 2",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            FilledTonalButton(
                                onClick = { narrowSubPage = 1 },
                                enabled = narrowSubPage != 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_subpage_next")
                            ) {
                                Text(strings.nextBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSliderItem(
    label: String,
    value: Float,
    tag: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
        )
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
        shape = RoundedCornerShape(10.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            }
        ),
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
