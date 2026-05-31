package com.keepfit.app.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.keepfit.core.model.BodyProfile
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.feature.assistant.ui.AssistantRoute
import com.keepfit.feature.assistant.ui.assistantRoute
import com.keepfit.feature.nutrition.ui.NutritionScreen
import com.keepfit.feature.nutrition.ui.TodayNutritionSection
import com.keepfit.feature.settings.ui.SettingsScreen
import com.keepfit.feature.steps.ui.TodayStepsSection
import com.keepfit.feature.transformation.ui.ProgressScreen
import com.keepfit.feature.transformation.ui.TodayProgressSection
import com.keepfit.feature.workouts.ui.TodayWorkoutSection
import com.keepfit.feature.workouts.ui.WorkoutsScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeShell(
    profile: BodyProfile,
    viewModel: HomeShellViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val assistantLaunchState by viewModel.assistantLaunchState.collectAsStateWithLifecycle()
    val assistantConnectionMessage by viewModel.assistantConnectionMessage.collectAsStateWithLifecycle()
    val showBottomBar = HomeDestination.entries.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    HomeDestination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon(),
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination.TODAY.route,
        ) {
            composable(HomeDestination.TODAY.route) {
                TodayScreen(
                    profile = profile,
                    padding = padding,
                    onOpenWorkout = {
                        navController.navigate(HomeDestination.WORKOUTS.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(HomeDestination.WORKOUTS.route) {
                WorkoutsScreen(modifier = Modifier.padding(padding))
            }
            composable(HomeDestination.NUTRITION.route) {
                NutritionScreen(modifier = Modifier.padding(padding))
            }
            composable(HomeDestination.PROGRESS.route) {
                ProgressScreen(modifier = Modifier.padding(padding))
            }
            composable(HomeDestination.SETTINGS.route) {
                SettingsScreen(
                    modifier = Modifier.padding(padding),
                    onOpenAssistant = { navController.navigate(assistantRoute) },
                    externalMessage = assistantConnectionMessage,
                    onExternalMessageShown = viewModel::dismissAssistantConnectionMessage,
                    onTestAssistantConnection = viewModel::testAssistantConnection,
                )
            }
            composable(assistantRoute) {
                AssistantRoute(
                    isEnabled = assistantLaunchState.isEnabled,
                    config = assistantLaunchState.config,
                    validationMessage = assistantLaunchState.validationMessage,
                    onBack = { navController.popBackStack() },
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
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        TodayHero(profile = profile)
        Spacer(modifier = Modifier.height(18.dp))
        TodayWorkoutSection(onOpenWorkout = onOpenWorkout)
        Spacer(modifier = Modifier.height(12.dp))
        TodayNutritionSection()
        Spacer(modifier = Modifier.height(12.dp))
        TodayProgressSection()
        Spacer(modifier = Modifier.height(12.dp))
        TodayStepsSection()
    }
}

@Composable
private fun TodayHero(profile: BodyProfile) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = com.keepfit.app.R.drawable.keepfit_brand_mark),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keepfit",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Good to see you, ${profile.displayName}.",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Keep the basics visible: training, food, recovery, and progress in one place.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row {
                AssistChip(onClick = {}, label = { Text("Workouts") })
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(onClick = {}, label = { Text("Nutrition") })
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(onClick = {}, label = { Text("Progress") })
            }
        }
    }
}

@Composable
private fun SummaryCard(
    icon: ImageVector,
    label: String,
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyDestinationScreen(
    padding: PaddingValues,
    eyebrow: String,
    title: String,
    description: String,
    icon: ImageVector,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(36.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
                    .size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun HomeDestination.icon(): ImageVector = when (this) {
    HomeDestination.TODAY -> Icons.Outlined.Home
    HomeDestination.WORKOUTS -> Icons.Outlined.FitnessCenter
    HomeDestination.NUTRITION -> Icons.Outlined.RestaurantMenu
    HomeDestination.PROGRESS -> Icons.Outlined.CalendarMonth
    HomeDestination.SETTINGS -> Icons.Outlined.Settings
}
