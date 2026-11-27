package com.example.astronautstories.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astronautstories.R
import com.example.astronautstories.assets_loader.NativeBitmapRegistry
import com.example.astronautstories.data.local.AppSettings
import com.example.astronautstories.data.local.PlayerProgressEntity
import com.example.astronautstories.domain.model.GameCatalog
import com.example.astronautstories.domain.model.GameMode
import com.example.astronautstories.domain.model.HeroId
import com.example.astronautstories.localization.AppLanguage
import com.example.astronautstories.localization.GameStrings
import com.example.astronautstories.ui.components.AssetSpriteImage
import com.example.astronautstories.ui.components.UniformAssetBadge

private data class QuickTileSpec(
    val title: String,
    val tag: String,
    val assetPath: String? = null,
    @DrawableRes val drawableRes: Int? = null,
    val darkBackdrop: Boolean = false,
    val cropToBounds: Boolean = false
)

@Composable
fun CommandHubScreen(
    settings: AppSettings,
    progress: PlayerProgressEntity,
    ownedItemIds: Set<String>,
    strings: GameStrings,
    bitmapRegistry: NativeBitmapRegistry,
    onSelectHero: (HeroId) -> Unit,
    onOpenSolarMap: () -> Unit,
    onLaunchMode: (GameMode, Int) -> Unit,
    onOpenShop: (Int) -> Unit,
    onOpenAchievements: (Int) -> Unit,
    onOpenAcademy: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val xpTarget = 150 + (progress.level - 1) * 90
    val xpRatio = (progress.xp.toFloat() / xpTarget.toFloat()).coerceIn(0f, 1f)
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))

    val initialHeroPage = remember(progress.selectedHeroId) {
        val idx = HeroId.entries.indexOfFirst { it.id == progress.selectedHeroId }.coerceAtLeast(0)
        if (idx >= 4) 1 else 0
    }
    var heroPage by remember { mutableIntStateOf(initialHeroPage) }
    var tilePage by remember { mutableIntStateOf(0) }
    var narrowPanelTab by remember { mutableIntStateOf(0) }
    val visibleHeroes = remember(heroPage) {
        HeroId.entries.drop(heroPage * 4).take(4)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        val isNarrow = maxWidth < 540.dp
        val isCompactHeight = maxHeight < 385.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 6.dp else 8.dp)
        ) {
            // 1. Top Commander Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    ) {
                        UniformAssetBadge(
                            bitmapRegistry = bitmapRegistry,
                            drawableRes = R.drawable.img_app_logo,
                            contentDescription = strings.appTitle,
                            sizeDp = 34.dp,
                            cornerRadius = 8.dp,
                            darkBackdrop = true,
                            cropToBounds = true
                        )
                        UniformAssetBadge(
                            bitmapRegistry = bitmapRegistry,
                            assetPath = "images/astronaut.png",
                            contentDescription = settings.playerDisplayName,
                            sizeDp = 34.dp,
                            cornerRadius = 17.dp,
                            innerPadding = 3.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = settings.playerDisplayName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = "${strings.levelLabel} ${progress.level} • ${progress.xp}/$xpTarget ${strings.xpLabel}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { xpRatio },
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "💎 ${progress.crystals}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenAcademy,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("hub_academy_button")
                        ) {
                            Icon(
                                Icons.Default.Movie,
                                contentDescription = strings.academyTitle,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("hub_settings_button")
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = strings.settingsTitle,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            if (isNarrow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HubPanelToggleButton(
                        label = strings.startMission,
                        selected = narrowPanelTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { narrowPanelTab = 0 }
                    )
                    HubPanelToggleButton(
                        label = strings.selectHero,
                        selected = narrowPanelTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { narrowPanelTab = 1 }
                    )
                }
            }

            // 2. Main Zero-Scroll Landscape Cockpit
            val heroMissionCard: @Composable (Modifier) -> Unit = { cardMod ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                    modifier = cardMod
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AssetSpriteImage(
                            bitmapRegistry = bitmapRegistry,
                            assetPath = "images/backgrounds/space_station.png",
                            contentDescription = strings.appTitle,
                            contentScale = ContentScale.Crop,
                            maxDimension = 768,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0x5009090D), Color(0xF209090D))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (isCompactHeight) 10.dp else 12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.appTitle,
                                    style = if (isCompactHeight) {
                                        MaterialTheme.typography.titleMedium
                                    } else {
                                        MaterialTheme.typography.titleLarge
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF5E6C8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )
                                UniformAssetBadge(
                                    bitmapRegistry = bitmapRegistry,
                                    assetPath = "images/astronaut.png",
                                    contentDescription = strings.selectHero,
                                    sizeDp = if (isCompactHeight) 40.dp else 52.dp,
                                    cornerRadius = 12.dp,
                                    innerPadding = 4.dp
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 4.dp else 6.dp)
                            ) {
                                Text(
                                    text = strings.storyIntro,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = if (isCompactHeight) 11.sp else 11.5.sp,
                                        lineHeight = if (isCompactHeight) 14.5.sp else 16.sp
                                    ),
                                    color = Color(0xFFDCC9AE),
                                    maxLines = if (isCompactHeight) 2 else 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val clampedUnlocked = progress.unlockedStage.coerceIn(1, GameCatalog.TOTAL_STAGES)
                                Button(
                                    onClick = { onLaunchMode(GameMode.STORY, clampedUnlocked) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("hub_continue_story_button")
                                ) {
                                    Icon(Icons.Default.RocketLaunch, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${strings.startMission} ($clampedUnlocked/${GameCatalog.TOTAL_STAGES})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val rightModesAndHeroesPanel: @Composable (Modifier) -> Unit = { panelMod ->
                Column(
                    modifier = panelMod,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val botSubtitle = when (settings.language) {
                        AppLanguage.ENGLISH -> "Bot Squad"
                        AppLanguage.ARABIC -> "منافسة الروبوت"
                        AppLanguage.PERSIAN -> "رقابت با ربات"
                    }
                    val blitzSubtitle = when (settings.language) {
                        AppLanguage.ENGLISH -> "90s Blitz"
                        AppLanguage.ARABIC -> "٩٠ ثانية"
                        AppLanguage.PERSIAN -> "۹۰ ثانیه"
                    }
                    val clampedUnlocked = progress.unlockedStage.coerceIn(1, GameCatalog.TOTAL_STAGES)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModeActionCard(
                                title = strings.storyMode,
                                subtitle = "$clampedUnlocked / ${GameCatalog.TOTAL_STAGES}",
                                assetPath = "images/moon_guardians_logo.png",
                                bitmapRegistry = bitmapRegistry,
                                accent = MaterialTheme.colorScheme.primary,
                                compact = isCompactHeight,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("mode_story_button"),
                                onClick = onOpenSolarMap
                            )
                            ModeActionCard(
                                title = strings.endlessMode,
                                subtitle = "Best: ${progress.endlessBestScore}",
                                assetPath = "images/alien_ship.png",
                                bitmapRegistry = bitmapRegistry,
                                accent = MaterialTheme.colorScheme.tertiary,
                                compact = isCompactHeight,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("mode_endless_button"),
                                onClick = { onLaunchMode(GameMode.ENDLESS, progress.unlockedStage) }
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModeActionCard(
                                title = strings.challengeMode,
                                subtitle = blitzSubtitle,
                                assetPath = "images/alien_ship2.png",
                                bitmapRegistry = bitmapRegistry,
                                accent = MaterialTheme.colorScheme.secondary,
                                compact = isCompactHeight,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("mode_challenge_button"),
                                onClick = { onLaunchMode(GameMode.CHALLENGE, progress.unlockedStage.coerceAtLeast(3)) }
                            )
                            ModeActionCard(
                                title = strings.multiplayerMode,
                                subtitle = botSubtitle,
                                assetPath = "images/astronauts_arguing.png",
                                bitmapRegistry = bitmapRegistry,
                                accent = MaterialTheme.colorScheme.primary,
                                compact = isCompactHeight,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("mode_multiplayer_button"),
                                onClick = { onLaunchMode(GameMode.MULTIPLAYER, progress.unlockedStage) }
                            )
                        }
                    }

                    // Paginated 8-Hero Selector (4 visible)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactHeight) 66.dp else 74.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { heroPage = if (heroPage == 0) 1 else 0 },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .testTag("hero_page_prev")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Heroes")
                        }

                        visibleHeroes.forEach { hero ->
                            val isUnlocked = hero.unlockCost == 0 || ownedItemIds.contains("hero_${hero.id}")
                            val isSelected = progress.selectedHeroId == hero.id
                            val spritePath = heroAssetPath(hero)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        if (isUnlocked) onSelectHero(hero) else onOpenShop(0)
                                    }
                                    .testTag("hero_card_${hero.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        UniformAssetBadge(
                                            bitmapRegistry = bitmapRegistry,
                                            assetPath = spritePath,
                                            contentDescription = hero.localizedName(settings.language.code),
                                            sizeDp = if (isCompactHeight) 28.dp else 32.dp,
                                            cornerRadius = 8.dp,
                                            innerPadding = 3.dp
                                        )
                                        if (!isUnlocked) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = strings.lockedLabel,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(12.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = hero.localizedName(settings.language.code),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            lineHeight = 12.sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "HP ${hero.bonusHp}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            lineHeight = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { heroPage = if (heroPage == 0) 1 else 0 },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .testTag("hero_page_next")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Heroes")
                        }
                    }
                }
            }

            if (isNarrow) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (narrowPanelTab == 0) {
                        heroMissionCard(Modifier.fillMaxSize())
                    } else {
                        rightModesAndHeroesPanel(Modifier.fillMaxSize())
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    heroMissionCard(
                        Modifier
                            .weight(0.92f)
                            .fillMaxHeight()
                    )
                    rightModesAndHeroesPanel(
                        Modifier
                            .weight(1.18f)
                            .fillMaxHeight()
                    )
                }
            }

            // 3. Bottom Quick Modules Bar (Button-Paginated on Narrow Screens, All 6 on Normal Landscape)
            val quickTiles = listOf(
                QuickTileSpec(
                    title = strings.shopTitle,
                    tag = "tile_shop",
                    assetPath = "images/gun.png"
                ) to { onOpenShop(0) },
                QuickTileSpec(
                    title = "${strings.skillsTitle} (${progress.skillPoints})",
                    tag = "tile_skills",
                    assetPath = "images/powerful_battery.png"
                ) to { onOpenShop(1) },
                QuickTileSpec(
                    title = strings.achievementsTitle,
                    tag = "tile_achievements",
                    assetPath = "images/army_of_stars_logo.png"
                ) to { onOpenAchievements(0) },
                QuickTileSpec(
                    title = strings.analyticsTitle,
                    tag = "tile_analytics",
                    drawableRes = R.drawable.img_analytics_emblem,
                    darkBackdrop = true,
                    cropToBounds = true
                ) to { onOpenAchievements(1) },
                QuickTileSpec(
                    title = strings.academyTitle,
                    tag = "tile_academy",
                    drawableRes = R.drawable.img_academy_emblem,
                    darkBackdrop = true,
                    cropToBounds = true
                ) to onOpenAcademy,
                QuickTileSpec(
                    title = strings.settingsTitle,
                    tag = "tile_settings",
                    drawableRes = R.drawable.img_settings_emblem,
                    darkBackdrop = true,
                    cropToBounds = true
                ) to onOpenSettings
            )

            val tilesPerPage = if (isNarrow) 3 else 6
            val totalTilePages = (quickTiles.size + tilesPerPage - 1) / tilesPerPage
            val clampedTilePage = tilePage.coerceIn(0, totalTilePages - 1)
            val visibleQuickTiles = quickTiles.drop(clampedTilePage * tilesPerPage).take(tilesPerPage)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompactHeight) 56.dp else 64.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (totalTilePages > 1) {
                    IconButton(
                        onClick = { tilePage = if (clampedTilePage == 0) totalTilePages - 1 else clampedTilePage - 1 },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .testTag("hub_tiles_prev")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Modules")
                    }
                }

                visibleQuickTiles.forEach { (spec, action) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = cardBorder,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(onClick = action)
                            .testTag(spec.tag)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            UniformAssetBadge(
                                bitmapRegistry = bitmapRegistry,
                                assetPath = spec.assetPath,
                                drawableRes = spec.drawableRes,
                                sizeDp = if (isCompactHeight) 24.dp else 28.dp,
                                cornerRadius = 7.dp,
                                innerPadding = 3.dp,
                                darkBackdrop = spec.darkBackdrop,
                                cropToBounds = spec.cropToBounds
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = spec.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = if (isCompactHeight) 10.sp else 10.5.sp,
                                    lineHeight = 12.sp
                                ),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = if (isCompactHeight) 1 else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (totalTilePages > 1) {
                    IconButton(
                        onClick = { tilePage = if (clampedTilePage >= totalTilePages - 1) 0 else clampedTilePage + 1 },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .testTag("hub_tiles_next")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Modules")
                    }
                }
            }
        }
    }
}

@Composable
private fun HubPanelToggleButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun heroAssetPath(hero: HeroId): String = when (hero) {
    HeroId.KOUROSH -> "images/astronaut.png"
    HeroId.DARIUSH -> "images/player_idle_gun.png"
    HeroId.SOHRAB -> "images/player_walk_gun.png"
    HeroId.ROSTAM -> "images/player_crouch_gun.png"
    HeroId.ARASH -> "images/player_jump_gun.png"
    HeroId.ATOUSA -> "images/player_idle_no_gun.png"
    HeroId.KAVEH -> "images/astronauts_happy.png"
    HeroId.GORDAFARID -> "images/player_jump_no_gun.png"
}

@Composable
private fun ModeActionCard(
    title: String,
    subtitle: String,
    assetPath: String,
    bitmapRegistry: NativeBitmapRegistry,
    accent: Color,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.6f)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        if (compact) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UniformAssetBadge(
                    bitmapRegistry = bitmapRegistry,
                    assetPath = assetPath,
                    sizeDp = 32.dp,
                    cornerRadius = 8.dp,
                    innerPadding = 3.dp,
                    borderColor = accent.copy(alpha = 0.7f)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UniformAssetBadge(
                        bitmapRegistry = bitmapRegistry,
                        assetPath = assetPath,
                        sizeDp = 34.dp,
                        cornerRadius = 8.dp,
                        innerPadding = 3.dp,
                        borderColor = accent.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
