package com.example.astronautstories.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.astronautstories.assets_loader.NativeBitmapRegistry
import com.example.astronautstories.data.local.PlayerProgressEntity
import com.example.astronautstories.domain.model.ShopCategory
import com.example.astronautstories.domain.model.ShopItemCatalog
import com.example.astronautstories.domain.model.SkillType
import com.example.astronautstories.domain.model.StoreAndAchievements
import com.example.astronautstories.localization.AppLanguage
import com.example.astronautstories.localization.GameStrings
import com.example.astronautstories.ui.components.AssetSpriteImage
import com.example.astronautstories.ui.components.UniformAssetBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopAndSkillsScreen(
    initialTab: Int,
    language: AppLanguage,
    strings: GameStrings,
    progress: PlayerProgressEntity,
    ownedItemIds: Set<String>,
    skillLevels: Map<String, Int>,
    bitmapRegistry: NativeBitmapRegistry,
    onPurchaseOrEquip: (ShopItemCatalog, (Boolean) -> Unit) -> Unit,
    onUpgradeSkill: (SkillType, (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var selectedCategory by remember { mutableStateOf<ShopCategory?>(null) }
    var shopPage by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTab == 0) strings.shopTitle else strings.skillsTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("shop_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.backBtn)
                    }
                },
                actions = {
                    Text(
                        text = if (selectedTab == 0) "💎 ${progress.crystals}" else "⚡ ${progress.skillPoints}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
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
                    text = { Text(strings.shopTitle, maxLines = 1) },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                    modifier = Modifier.testTag("tab_shop")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${strings.skillsTitle} (${progress.skillPoints})", maxLines = 1) },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    modifier = Modifier.testTag("tab_skills")
                )
            }

            if (selectedTab == 0) {
                val filteredItems = remember(selectedCategory) {
                    if (selectedCategory == null) StoreAndAchievements.shopItems
                    else StoreAndAchievements.shopItems.filter { it.category == selectedCategory }
                }
                val totalPages = ((filteredItems.size + 3) / 4).coerceAtLeast(1)
                val clampedPage = shopPage.coerceIn(0, totalPages - 1)
                val pageItems = filteredItems.drop(clampedPage * 4).take(4)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        ) {
                            AssetSpriteImage(
                                bitmapRegistry = bitmapRegistry,
                                assetPath = "images/Inside_the_ship.jpg",
                                contentDescription = strings.shopTitle,
                                contentScale = ContentScale.Crop,
                                maxDimension = 512,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.horizontalGradient(listOf(Color(0xD809090D), Color(0x9909090D))))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "💊 ${progress.medkitBoosters}  •  🔋 ${progress.ammoBoosters}  •  🛡 ${progress.shieldBoosters}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFF5E6C8),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.weight(1.3f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactFilterPill(
                                label = if (language == AppLanguage.ENGLISH) "All" else "همه",
                                selected = selectedCategory == null,
                                tag = "shop_filter_all",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedCategory = null
                                    shopPage = 0
                                }
                            )
                            ShopCategory.entries.forEach { cat ->
                                val label = when (cat) {
                                    ShopCategory.WEAPONS -> if (language == AppLanguage.ENGLISH) "Weapons" else "سلاح‌ها"
                                    ShopCategory.HEROES -> if (language == AppLanguage.ENGLISH) "Heroes" else "قهرمان‌ها"
                                    ShopCategory.BOOSTERS -> if (language == AppLanguage.ENGLISH) "Boosts" else "تقویت‌گر"
                                }
                                CompactFilterPill(
                                    label = label,
                                    selected = selectedCategory == cat,
                                    tag = "shop_filter_${cat.name}",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedCategory = cat
                                        shopPage = 0
                                    }
                                )
                            }
                        }
                    }

                    // Fixed 2x2 Grid for Shop Items
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val rows = pageItems.chunked(2)
                        for (r in 0 until 2) {
                            val pair = rows.getOrNull(r) ?: emptyList()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (c in 0 until 2) {
                                    val item = pair.getOrNull(c)
                                    if (item == null) {
                                        Spacer(Modifier.weight(1f))
                                    } else {
                                        val isBooster = item.category == ShopCategory.BOOSTERS
                                        val isOwned = !isBooster && ownedItemIds.contains(item.id)
                                        val isEquipped = (item.weaponType != null && progress.equippedWeaponId == item.weaponType.id) ||
                                            (item.heroId != null && progress.selectedHeroId == item.heroId.id)

                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                UniformAssetBadge(
                                                    bitmapRegistry = bitmapRegistry,
                                                    assetPath = shopItemAssetPath(item),
                                                    contentDescription = item.localizedTitle(language.code),
                                                    sizeDp = 46.dp,
                                                    cornerRadius = 10.dp,
                                                    innerPadding = 4.dp
                                                )
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        text = item.localizedTitle(language.code),
                                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp, lineHeight = 15.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 2
                                                    )
                                                    Text(
                                                        text = item.descFa,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2
                                                    )
                                                }
                                                if (isEquipped) {
                                                    FilledTonalButton(
                                                        onClick = {},
                                                        enabled = false,
                                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                                        modifier = Modifier.height(44.dp).testTag("shop_btn_${item.id}")
                                                    ) {
                                                        Text(strings.equippedLabel, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            onPurchaseOrEquip(item) { ok ->
                                                                if (!ok) {
                                                                    scope.launch {
                                                                        snackbarHostState.showSnackbar(
                                                                            if (language == AppLanguage.ENGLISH) "Not enough crystals!"
                                                                            else "کریستال کافی نیست!"
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                                        modifier = Modifier.height(44.dp).testTag("shop_btn_${item.id}")
                                                    ) {
                                                        Text(
                                                            text = if (isOwned) strings.equipBtn else "${item.priceCrystals}💎",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            maxLines = 1
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (clampedPage > 0) shopPage = clampedPage - 1 },
                            enabled = clampedPage > 0,
                            modifier = Modifier.size(48.dp).testTag("shop_prev_page")
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
                            onClick = { if (clampedPage < totalPages - 1) shopPage = clampedPage + 1 },
                            enabled = clampedPage < totalPages - 1,
                            modifier = Modifier.size(48.dp).testTag("shop_next_page")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                        }
                    }
                }
            } else {
                // Fixed 3x2 Grid for ALL 6 Skills
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkillType.entries.chunked(3).forEach { rowSkills ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSkills.forEach { skill ->
                                val lvl = skillLevels[skill.id] ?: 0
                                val maxed = lvl >= skill.maxLevel
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
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
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            UniformAssetBadge(
                                                bitmapRegistry = bitmapRegistry,
                                                assetPath = skillAssetPath(skill),
                                                sizeDp = 38.dp,
                                                cornerRadius = 9.dp,
                                                innerPadding = 4.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${skill.localizedTitle(language.code)} ($lvl/${skill.maxLevel})",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp, lineHeight = 14.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2
                                                )
                                                Text(
                                                    text = skill.descFa,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, lineHeight = 13.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            LinearProgressIndicator(
                                                progress = { (lvl.toFloat() / skill.maxLevel.toFloat()).coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                            )
                                            Button(
                                                onClick = {
                                                    onUpgradeSkill(skill) { ok ->
                                                        if (!ok) {
                                                            scope.launch {
                                                                snackbarHostState.showSnackbar(
                                                                    if (language == AppLanguage.ENGLISH) "No Skill Points left or maxed!"
                                                                    else "امتیاز مهارت کافی نیست یا به حداکثر رسیده است!"
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                enabled = !maxed && progress.skillPoints > 0,
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(40.dp)
                                                    .testTag("skill_upgrade_${skill.id}")
                                            ) {
                                                Text(if (maxed) "MAX" else strings.upgradeBtn)
                                            }
                                        }
                                    }
                                }
                            }
                            repeat(3 - rowSkills.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFilterPill(
    label: String,
    selected: Boolean,
    tag: String,
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
        modifier = modifier
            .height(44.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

private fun shopItemAssetPath(item: ShopItemCatalog): String = when {
    item.category == ShopCategory.WEAPONS -> "images/gun.png"
    item.id == "hero_arash" -> "images/player_jump_gun.png"
    item.id == "hero_atousa" -> "images/player_idle_no_gun.png"
    item.id == "hero_kaveh" -> "images/astronauts_happy.png"
    item.id == "hero_gordafarid" -> "images/player_jump_no_gun.png"
    item.id == "booster_medkit" -> "images/health_pack.png"
    item.id == "booster_ammo" -> "images/ammo_pack.png"
    item.id == "booster_shield" -> "images/battery.png"
    else -> "images/astronaut.png"
}

private fun skillAssetPath(skill: SkillType): String = when (skill) {
    SkillType.SPEED -> "images/player_jump_gun.png"
    SkillType.FIREPOWER -> "images/gun.png"
    SkillType.ARMOR -> "images/astronaut.png"
    SkillType.OXYGEN_CAP -> "images/battery.png"
    SkillType.LUCK -> "images/powerful_battery.png"
    SkillType.DRONE -> "images/alien_ship.png"
}
