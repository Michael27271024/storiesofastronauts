package com.example.astronautstories.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTab == 0) strings.achievementsTitle else strings.analyticsTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("achievements_back_button")
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
                    text = { Text(strings.achievementsTitle, maxLines = 1) },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                    modifier = Modifier.testTag("tab_achievements")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(strings.analyticsTitle, maxLines = 1) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    modifier = Modifier.testTag("tab_analytics")
                )
            }

            if (selectedTab == 0) {
                val unlockedCount = achievements.count { it.unlocked }
                val totalPages = ((StoreAndAchievements.achievements.size + 5) / 6).coerceAtLeast(1)
                val clampedPage = achPage.coerceIn(0, totalPages - 1)
                val pageItems = StoreAndAchievements.achievements.drop(clampedPage * 6).take(6)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (clampedPage > 0) achPage = clampedPage - 1 },
                                    enabled = clampedPage > 0,
                                    modifier = Modifier.size(48.dp).testTag("ach_prev_page")
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
                                    modifier = Modifier.size(48.dp).testTag("ach_next_page")
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
                        val rows = pageItems.chunked(3)
                        for (r in 0 until 2) {
                            val triple = rows.getOrNull(r) ?: emptyList()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (c in 0 until 3) {
                                    val cat = triple.getOrNull(c)
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
                                                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp, lineHeight = 14.sp),
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 2
                                                        )
                                                        Spacer(Modifier.height(2.dp))
                                                        Text(
                                                            text = cat.descFa,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, lineHeight = 13.sp),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 2
                                                        )
                                                    }
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = "+${cat.rewardCrystals}💎",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        fontWeight = FontWeight.Bold
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
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "$cur / ${cat.targetCount}",
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                        if (unlocked) {
                                                            IconButton(
                                                                onClick = {
                                                                    shareText("${strings.appTitle} — ${cat.localizedTitle(language.code)} (${strings.unlockedLabel})!")
                                                                },
                                                                modifier = Modifier.size(36.dp).testTag("share_ach_${cat.id}")
                                                            ) {
                                                                Icon(Icons.Default.Share, contentDescription = strings.shareBtn, modifier = Modifier.size(18.dp))
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
                    (if (language == AppLanguage.ENGLISH) "Commander Level" else "سطح فرمانده") to "${progress.level} (${progress.xp} XP)",
                    (if (language == AppLanguage.ENGLISH) "Story Unlocked" else "مراحل داستانی") to "$clampedUnlocked / ${GameCatalog.TOTAL_STAGES}",
                    (if (language == AppLanguage.ENGLISH) "Total Stars" else "مجموع ستاره‌ها") to "$totalStars / ${GameCatalog.MAX_STARS}",
                    (if (language == AppLanguage.ENGLISH) "Total Score" else "امتیاز کل") to "${progress.totalScore}",
                    (if (language == AppLanguage.ENGLISH) "Endless Best" else "رکورد بی‌پایان") to "${progress.endlessBestScore}",
                    (if (language == AppLanguage.ENGLISH) "Challenge Best" else "رکورد چالش") to "${progress.challengeBestScore}",
                    (if (language == AppLanguage.ENGLISH) "Aliens Defeated" else "دشمنان شکست‌خورده") to "${progress.enemiesDefeated}",
                    (if (language == AppLanguage.ENGLISH) "Bosses Defeated" else "باس‌های شکست‌خورده") to "${progress.bossesDefeated}",
                    (if (language == AppLanguage.ENGLISH) "Accuracy" else "دقت تیراندازی") to "$accuracy%",
                    (if (language == AppLanguage.ENGLISH) "Flight Time" else "زمان مأموریت") to "${minsPlayed}m ${progress.totalPlayTimeSeconds % 60L}s"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (r in 0 until 3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (c in 0 until 4) {
                                val cellIndex = r * 4 + c
                                when {
                                    cellIndex < kpis.size -> {
                                        val (label, value) = kpis[cellIndex]
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
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
                                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp, lineHeight = 14.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2
                                                )
                                                Text(
                                                    text = value,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                    cellIndex == 10 -> {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                UniformAssetBadge(
                                                    bitmapRegistry = bitmapRegistry,
                                                    drawableRes = R.drawable.img_analytics_emblem,
                                                    sizeDp = 42.dp,
                                                    cornerRadius = 10.dp,
                                                    darkBackdrop = true,
                                                    cropToBounds = true
                                                )
                                                Text(
                                                    text = "💎 ${progress.crystals}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        Button(
                                            onClick = {
                                                shareText("${strings.appTitle}: Level ${progress.level}, Stage $clampedUnlocked/${GameCatalog.TOTAL_STAGES}, Score ${progress.totalScore}, Accuracy $accuracy%!")
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .testTag("share_career_stats_button")
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text(strings.shareBtn, maxLines = 1)
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
