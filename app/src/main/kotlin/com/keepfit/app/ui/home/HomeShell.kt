package com.keepfit.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.keepfit.core.model.BodyProfile
import com.keepfit.feature.assistant.ui.AssistantRoute
import com.keepfit.feature.nutrition.ui.NutritionScreen
import com.keepfit.feature.nutrition.ui.TodayNutritionSection
import com.keepfit.feature.review.ui.TodayWeeklyReviewCard
import com.keepfit.feature.review.ui.WeeklyReviewRoute
import com.keepfit.feature.review.ui.weeklyReviewRoute
import com.keepfit.feature.settings.ui.SettingsScreen
import com.keepfit.feature.steps.ui.TodayStepsSection
import com.keepfit.feature.transformation.ui.ProgressScreen
import com.keepfit.feature.transformation.ui.TodayProgressSection
import com.keepfit.feature.workouts.ui.StarterPlanRoute
import com.keepfit.feature.workouts.ui.TodayWorkoutSection
import com.keepfit.feature.workouts.ui.WorkoutsScreen
import com.keepfit.feature.workouts.ui.starterPlanRoute
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val settingsRoute = "settings"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeShell(
    profile: BodyProfile,
    startInGuidedSetup: Boolean = false,
    viewModel: HomeShellViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val assistantLaunchState by viewModel.assistantLaunchState.collectAsStateWithLifecycle()
    val assistantConnectionMessage by viewModel.assistantConnectionMessage.collectAsStateWithLifecycle()
    val showNavigationLabels = shouldShowNavigationLabels(LocalDensity.current.fontScale)
    val showBottomBar = HomeDestination.entries.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }
    val showShellTopBar = currentRoute in HomeDestination.entries
        .filterNot { it == HomeDestination.COACH }
        .map(HomeDestination::route) || currentRoute == settingsRoute

    Scaffold(
        topBar = {
            if (showShellTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            if (currentRoute == settingsRoute) "Profile and settings"
                            else HomeDestination.entries.firstOrNull { it.route == currentRoute }?.label ?: "Keepfit",
                        )
                    },
                    navigationIcon = {
                        if (currentRoute == settingsRoute) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (currentRoute != settingsRoute) {
                            IconButton(onClick = { navController.navigate(settingsRoute) }) {
                                Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile and settings")
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    HomeDestination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon(), contentDescription = item.label) },
                            label = if (showNavigationLabels) ({ Text(item.label) }) else null,
                            alwaysShowLabel = showNavigationLabels,
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (startInGuidedSetup) starterPlanRoute else HomeDestination.TODAY.route,
        ) {
            composable(HomeDestination.TODAY.route) {
                TodayScreen(
                    profile = profile,
                    padding = padding,
                    onOpenWorkout = { navController.navigate(HomeDestination.PLAN.route) },
                    onOpenWeeklyReview = { navController.navigate(weeklyReviewRoute) },
                    onOpenNutrition = { navController.navigate(HomeDestination.LOG.route) },
                    onOpenProgress = { navController.navigate(HomeDestination.PROGRESS.route) },
                )
            }
            composable(HomeDestination.PLAN.route) {
                WorkoutsScreen(
                    modifier = Modifier.padding(padding),
                    onOpenStarterPlan = { navController.navigate(starterPlanRoute) },
                )
            }
            composable(HomeDestination.LOG.route) {
                NutritionScreen(modifier = Modifier.padding(padding))
            }
            composable(HomeDestination.PROGRESS.route) {
                ProgressScreen(modifier = Modifier.padding(padding))
            }
            composable(HomeDestination.COACH.route) {
                AssistantRoute(
                    isEnabled = assistantLaunchState.isEnabled,
                    config = assistantLaunchState.config,
                    validationMessage = assistantLaunchState.validationMessage,
                    onOpenSettings = { navController.navigate(settingsRoute) },
                    modifier = Modifier.padding(padding),
                )
            }
            composable(settingsRoute) {
                SettingsScreen(
                    modifier = Modifier.padding(padding),
                    onOpenAssistant = { navController.navigate(HomeDestination.COACH.route) },
                    externalMessage = assistantConnectionMessage,
                    onExternalMessageShown = viewModel::dismissAssistantConnectionMessage,
                    onTestAssistantConnection = viewModel::testAssistantConnection,
                )
            }
            composable(weeklyReviewRoute) {
                WeeklyReviewRoute(
                    onBack = { navController.popBackStack() },
                    modifier = Modifier.padding(padding),
                )
            }
            composable(starterPlanRoute) {
                StarterPlanRoute(
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(HomeDestination.TODAY.route) {
                                popUpTo(starterPlanRoute) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun TodayScreen(
    profile: BodyProfile,
    padding: PaddingValues,
    onOpenWorkout: () -> Unit,
    onOpenWeeklyReview: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenProgress: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
            style = if (largeText) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            if (largeText) "Hello, ${profile.displayName}." else "Good to see you, ${profile.displayName}.",
            style = if (largeText) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(14.dp))
        TodayWorkoutSection(onOpenWorkout = onOpenWorkout)
        Spacer(Modifier.height(10.dp))
        TodayWeeklyReviewCard(onOpenReview = onOpenWeeklyReview)
        Spacer(Modifier.height(10.dp))
        TodayNutritionSection(onOpenNutrition = onOpenNutrition)
        Spacer(Modifier.height(10.dp))
        TodayProgressSection(onOpenProgress = onOpenProgress)
        Spacer(Modifier.height(10.dp))
        TodayStepsSection()
    }
}

internal fun shouldShowNavigationLabels(fontScale: Float): Boolean = fontScale < 1.5f

private fun HomeDestination.icon(): ImageVector = when (this) {
    HomeDestination.TODAY -> Icons.Outlined.Home
    HomeDestination.PLAN -> Icons.Outlined.CalendarMonth
    HomeDestination.LOG -> Icons.Outlined.AddCircleOutline
    HomeDestination.PROGRESS -> Icons.Outlined.Insights
    HomeDestination.COACH -> Icons.Outlined.ChatBubbleOutline
}
