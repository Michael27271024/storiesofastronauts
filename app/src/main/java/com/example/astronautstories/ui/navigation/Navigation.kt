package com.example.astronautstories.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.astronautstories.domain.model.GameCatalog
import com.example.astronautstories.domain.model.GameMode
import com.example.astronautstories.localization.StringProvider
import com.example.astronautstories.ui.screens.AchievementsAndStatsScreen
import com.example.astronautstories.ui.screens.CommandHubScreen
import com.example.astronautstories.ui.screens.GameArenaScreen
import com.example.astronautstories.ui.screens.SettingsScreen
import com.example.astronautstories.ui.screens.ShopAndSkillsScreen
import com.example.astronautstories.ui.screens.SolarSystemMapScreen
import com.example.astronautstories.ui.screens.SplashScreen
import com.example.astronautstories.ui.screens.TrailerAndAcademyScreen
import com.example.astronautstories.ui.viewmodel.AppContainer
import com.example.astronautstories.ui.viewmodel.GameArenaViewModel
import com.example.astronautstories.ui.viewmodel.MainViewModel
import kotlinx.serialization.Serializable

@Serializable data object RouteSplash
@Serializable data object RouteHub
@Serializable data object RouteSolarMap
@Serializable data class RouteArena(val modeName: String, val stageNumber: Int)
@Serializable data class RouteShopSkills(val initialTab: Int = 0)
@Serializable data class RouteAchievementsStats(val initialTab: Int = 0)
@Serializable data class RouteAcademy(val initialTab: Int = 0)
@Serializable data object RouteSettings

@Composable
fun AstronautNavHost(
    container: AppContainer,
    mainViewModel: MainViewModel,
    arenaViewModel: GameArenaViewModel
) {
    val navController = rememberNavController()

    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val progress by mainViewModel.progress.collectAsStateWithLifecycle()
    val stageRecords by mainViewModel.stageRecords.collectAsStateWithLifecycle()
    val ownedItemIds by mainViewModel.ownedItemIds.collectAsStateWithLifecycle()
    val skillLevels by mainViewModel.skillLevels.collectAsStateWithLifecycle()
    val achievements by mainViewModel.achievements.collectAsStateWithLifecycle()

    val strings = StringProvider.get(settings.language)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val routeStr = navBackStackEntry?.destination?.route ?: ""

    // Switch background music seamlessly based on active screen
    LaunchedEffect(routeStr) {
        when {
            routeStr.contains("RouteHub") -> {
                container.audioEngine.playTheme("menu", loop = true)
            }
            routeStr.contains("RouteSolarMap") -> {
                container.audioEngine.playTheme("level_select", loop = true)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = RouteSplash
    ) {
        composable<RouteSplash> {
            SplashScreen(
                strings = strings,
                reduceMotion = settings.reduceMotion,
                onFinished = {
                    navController.navigate(RouteHub) {
                        popUpTo(RouteSplash) { inclusive = true }
                    }
                }
            )
        }

        composable<RouteHub> {
            CommandHubScreen(
                settings = settings,
                progress = progress,
                ownedItemIds = ownedItemIds,
                strings = strings,
                bitmapRegistry = container.bitmapRegistry,
                onSelectHero = { mainViewModel.selectHero(it) },
                onOpenSolarMap = { navController.navigate(RouteSolarMap) },
                onLaunchMode = { mode, stage ->
                    navController.navigate(RouteArena(mode.name, stage))
                },
                onOpenShop = { tab -> navController.navigate(RouteShopSkills(tab)) },
                onOpenAchievements = { tab -> navController.navigate(RouteAchievementsStats(tab)) },
                onOpenAcademy = { navController.navigate(RouteAcademy(0)) },
                onOpenSettings = { navController.navigate(RouteSettings) }
            )
        }

        composable<RouteSolarMap> {
            SolarSystemMapScreen(
                language = settings.language,
                strings = strings,
                progress = progress,
                stageRecords = stageRecords,
                bitmapRegistry = container.bitmapRegistry,
                onSelectStage = { stage ->
                    navController.navigate(RouteArena(GameMode.STORY.name, stage))
                },
                onResetProgress = { mainViewModel.resetStoryProgress() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RouteArena>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<RouteArena>()
            val mode = GameMode.entries.firstOrNull { it.name == args.modeName } ?: GameMode.STORY
            val safeStage = args.stageNumber.coerceIn(1, GameCatalog.TOTAL_STAGES)
            GameArenaScreen(
                mode = mode,
                stageNumber = safeStage,
                arenaViewModel = arenaViewModel,
                bitmapRegistry = container.bitmapRegistry,
                settings = settings,
                strings = strings,
                onNavigateNextStage = { nextStage ->
                    val clampedNext = nextStage.coerceIn(1, GameCatalog.TOTAL_STAGES)
                    navController.navigate(RouteArena(GameMode.STORY.name, clampedNext)) {
                        popUpTo<RouteArena> { inclusive = true }
                    }
                },
                onExitArena = { navController.popBackStack() }
            )
        }

        composable<RouteShopSkills> { backStackEntry ->
            val args = backStackEntry.toRoute<RouteShopSkills>()
            ShopAndSkillsScreen(
                initialTab = args.initialTab,
                language = settings.language,
                strings = strings,
                progress = progress,
                ownedItemIds = ownedItemIds,
                skillLevels = skillLevels,
                bitmapRegistry = container.bitmapRegistry,
                onPurchaseOrEquip = { item, cb -> mainViewModel.purchaseOrEquip(item, cb) },
                onUpgradeSkill = { skill, cb -> mainViewModel.upgradeSkill(skill, cb) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<RouteAchievementsStats> { backStackEntry ->
            val args = backStackEntry.toRoute<RouteAchievementsStats>()
            AchievementsAndStatsScreen(
                initialTab = args.initialTab,
                language = settings.language,
                strings = strings,
                progress = progress,
                stageRecords = stageRecords,
                achievements = achievements,
                bitmapRegistry = container.bitmapRegistry,
                onBack = { navController.popBackStack() }
            )
        }

        composable<RouteAcademy> { backStackEntry ->
            val args = backStackEntry.toRoute<RouteAcademy>()
            TrailerAndAcademyScreen(
                initialTab = args.initialTab,
                language = settings.language,
                strings = strings,
                bitmapRegistry = container.bitmapRegistry,
                onBack = { navController.popBackStack() }
            )
        }

        composable<RouteSettings> {
            SettingsScreen(
                settings = settings,
                strings = strings,
                bitmapRegistry = container.bitmapRegistry,
                mainViewModel = mainViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
