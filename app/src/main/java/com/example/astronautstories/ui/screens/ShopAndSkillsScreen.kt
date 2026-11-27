package com.example.astronautstories.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    var skillPage by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) strings.shopTitle else strings.skillsTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("shop_back_button")
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
                        maxLines = 1,
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
                    text = {
                        Text(
                            text = strings.shopTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                    modifier = Modifier.testTag("tab_shop")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "${strings.skillsTitle} (${progress.skillPoints})",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    modifier = Modifier.testTag("tab_skills")
                )
            }

            if (selectedTab == 0) {
                val filteredItems = remember(selectedCategory) {
                    if (selectedCategory == null) StoreAndAchievements.shopItems
                    else StoreAndAchievements.shopItems.filter { it.category == selectedCategory }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val isWideHeader = maxWidth >= 660.dp
                    val colCount = if (maxWidth >= 560.dp) 2 else 1
                    val rowCount = when {
                        colCount == 2 && maxHeight >= 295.dp -> 2
                        colCount == 1 && maxHeight >= 315.dp -> 2
                        else -> 1
                    }
                    val pageSize = colCount * rowCount
                    val totalPages = ((filteredItems.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                    val clampedPage = shopPage.coerceIn(0, totalPages - 1)
                    val pageItems = filteredItems.drop(clampedPage * pageSize).take(pageSize)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 1120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val boosterBanner: @Composable (Modifier) -> Unit = { bannerMod ->
                            Box(
                                modifier = bannerMod
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                AssetSpriteImage(
                                    bitmapRegistry = bitmapRegistry,
                                    assetPath = "images/Inside_the_ship.jpg",
                                    contentDescription = strings.shopTitle,
                                    contentScale = ContentScale.Crop,
                                    maxDimension = 512,
                                    modifier = Modifier.matchParentSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xD809090D), Color(0x9909090D))
                                            )
                                        )
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "💊 ${progress.medkitBoosters}  •  🔋 ${progress.ammoBoosters}  •  🛡 ${progress.shieldBoosters}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFF5E6C8),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        val filterPillsRow: @Composable (Modifier) -> Unit = { pillsMod ->
                            Row(
                                modifier = pillsMod,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompactFilterPill(
                                    label = when (language) {
                                        AppLanguage.ENGLISH -> "All"
                                        AppLanguage.ARABIC -> "الكل"
                                        AppLanguage.PERSIAN -> "همه"
                                    },
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
                                        ShopCategory.WEAPONS -> when (language) {
                                            AppLanguage.ENGLISH -> "Weapons"
                                            AppLanguage.ARABIC -> "الأسلحة"
                                            AppLanguage.PERSIAN -> "سلاح‌ها"
                                        }
                                        ShopCategory.HEROES -> when (language) {
                                            AppLanguage.ENGLISH -> "Heroes"
                                            AppLanguage.ARABIC -> "الأبطال"
                                            AppLanguage.PERSIAN -> "قهرمان‌ها"
                                        }
                                        ShopCategory.BOOSTERS -> when (language) {
                                            AppLanguage.ENGLISH -> "Boosts"
                                            AppLanguage.ARABIC -> "المعززات"
                                            AppLanguage.PERSIAN -> "تقویت‌گر"
                                        }
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

                        if (isWideHeader) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                boosterBanner(Modifier.weight(0.95f))
                                filterPillsRow(Modifier.weight(1.3f))
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                boosterBanner(Modifier.fillMaxWidth())
                                filterPillsRow(Modifier.fillMaxWidth())
                            }
                        }

                        // Zero-Scroll Adaptive Grid
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val rows = pageItems.chunked(colCount)
                            for (r in 0 until rowCount) {
                                val rowItems = rows.getOrNull(r) ?: emptyList()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (c in 0 until colCount) {
                                        val item = rowItems.getOrNull(c)
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
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = item.descFa,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    if (isEquipped) {
                                                        FilledTonalButton(
                                                            onClick = {},
                                                            enabled = false,
                                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                                            modifier = Modifier
                                                                .heightIn(min = 48.dp)
                                                                .testTag("shop_btn_${item.id}")
                                                        ) {
                                                            Text(
                                                                text = strings.equippedLabel,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    } else {
                                                        Button(
                                                            onClick = {
                                                                onPurchaseOrEquip(item) { ok ->
                                                                    if (!ok) {
                                                                        scope.launch {
                                                                            snackbarHostState.showSnackbar(
                                                                                when (language) {
                                                                                    AppLanguage.ENGLISH -> "Not enough crystals!"
                                                                                    AppLanguage.ARABIC -> "البلورات غير كافية!"
                                                                                    AppLanguage.PERSIAN -> "کریستال کافی نیست!"
                                                                                }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                                            modifier = Modifier
                                                                .heightIn(min = 48.dp)
                                                                .testTag("shop_btn_${item.id}")
                                                        ) {
                                                            Text(
                                                                text = if (isOwned) strings.equipBtn else "${item.priceCrystals}💎",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
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

                        // Button-Driven Pagination Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (clampedPage > 0) shopPage = clampedPage - 1 },
                                enabled = clampedPage > 0,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("shop_prev_page")
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
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("shop_next_page")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                            }
                        }
                    }
                }
            } else {
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
                    val allSkills = SkillType.entries
                    val totalPages = ((allSkills.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                    val clampedPage = skillPage.coerceIn(0, totalPages - 1)
                    val pageSkills = allSkills.drop(clampedPage * pageSize).take(pageSize)

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
                            val rows = pageSkills.chunked(colCount)
                            for (r in 0 until rowCount) {
                                val rowSkills = rows.getOrNull(r) ?: emptyList()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (c in 0 until colCount) {
                                        val skill = rowSkills.getOrNull(c)
                                        if (skill == null) {
                                            Spacer(Modifier.weight(1f))
                                        } else {
                                            val lvl = skillLevels[skill.id] ?: 0
                                            val maxed = lvl >= skill.maxLevel
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(12.dp),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        UniformAssetBadge(
                                                            bitmapRegistry = bitmapRegistry,
                                                            assetPath = skillAssetPath(skill),
                                                            contentDescription = skill.localizedTitle(language.code),
                                                            sizeDp = 40.dp,
                                                            cornerRadius = 10.dp,
                                                            innerPadding = 4.dp
                                                        )
                                                        Column(
                                                            modifier = Modifier.weight(1f),
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Text(
                                                                text = "${skill.localizedTitle(language.code)} ($lvl/${skill.maxLevel})",
                                                                style = MaterialTheme.typography.titleSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = skill.descFa,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
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
                                                                                when (language) {
                                                                                    AppLanguage.ENGLISH -> "No Skill Points left or maxed!"
                                                                                    AppLanguage.ARABIC -> "لا توجد نقاط مهارة كافية أو وصلت للحد الأقصى!"
                                                                                    AppLanguage.PERSIAN -> "امتیاز مهارت کافی نیست یا به حداکثر رسیده است!"
                                                                                }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            enabled = !maxed && progress.skillPoints > 0,
                                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .heightIn(min = 48.dp)
                                                                .testTag("skill_upgrade_${skill.id}")
                                                        ) {
                                                            Text(
                                                                text = if (maxed) "MAX" else strings.upgradeBtn,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
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

                        if (totalPages > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (clampedPage > 0) skillPage = clampedPage - 1 },
                                    enabled = clampedPage > 0,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("skill_prev_page")
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
                                    onClick = { if (clampedPage < totalPages - 1) skillPage = clampedPage + 1 },
                                    enabled = clampedPage < totalPages - 1,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("skill_next_page")
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
                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
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
