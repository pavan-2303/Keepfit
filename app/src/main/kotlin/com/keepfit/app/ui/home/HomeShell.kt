package com.keepfit.app.ui.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.keepfit.core.model.BodyProfile
import com.keepfit.feature.nutrition.ui.NutritionScreen
import com.keepfit.feature.nutrition.ui.TodayNutritionSection
import com.keepfit.feature.workouts.ui.TodayWorkoutSection
import com.keepfit.feature.workouts.ui.WorkoutsScreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeShell(profile: BodyProfile) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
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
                EmptyDestinationScreen(
                    padding = padding,
                    eyebrow = "PROGRESS",
                    title = "Transformation",
                    description = "Your weekly measurements and private photo comparisons will live here.",
                    icon = Icons.Outlined.MonitorWeight,
                )
            }
            composable(HomeDestination.SETTINGS.route) {
                EmptyDestinationScreen(
                    padding = padding,
                    eyebrow = "PREFERENCES",
                    title = "Settings",
                    description = "Your goals, reminders, backup, and optional integrations will live here.",
                    icon = Icons.Outlined.Tune,
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
        Text(
            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Good to see you, ${profile.displayName}.",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(24.dp))
        TodayWorkoutSection(onOpenWorkout = onOpenWorkout)
        Spacer(modifier = Modifier.height(12.dp))
        TodayNutritionSection()
        Spacer(modifier = Modifier.height(12.dp))
        SummaryCard(
            icon = Icons.Outlined.Insights,
            label = "PROGRESS",
            title = "Starting line",
            description = "Weekly progress summaries will appear here.",
        )
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
