package com.example.astronautstories.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val visibleHeroes = remember(heroPage) {
        HeroId.entries.drop(heroPage * 4).take(4)
    }

    // Fixed, zero-scroll Landscape cockpit with complete non-truncated labels & uniform image badges
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    modifier = Modifier.weight(1f)
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
                                maxLines = 1
                            )
                            Text(
                                text = "${strings.levelLabel} ${progress.level} • ${progress.xp}/$xpTarget ${strings.xpLabel}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { xpRatio },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenAcademy,
                        modifier = Modifier.size(48.dp).testTag("hub_academy_button")
                    ) {
                        Icon(
                            Icons.Default.Movie,
                            contentDescription = strings.academyTitle,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp).testTag("hub_settings_button")
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

        // 2. Main Landscape Cockpit
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero Mission Card
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight()
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
                                    listOf(Color(0x4509090D), Color(0xF209090D))
                                )
                            )
                    )
                    UniformAssetBadge(
                        bitmapRegistry = bitmapRegistry,
                        assetPath = "images/astronaut.png",
                        contentDescription = strings.selectHero,
                        sizeDp = 56.dp,
                        cornerRadius = 14.dp,
                        innerPadding = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = strings.appTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5E6C8)
                        )
                        Text(
                            text = strings.storyIntro,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                            color = Color(0xFFDCC9AE)
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
                                .height(48.dp)
                                .testTag("hub_continue_story_button")
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${strings.startMission} ($clampedUnlocked/${GameCatalog.TOTAL_STAGES})",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Right Panel: 2x2 Modes + Paginated 4-Hero Squad Strip
            Column(
                modifier = Modifier
                    .weight(1.18f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val botSubtitle = if (settings.language == AppLanguage.ENGLISH) "Bot Squad" else "رقابت با ربات"
                    val blitzSubtitle = if (settings.language == AppLanguage.ENGLISH) "90s Blitz" else "۹۰ ثانیه"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val clampedUnlocked = progress.unlockedStage.coerceIn(1, GameCatalog.TOTAL_STAGES)
                        ModeActionCard(
                            title = strings.storyMode,
                            subtitle = "$clampedUnlocked / ${GameCatalog.TOTAL_STAGES}",
                            assetPath = "images/moon_guardians_logo.png",
                            bitmapRegistry = bitmapRegistry,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("mode_story_button"),
                            onClick = onOpenSolarMap
                        )
                        ModeActionCard(
                            title = strings.endlessMode,
                            subtitle = "Best: ${progress.endlessBestScore}",
                            assetPath = "images/alien_ship.png",
                            bitmapRegistry = bitmapRegistry,
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("mode_endless_button"),
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
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("mode_challenge_button"),
                            onClick = { onLaunchMode(GameMode.CHALLENGE, progress.unlockedStage.coerceAtLeast(3)) }
                        )
                        ModeActionCard(
                            title = strings.multiplayerMode,
                            subtitle = botSubtitle,
                            assetPath = "images/astronauts_arguing.png",
                            bitmapRegistry = bitmapRegistry,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("mode_multiplayer_button"),
                            onClick = { onLaunchMode(GameMode.MULTIPLAYER, progress.unlockedStage) }
                        )
                    }
                }

                // Paginated 8-Hero Selector (4 visible, centered portrait + full name)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { heroPage = if (heroPage == 0) 1 else 0 },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
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
                                        sizeDp = 32.dp,
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
                                                .size(13.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = hero.localizedName(settings.language.code),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                Text(
                                    text = "HP ${hero.bonusHp}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, lineHeight = 11.sp),
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
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .testTag("hero_page_next")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Heroes")
                    }
                }
            }
        }

        // 3. Bottom Quick Modules Bar (6 Fixed Stacked Tiles — Zero Ellipsis, Uniform Badges)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickTiles.forEach { (spec, action) ->
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
                            sizeDp = 28.dp,
                            cornerRadius = 7.dp,
                            innerPadding = 3.dp,
                            darkBackdrop = spec.darkBackdrop,
                            cropToBounds = spec.cropToBounds
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = spec.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                lineHeight = 13.sp
                            ),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.6f)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
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
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, lineHeight = 15.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
    }
}
