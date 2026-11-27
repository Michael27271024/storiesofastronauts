package com.example.astronautstories.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextOverflow
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
    var stagePage by remember(selectedChapter) { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val chapter = GameCatalog.chapters[selectedChapter - 1]
    val chapterStages = remember(selectedChapter) {
        GameCatalog.allStages.filter { it.chapterIndex == selectedChapter }
    }
    val totalStars = stageRecords.values.sumOf { it.stars }
    val chapterBg = bitmapRegistry.getBitmap(chapter.bgAssetPath, 768)

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    when (language) {
                        AppLanguage.ENGLISH -> "Reset Campaign?"
                        AppLanguage.ARABIC -> "إعادة ضبط الحملة؟"
                        AppLanguage.PERSIAN -> "بازنشانی پیشرفت مراحل؟"
                    }
                )
            },
            text = {
                Text(
                    when (language) {
                        AppLanguage.ENGLISH ->
                            "This will reset unlocked story stages back to Stage 1 while keeping your heroes and weapons."
                        AppLanguage.ARABIC ->
                            "سيؤدي هذا إلى إعادة مراحل القصة المفتوحة إلى المرحلة ١ مع الاحتفاظ بالأبطال والأسلحة."
                        AppLanguage.PERSIAN ->
                            "با این کار مراحل داستانی به مرحله ۱ بازمی‌گردند (قهرمان‌ها و سلاح‌های خریداری‌شده حفظ می‌شوند)."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetProgress()
                        selectedChapter = 1
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("confirm_reset_progress_button")
                ) {
                    Text(
                        when (language) {
                            AppLanguage.ENGLISH -> "Reset"
                            AppLanguage.ARABIC -> "تأكيد"
                            AppLanguage.PERSIAN -> "تایید بازنشانی"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.backBtn)
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.solarMapTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backBtn)
                    }
                },
                actions = {
                    Text(
                        text = "★ $totalStars/${GameCatalog.MAX_STARS}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_reset_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Reset Progress")
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
            val chaptersPerPage = if (maxWidth >= 640.dp) 5 else 3
            val totalChapterPages = ((GameCatalog.chapters.size + chaptersPerPage - 1) / chaptersPerPage).coerceAtLeast(1)
            val currentChapterPage = ((selectedChapter - 1) / chaptersPerPage).coerceIn(0, totalChapterPages - 1)
            val visibleChapters = GameCatalog.chapters.drop(currentChapterPage * chaptersPerPage).take(chaptersPerPage)

            val stagesPerPage = when {
                maxWidth >= 640.dp -> 5
                maxWidth >= 460.dp -> 3
                else -> 2
            }
            val totalStagePages = ((chapterStages.size + stagesPerPage - 1) / stagesPerPage).coerceAtLeast(1)
            val clampedStagePage = stagePage.coerceIn(0, totalStagePages - 1)
            val visibleStages = chapterStages.drop(clampedStagePage * stagesPerPage).take(stagesPerPage)
            val isCompactHeight = maxHeight < 290.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button-Paginated Chapter Selector Strip (Zero Scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            val prevPage = if (currentChapterPage == 0) totalChapterPages - 1 else currentChapterPage - 1
                            selectedChapter = prevPage * chaptersPerPage + 1
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
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
                                .heightIn(min = 48.dp)
                                .testTag("chapter_chip_${ch.chapterIndex}")
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${ch.chapterIndex}. ${ch.localizedName(language.code)}",
                                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            val nextPage = if (currentChapterPage >= totalChapterPages - 1) 0 else currentChapterPage + 1
                            selectedChapter = nextPage * chaptersPerPage + 1
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(10.dp)
                            )
                            .testTag("chapter_page_next")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Planets")
                    }
                }

                // Chapter Overview Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                    colors = CardDefaults.cardColors(containerColor = Color(chapter.skyBottomHex)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (chapterBg != null) {
                            Image(
                                bitmap = chapterBg,
                                contentDescription = chapter.localizedName(language.code),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(0.45f)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = if (isCompactHeight) 6.dp else 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UniformAssetBadge(
                                bitmapRegistry = bitmapRegistry,
                                assetPath = when (chapter.chapterIndex) {
                                    1 -> "images/moon_guardians_logo.png"
                                    4 -> "images/iran_flag_on_saturn.png"
                                    9 -> "images/starspawn_flag.png"
                                    else -> "images/rocket_saturn.png"
                                },
                                sizeDp = if (isCompactHeight) 40.dp else 48.dp,
                                cornerRadius = 10.dp,
                                innerPadding = 4.dp
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = chapter.localizedName(language.code),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = chapter.subtitleFa,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF5E6C8),
                                    maxLines = if (isCompactHeight) 1 else 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "G = ${chapter.gravity}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(chapter.accentHex),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Zero-Scroll Stage Cards Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visibleStages.forEach { st ->
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
                            shape = RoundedCornerShape(14.dp),
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
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                            Icon(
                                                Icons.Default.Brightness2,
                                                contentDescription = "Dark",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        if (st.hasBoss) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = "Boss",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        if (!unlocked) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = strings.lockedLabel,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (language == AppLanguage.ENGLISH) st.titleEn else st.titleFa,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = if (isCompactHeight) 2 else 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (st.hasBoss) {
                                        Text(
                                            text = when (language) {
                                                AppLanguage.ENGLISH -> "Chapter Finale"
                                                AppLanguage.ARABIC -> "زعيم الفصل"
                                                AppLanguage.PERSIAN -> "باس نهایی فصل"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                    repeat(stagesPerPage - visibleStages.size) { Spacer(Modifier.weight(1f)) }
                }

                // Stage Pagination Controls when narrow screen splits 5 stages into pages
                if (totalStagePages > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (clampedStagePage > 0) stagePage = clampedStagePage - 1 },
                            enabled = clampedStagePage > 0,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("stage_page_prev")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Stages")
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "${clampedStagePage + 1} / $totalStagePages",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = { if (clampedStagePage < totalStagePages - 1) stagePage = clampedStagePage + 1 },
                            enabled = clampedStagePage < totalStagePages - 1,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("stage_page_next")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Stages")
                        }
                    }
                }
            }
        }
    }
}
