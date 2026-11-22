package com.example.astronautstories.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astronautstories.assets_loader.NativeBitmapRegistry
import com.example.astronautstories.data.local.PlayerProgressEntity
import com.example.astronautstories.data.local.StageRecordEntity
import com.example.astronautstories.domain.model.GameCatalog
import com.example.astronautstories.localization.AppLanguage
import com.example.astronautstories.localization.GameStrings
import com.example.astronautstories.ui.components.UniformAssetBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolarSystemMapScreen(
    language: AppLanguage,
    strings: GameStrings,
    progress: PlayerProgressEntity,
    stageRecords: Map<Int, StageRecordEntity>,
    bitmapRegistry: NativeBitmapRegistry,
    onSelectStage: (Int) -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit
) {
    val clampedUnlocked = progress.unlockedStage.coerceIn(1, GameCatalog.TOTAL_STAGES)
    val initialChapter = ((clampedUnlocked - 1) / GameCatalog.STAGES_PER_CHAPTER + 1).coerceIn(1, GameCatalog.TOTAL_CHAPTERS)
    var selectedChapter by remember { mutableIntStateOf(initialChapter) }
    var chapterPage by remember { mutableIntStateOf(if (initialChapter > 5) 1 else 0) }
    var showResetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val chapter = GameCatalog.chapters[selectedChapter - 1]
    val chapterStages = remember(selectedChapter) {
        GameCatalog.allStages.filter { it.chapterIndex == selectedChapter }
    }
    val visibleChapters = remember(chapterPage) {
        GameCatalog.chapters.drop(chapterPage * 5).take(5)
    }
    val totalStars = stageRecords.values.sumOf { it.stars }
    val chapterBg = bitmapRegistry.getBitmap(chapter.bgAssetPath, 768)

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(if (language == AppLanguage.ENGLISH) "Reset Campaign?" else "بازنشانی پیشرفت مراحل؟") },
            text = {
                Text(
                    if (language == AppLanguage.ENGLISH)
                        "This will reset unlocked story stages back to Stage 1 while keeping your heroes and weapons."
                    else "با این کار مراحل داستانی به مرحله ۱ بازمی‌گردند (قهرمان‌ها و سلاح‌های خریداری‌شده حفظ می‌شوند)."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetProgress()
                        selectedChapter = 1
                        chapterPage = 0
                    },
                    modifier = Modifier.testTag("confirm_reset_progress_button")
                ) {
                    Text(if (language == AppLanguage.ENGLISH) "Reset" else "تایید بازنشانی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(strings.backBtn)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.solarMapTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("map_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backBtn)
                    }
                },
                actions = {
                    Text(
                        text = "★ $totalStars/${GameCatalog.MAX_STARS}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("map_reset_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Reset Progress")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Fixed Chapter Selector Strip with compact full-label selectable pills (Zero Ellipsis)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        chapterPage = if (chapterPage == 0) 1 else 0
                        selectedChapter = chapterPage * 5 + 1
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .testTag("chapter_page_prev")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Planets")
                }

                visibleChapters.forEach { ch ->
                    val isSelected = ch.chapterIndex == selectedChapter
                    Surface(
                        onClick = { selectedChapter = ch.chapterIndex },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("chapter_chip_${ch.chapterIndex}")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${ch.chapterIndex}. ${ch.localizedName(language.code)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        chapterPage = if (chapterPage == 0) 1 else 0
                        selectedChapter = chapterPage * 5 + 1
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .testTag("chapter_page_next")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Planets")
                }
            }

            // Compact Chapter Overview Banner with uniform badge
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                colors = CardDefaults.cardColors(containerColor = Color(chapter.skyBottomHex)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (chapterBg != null) {
                        Image(
                            bitmap = chapterBg,
                            contentDescription = chapter.localizedName(language.code),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.45f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            UniformAssetBadge(
                                bitmapRegistry = bitmapRegistry,
                                assetPath = when (chapter.chapterIndex) {
                                    1 -> "images/moon_guardians_logo.png"
                                    4 -> "images/iran_flag_on_saturn.png"
                                    9 -> "images/starspawn_flag.png"
                                    else -> "images/rocket_saturn.png"
                                },
                                sizeDp = 48.dp,
                                cornerRadius = 10.dp,
                                innerPadding = 4.dp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = chapter.localizedName(language.code),
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = chapter.subtitleFa,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 14.sp),
                                    color = Color(0xFFF5E6C8),
                                    maxLines = 2
                                )
                            }
                        }
                        Text(
                            text = "G = ${chapter.gravity}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(chapter.accentHex),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Fixed 5-Stage Chapter Grid (5 distinct stages per chapter, Zero Scroll, Zero Ellipsis)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chapterStages.forEach { st ->
                    val unlocked = st.stageNumber <= clampedUnlocked
                    val record = stageRecords[st.stageNumber]
                    val stars = record?.stars ?: 0
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (unlocked) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                        border = BorderStroke(
                            width = if (st.hasBoss) 2.dp else 1.dp,
                            color = if (!unlocked) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            else if (st.hasBoss) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                if (unlocked) {
                                    onSelectStage(st.stageNumber)
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar(strings.stageLockedMsg) }
                                }
                            }
                            .testTag("stage_card_${st.stageNumber}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${st.stageNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    if (st.hasDynamicDarkness) {
                                        Icon(Icons.Default.Brightness2, contentDescription = "Dark", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                    }
                                    if (st.hasBoss) {
                                        Icon(Icons.Default.Warning, contentDescription = "Boss", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                    if (!unlocked) {
                                        Icon(Icons.Default.Lock, contentDescription = strings.lockedLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (language == AppLanguage.ENGLISH) st.titleEn else st.titleFa,
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp, lineHeight = 16.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 3
                                )
                                if (st.hasBoss) {
                                    Text(
                                        text = if (language == AppLanguage.ENGLISH) "Chapter Finale" else "باس نهایی فصل",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(3) { idx ->
                                    Icon(
                                        imageVector = if (idx < stars) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (idx < stars) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
