package com.example.astronautstories.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.astronautstories.R
import com.example.astronautstories.assets_loader.NativeBitmapRegistry
import com.example.astronautstories.localization.AppLanguage
import com.example.astronautstories.localization.GameStrings
import com.example.astronautstories.ui.components.AssetSpriteImage
import com.example.astronautstories.ui.components.UniformAssetBadge
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    strings: GameStrings,
    reduceMotion: Boolean,
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(if (reduceMotion) 900L else 1800L)
        onFinished()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010101))
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        val compactSplash = maxWidth < 500.dp || maxHeight < 260.dp
        val logoSize = if (maxHeight < 280.dp) 120.dp else 172.dp

        if (compactSplash) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_logo),
                    contentDescription = strings.appTitle,
                    modifier = Modifier
                        .size(logoSize)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFC47B42).copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = strings.appTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5E6C8),
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .fillMaxWidth(0.75f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFC47B42),
                    trackColor = Color(0xFF1E1D26)
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_logo),
                    contentDescription = strings.appTitle,
                    modifier = Modifier
                        .size(logoSize)
                        .clip(RoundedCornerShape(28.dp))
                        .border(2.dp, Color(0xFFC47B42).copy(alpha = 0.7f), RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Fit
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5E6C8)
                    )
                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(220.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFC47B42),
                        trackColor = Color(0xFF1E1D26)
                    )
                }
            }
        }
    }
}

data class TutorialModule(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val bodyFa: String,
    val bodyEn: String,
    val demoActionLabelFa: String,
    val demoActionLabelEn: String,
    val assetPath: String? = null,
    @DrawableRes val drawableRes: Int? = null,
    val darkBackdrop: Boolean = false,
    val cropToBounds: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailerAndAcademyScreen(
    initialTab: Int,
    language: AppLanguage,
    strings: GameStrings,
    bitmapRegistry: NativeBitmapRegistry,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var tutorialPage by remember { mutableIntStateOf(0) }

    val tutorials = remember {
        listOf(
            TutorialModule(
                id = "move",
                titleFa = "۱. حرکت، پرش و جاذبه سیاره‌ها",
                titleEn = "1. Movement & Planetary Gravity",
                bodyFa = "هر سیاره جاذبه منحصربه‌فردی دارد؛ در ماه پرش بلندتر و در مشتری جاذبه سنگین‌تر است.",
                bodyEn = "Each planet has unique gravity; Moon offers higher leaps while Jupiter pulls harder.",
                demoActionLabelFa = "آزمایش رانشگر جت‌پک",
                demoActionLabelEn = "Test Jetpack Thruster",
                assetPath = "images/astronaut.png"
            ),
            TutorialModule(
                id = "combat",
                titleFa = "۲. سلاح‌ها و موج شوک ویژه",
                titleEn = "2. Weapons & Special Shockwave",
                bodyFa = "با ۵ سلاح پیشرفته مبارزه کنید. دکمه ویژه هر ۸ ثانیه شارژ شده و موج نابودگر می‌فرستد.",
                bodyEn = "Equip 5 upgradable weapons. Your Special Shockwave recharges every 8 seconds.",
                demoActionLabelFa = "شارژ نمونه شوک پلاسما",
                demoActionLabelEn = "Simulate Plasma Shockwave",
                assetPath = "images/gun.png"
            ),
            TutorialModule(
                id = "darkness",
                titleFa = "۳. نورپردازی پویا در عطارد",
                titleEn = "3. Mercury Dynamic Darkness",
                bodyFa = "در مراحل عمیق عطارد و قلعه بیگانگان، تنها چراغ‌قوه فضانورد مسیر پیش رو را روشن می‌کند.",
                bodyEn = "In deep Mercury sectors, only your helmet beam lights the path ahead.",
                demoActionLabelFa = "تست چراغ‌قوه کلاه فضایی",
                demoActionLabelEn = "Toggle Helmet Beam",
                assetPath = "images/alien_base.png"
            ),
            TutorialModule(
                id = "gamepad",
                titleFa = "۴. کنترلر و ویدئوهای داستانی خودکار",
                titleEn = "4. Gamepad & Auto Story Cutscenes",
                bodyFa = "کنترلر بلوتوث و USB پشتیبانی می‌شود و ویدئوهای داستانی پس از برد خودکار پخش می‌شوند.",
                bodyEn = "External controllers work natively, and story cutscenes play automatically after stage wins.",
                demoActionLabelFa = "بررسی چیدمان کنترل",
                demoActionLabelEn = "Inspect Control Map",
                drawableRes = R.drawable.img_academy_emblem,
                darkBackdrop = true,
                cropToBounds = true
            )
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.academyTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("academy_back_button")
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
                            text = strings.trailerTab,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.Movie, contentDescription = null) },
                    modifier = Modifier.testTag("tab_trailer")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = strings.tutorialsTab,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Default.School, contentDescription = null) },
                    modifier = Modifier.testTag("tab_tutorials")
                )
            }

            if (selectedTab == 0) {
                CinematicTrailerView(language = language, bitmapRegistry = bitmapRegistry)
            } else {
                var highlightedDemo by remember { mutableStateOf<String?>(null) }
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val colCount = if (maxWidth >= 560.dp) 2 else 1
                    val rowCount = if (maxHeight >= 305.dp) 2 else 1
                    val pageSize = colCount * rowCount
                    val totalPages = ((tutorials.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                    val clampedPage = tutorialPage.coerceIn(0, totalPages - 1)
                    val pageTutorials = tutorials.drop(clampedPage * pageSize).take(pageSize)

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
                            val rows = pageTutorials.chunked(colCount)
                            for (r in 0 until rowCount) {
                                val pair = rows.getOrNull(r) ?: emptyList()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { mod ->
                                        val isActive = highlightedDemo == mod.id
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isActive) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                }
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isActive) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                UniformAssetBadge(
                                                    bitmapRegistry = bitmapRegistry,
                                                    assetPath = mod.assetPath,
                                                    drawableRes = mod.drawableRes,
                                                    sizeDp = 52.dp,
                                                    cornerRadius = 12.dp,
                                                    innerPadding = 5.dp,
                                                    darkBackdrop = mod.darkBackdrop,
                                                    cropToBounds = mod.cropToBounds
                                                )
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight(),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                        Text(
                                                            text = if (language == AppLanguage.ENGLISH) mod.titleEn else mod.titleFa,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = if (language == AppLanguage.ENGLISH) mod.bodyEn else mod.bodyFa,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            maxLines = 3,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    FilledTonalButton(
                                                        onClick = { highlightedDemo = if (isActive) null else mod.id },
                                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                                        modifier = Modifier
                                                            .heightIn(min = 48.dp)
                                                            .testTag("tutorial_demo_${mod.id}")
                                                    ) {
                                                        Text(
                                                            text = if (language == AppLanguage.ENGLISH) mod.demoActionLabelEn else mod.demoActionLabelFa,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    repeat(colCount - pair.size) { Spacer(Modifier.weight(1f)) }
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
                                    onClick = { if (clampedPage > 0) tutorialPage = clampedPage - 1 },
                                    enabled = clampedPage > 0,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("tutorial_prev_page")
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
                                    onClick = { if (clampedPage < totalPages - 1) tutorialPage = clampedPage + 1 },
                                    enabled = clampedPage < totalPages - 1,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("tutorial_next_page")
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
private fun CinematicTrailerView(
    language: AppLanguage,
    bitmapRegistry: NativeBitmapRegistry
) {
    var playing by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(playing) {
        while (playing) {
            delay(32L)
            progress = (progress + 0.0045f) % 1f
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "trailer_warp")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val sceneIndex = ((progress * 4f).toInt()).coerceIn(0, 3)
    val slidePaths = listOf(
        "images/astronauts_happy.png",
        "images/backgrounds/mars_background.png",
        "images/backgrounds/alien_base_under_attack.jpg",
        "images/iran_flag_on_saturn.png"
    )
    val captions = when (language) {
        AppLanguage.ENGLISH -> listOf(
            "JUNE 21, 2025 — FOUR HEROES LAUNCHED BEYOND EARTH",
            "AMBUSHED ON THE MOON — COLLECT OXYGEN & REBUILD THE SHIP",
            "10 PLANETS, 50 STAGES, 8 HEROES & DEADLY STELLAR BOSSES",
            "STORY CAMPAIGN, ENDLESS SURVIVAL & AI SQUAD DUEL!"
        )
        AppLanguage.ARABIC -> listOf(
            "٢١ يونيو ٢٠٢٥ — انطلق أربعة رواد فضاء إلى أعماق الفضاء",
            "كمين على القمر — اجمع الأكسجين وأصلح السفينة",
            "١٠ كواكب، ٥٠ مرحلة، ٨ أبطال وزعماء كونيون أقوياء",
            "طور القصة، البقاء اللانهائي ومبارزة الذكاء الاصطناعي!"
        )
        AppLanguage.PERSIAN -> listOf(
            "۲۱ ژوئن ۲۰۲۵ — پرتاب چهار فضانورد دلیر به اعماق منظومه شمسی",
            "حمله ناگهانی ستاره‌زادگان — جمع‌آوری اکسیژن و بازیابی قطعات سفینه",
            "۱۰ سیاره، ۵۰ مرحله متمایز، ۸ قهرمان و باس‌های چندفاز کیهانی",
            "حالت داستانی، موج بی‌پایان، چالش زمان‌دار و دوئل هوشمند!"
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val stacked = maxWidth < 520.dp

        val previewStage: @Composable (Modifier) -> Unit = { boxMod ->
            Box(
                modifier = boxMod
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, Color(0xFFC47B42).copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .background(Color(0xFF09090D))
            ) {
                AssetSpriteImage(
                    bitmapRegistry = bitmapRegistry,
                    assetPath = slidePaths[sceneIndex],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    maxDimension = 768,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.65f)
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 0 until 24) {
                        val t = ((progress * 3f + i * 0.13f) % 1f)
                        val x = (1f - t) * size.width
                        val y = ((i * 73) % size.height.toInt().coerceAtLeast(1)).toFloat()
                        drawLine(
                            color = (if (i % 2 == 0) Color(0xFFC47B42) else Color(0xFFF5E6C8)).copy(alpha = 0.65f),
                            start = Offset(x, y),
                            end = Offset(x + 36f, y),
                            strokeWidth = 2.5f
                        )
                    }
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = 68f + sin(progress * 12f) * 14f
                    drawCircle(
                        color = Color(0xFFE09658).copy(alpha = pulse * 0.35f),
                        radius = r,
                        center = Offset(cx + cos(progress * 6f) * 40f, cy)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xE609090D))
                            )
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = captions[sceneIndex],
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5E6C8),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        val playButton: @Composable (Modifier) -> Unit = { mod ->
            Button(
                onClick = { playing = !playing },
                modifier = mod
                    .heightIn(min = 48.dp)
                    .testTag("trailer_play_pause_button")
            ) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (playing) {
                        when (language) {
                            AppLanguage.ENGLISH -> "Pause Preview"
                            AppLanguage.ARABIC -> "إيقاف المعاينة"
                            AppLanguage.PERSIAN -> "توقف پیش‌نمایش"
                        }
                    } else {
                        when (language) {
                            AppLanguage.ENGLISH -> "Play Preview"
                            AppLanguage.ARABIC -> "تشغيل المعاينة"
                            AppLanguage.PERSIAN -> "پخش پیش‌نمایش"
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val replayButton: @Composable (Modifier) -> Unit = { mod ->
            OutlinedButton(
                onClick = {
                    progress = 0f
                    playing = true
                },
                modifier = mod
                    .heightIn(min = 48.dp)
                    .testTag("trailer_restart_button")
            ) {
                Icon(Icons.Default.Replay, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (language) {
                        AppLanguage.ENGLISH -> "Replay"
                        AppLanguage.ARABIC -> "إعادة التشغيل"
                        AppLanguage.PERSIAN -> "پخش از ابتدا"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                previewStage(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    playButton(Modifier.weight(1f))
                    replayButton(Modifier.weight(1f))
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                previewStage(
                    Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                )
                Column(
                    modifier = Modifier.weight(0.65f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    playButton(Modifier.fillMaxWidth())
                    replayButton(Modifier.fillMaxWidth())
                }
            }
        }
    }
}
