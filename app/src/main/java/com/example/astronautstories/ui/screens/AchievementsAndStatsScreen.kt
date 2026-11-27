package com.example.astronautstories.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.astronautstories.R
import com.example.astronautstories.assets_loader.NativeBitmapRegistry
import com.example.astronautstories.data.local.AchievementEntity
import com.example.astronautstories.data.local.PlayerProgressEntity
import com.example.astronautstories.data.local.StageRecordEntity
import com.example.astronautstories.domain.model.GameCatalog
import com.example.astronautstories.domain.model.StoreAndAchievements
import com.example.astronautstories.localization.AppLanguage
import com.example.astronautstories.localization.GameStrings
import com.example.astronautstories.ui.components.UniformAssetBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsAndStatsScreen(
    initialTab: Int,
    language: AppLanguage,
    strings: GameStrings,
    progress: PlayerProgressEntity,
    stageRecords: Map<Int, StageRecordEntity>,
    achievements: List<AchievementEntity>,
    bitmapRegistry: NativeBitmapRegistry,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var achPage by remember { mutableIntStateOf(0) }
    var statsPage by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val achMap = remember(achievements) { achievements.associateBy { it.achievementId } }

    fun shareText(message: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, strings.appTitle)
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(intent, strings.shareBtn))
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) strings.achievementsTitle else strings.analyticsTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("achievements_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backBtn)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = strings.achievementsTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                    modifier = Modifier.testTag("tab_achievements")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = strings.analyticsTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    modifier = Modifier.testTag("tab_analytics")
                )
            }

            if (selectedTab == 0) {
                val unlockedCount = achievements.count { it.unlocked }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val colCount = when {
                        maxWidth >= 720.dp -> 3
                        maxWidth >= 480.dp -> 2
                        else -> 1
                    }
                    val rowCount = if (maxHeight >= 310.dp) 2 else 1
                    val pageSize = colCount * rowCount
                    val totalPages = ((StoreAndAchievements.achievements.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                    val clampedPage = achPage.coerceIn(0, totalPages - 1)
                    val pageItems = StoreAndAchievements.achievements.drop(clampedPage * pageSize).take(pageSize)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    UniformAssetBadge(
                                        bitmapRegistry = bitmapRegistry,
                                        assetPath = "images/army_of_stars_logo.png",
                                        sizeDp = 34.dp,
                                        cornerRadius = 8.dp,
                                        innerPadding = 3.dp
                                    )
                                    Text(
                                        text = "${strings.unlockedLabel}: $unlockedCount / ${StoreAndAchievements.achievements.size}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (clampedPage > 0) achPage = clampedPage - 1 },
                                        enabled = clampedPage > 0,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("ach_prev_page")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                                    }
                                    Text(
                                        text = "${clampedPage + 1} / $totalPages",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { if (clampedPage < totalPages - 1) achPage = clampedPage + 1 },
                                        enabled = clampedPage < totalPages - 1,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("ach_next_page")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val rows = pageItems.chunked(colCount)
                            for (r in 0 until rowCount) {
                                val rowCats = rows.getOrNull(r) ?: emptyList()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (c in 0 until colCount) {
                                        val cat = rowCats.getOrNull(c)
                                        if (cat == null) {
                                            Spacer(Modifier.weight(1f))
                                        } else {
                                            val state = achMap[cat.id]
                                            val cur = state?.progress ?: 0
                                            val unlocked = state?.unlocked == true
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surface
                                                ),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (unlocked) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                                ),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(10.dp),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = cat.localizedTitle(language.code),
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Spacer(Modifier.height(2.dp))
                                                            Text(
                                                                text = cat.descFa,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = "+${cat.rewardCrystals}💎",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1
                                                        )
                                                    }

                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        LinearProgressIndicator(
                                                            progress = { (cur.toFloat() / cat.targetCount.toFloat()).coerceIn(0f, 1f) },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(6.dp)
                                                                .clip(RoundedCornerShape(3.dp))
                                                        )
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .heightIn(min = 48.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "$cur / ${cat.targetCount}",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                maxLines = 1
                                                            )
                                                            if (unlocked) {
                                                                IconButton(
                                                                    onClick = {
                                                                        shareText("${strings.appTitle} — ${cat.localizedTitle(language.code)} (${strings.unlockedLabel})!")
                                                                    },
                                                                    modifier = Modifier
                                                                        .size(48.dp)
                                                                        .testTag("share_ach_${cat.id}")
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Share,
                                                                        contentDescription = strings.shareBtn,
                                                                        modifier = Modifier.size(20.dp)
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
                            }
                        }
                    }
                }
            } else {
                val accuracy = if (progress.totalShotsFired > 0) (progress.totalShotsHit * 100) / progress.totalShotsFired else 0
                val totalStars = stageRecords.values.sumOf { it.stars }
                val clampedUnlocked = progress.unlockedStage.coerceIn(1, GameCatalog.TOTAL_STAGES)
                val minsPlayed = progress.totalPlayTimeSeconds / 60L

                val kpis = listOf(
                    (when (language) {
                        AppLanguage.ENGLISH -> "Commander Level"
                        AppLanguage.ARABIC -> "مستوى القائد"
                        AppLanguage.PERSIAN -> "سطح فرمانده"
                    }) to "${progress.level} (${progress.xp} XP)",
                    (when (language) {
                        AppLanguage.ENGLISH -> "Story Unlocked"
                        AppLanguage.ARABIC -> "مراحل القصة"
                        AppLanguage.PERSIAN -> "مراحل داستانی"
                    }) to "$clampedUnlocked / ${GameCatalog.TOTAL_STAGES}",
                    (when (language) {
                        AppLanguage.ENGLISH -> "Total Stars"
                        AppLanguage.ARABIC -> "مجموع النجوم"
                        AppLanguage.PERSIAN -> "مجموع ستاره‌ها"
                    }) to "$totalStars / ${GameCatalog.MAX_STARS}",
                    (when (language) {
                        AppLanguage.ENGLISH -> "Total Score"
                        AppLanguage.ARABIC -> "مجموع النقاط"
                        AppLanguage.PERSIAN -> "امتیاز کل"
                    }) to "${progress.totalScore}",
                    (when (language) {
                        AppLanguage.ENGLISH -> "Endless Best"
                        AppLanguage.ARABIC -> "أفضل لا نهائي"
                        AppLanguage.PERSIAN -> "رکورد بی‌پایان"
                    }) to "${progress.endlessBestScore}",
                    (when (language) {
                        AppLanguage.ENGLISH -> "Challenge Best"
                        AppLanguage.ARABIC -> "أفضل تحدي"
                        AppLanguage.PERSIAN -> "رکورد چالش"
                    }) to "${progress.challengeBestScore}",
                    (when (language) {
                        AppLanguage.ENGLISH -> "Aliens Defeated"
                        AppLanguage.ARABIC -> "الأعداء المهزومون"
                        AppLanguage.PERSIAN -> "دشمنان شکست‌خورده"
                    }) to "${progress.enemiesDefeated}",
                    (when (language) {
                        AppLanguage.ENGLISH -> "Bosses Defeated"
                        AppLanguage.ARABIC -> "الزعماء المهزومون"
                        AppLanguage.PERSIAN -> "باس‌های شکست‌خورده"
                    }) to "${progress.bossesDefeated}",
                    (when (language) {
                        AppLanguage.ENGLISH -> "Accuracy"
                        AppLanguage.ARABIC -> "دقة التصويب"
                        AppLanguage.PERSIAN -> "دقت تیراندازی"
                    }) to "$accuracy%",
                    (when (language) {
                        AppLanguage.ENGLISH -> "Flight Time"
                        AppLanguage.ARABIC -> "وقت المهمة"
                        AppLanguage.PERSIAN -> "زمان مأموریت"
                    }) to "${minsPlayed}m ${progress.totalPlayTimeSeconds % 60L}s"
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val colCount = when {
                        maxWidth >= 720.dp -> 4
                        maxWidth >= 480.dp -> 2
                        else -> 1
                    }
                    val rowCount = when {
                        maxHeight >= 315.dp -> 3
                        maxHeight >= 210.dp -> 2
                        else -> 1
                    }
                    val totalCells = kpis.size + 2
                    val pageSize = colCount * rowCount
                    val totalPages = ((totalCells + pageSize - 1) / pageSize).coerceAtLeast(1)
                    val clampedPage = statsPage.coerceIn(0, totalPages - 1)
                    val pageStart = clampedPage * pageSize

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (r in 0 until rowCount) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (c in 0 until colCount) {
                                        val cellIndex = pageStart + r * colCount + c
                                        when {
                                            cellIndex < kpis.size -> {
                                                val (label, value) = kpis[cellIndex]
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(10.dp),
                                                        verticalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = value,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                            cellIndex == kpis.size -> {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        UniformAssetBadge(
                                                            bitmapRegistry = bitmapRegistry,
                                                            drawableRes = R.drawable.img_analytics_emblem,
                                                            sizeDp = 40.dp,
                                                            cornerRadius = 10.dp,
                                                            darkBackdrop = true,
                                                            cropToBounds = true
                                                        )
                                                        Text(
                                                            text = "💎 ${progress.crystals}",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                            cellIndex == kpis.size + 1 -> {
                                                Button(
                                                    onClick = {
                                                        shareText("${strings.appTitle}: Level ${progress.level}, Stage $clampedUnlocked/${GameCatalog.TOTAL_STAGES}, Score ${progress.totalScore}, Accuracy $accuracy%!")
                                                    },
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .testTag("share_career_stats_button")
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = strings.shareBtn,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            else -> {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (totalPages > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (clampedPage > 0) statsPage = clampedPage - 1 },
                                    enabled = clampedPage > 0,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("stats_prev_page")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "${clampedPage + 1} / $totalPages",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(12.dp))
                                IconButton(
                                    onClick = { if (clampedPage < totalPages - 1) statsPage = clampedPage + 1 },
                                    enabled = clampedPage < totalPages - 1,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("stats_next_page")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
