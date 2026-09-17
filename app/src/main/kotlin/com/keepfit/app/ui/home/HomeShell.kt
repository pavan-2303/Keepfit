package com.keepfit.app.ui.home

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import com.keepfit.core.designsystem.KeepfitMotionProvider
import com.keepfit.core.designsystem.KeepfitSectionHeader
import com.keepfit.app.profile.ProfileInputValidator
import com.keepfit.app.profile.ProfileValidationResult
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
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val settingsRoute = "settings"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeShell(
    profile: BodyProfile,
    profiles: List<BodyProfile> = listOf(profile),
    startInGuidedSetup: Boolean = false,
    profileValidationMessage: String? = null,
    onAddProfile: (String, String, LocalDate?) -> Unit = { _, _, _ -> },
    onEditProfile: (String, String, String, LocalDate?) -> Unit = { _, _, _, _ -> },
    onSelectProfile: (String) -> Unit = {},
    onArchiveProfile: (String) -> Unit = {},
    onDismissProfileMessage: () -> Unit = {},
    viewModel: HomeShellViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val assistantLaunchState by viewModel.assistantLaunchState.collectAsStateWithLifecycle()
    val assistantConnectionMessage by viewModel.assistantConnectionMessage.collectAsStateWithLifecycle()
    val credentialRecoveryEnabled by viewModel.credentialRecoveryEnabled.collectAsStateWithLifecycle()
    val credentialRecoveryAvailable by viewModel.credentialRecoveryAvailable.collectAsStateWithLifecycle()
    val reduceMotion by viewModel.reduceMotion.collectAsStateWithLifecycle()
    val showNavigationLabels = shouldShowNavigationLabels(LocalDensity.current.fontScale)
    val showBottomBar = HomeDestination.entries.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }
    val showShellTopBar = currentRoute in HomeDestination.entries
        .filterNot { it == HomeDestination.COACH }
        .map(HomeDestination::route) || currentRoute == settingsRoute
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var editorMode by remember { mutableStateOf<ProfileEditorMode?>(null) }
    var confirmArchive by remember { mutableStateOf(false) }
    var openAssistantPlanner by remember { mutableStateOf(false) }

    KeepfitMotionProvider(reduceMotion = reduceMotion) {
    Scaffold(
        topBar = {
            if (showShellTopBar) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .width(4.dp)
                                    .height(26.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (currentRoute == settingsRoute) "Profile and settings"
                                else HomeDestination.entries.firstOrNull { it.route == currentRoute }?.label ?: "Keepfit",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
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
                            Box {
                                Surface(
                                    onClick = { profileMenuExpanded = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .semantics {
                                            contentDescription = "Switch profile. ${profile.displayName} is active."
                                        },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(profile.displayName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                                    }
                                }
                                DropdownMenu(
                                    expanded = profileMenuExpanded,
                                    onDismissRequest = { profileMenuExpanded = false },
                                ) {
                                    ProfileMenuItems(
                                        profile = profile,
                                        profiles = profiles,
                                        onSelect = {
                                            profileMenuExpanded = false
                                            onSelectProfile(it)
                                        },
                                        onAdd = {
                                            profileMenuExpanded = false
                                            editorMode = ProfileEditorMode.Add
                                        },
                                        onEdit = {
                                            profileMenuExpanded = false
                                            editorMode = ProfileEditorMode.Edit
                                        },
                                        onArchive = {
                                            profileMenuExpanded = false
                                            confirmArchive = true
                                        },
                                        onSettings = {
                                            profileMenuExpanded = false
                                            navController.navigate(settingsRoute)
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
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
            enterTransition = {
                if (reduceMotion) EnterTransition.None else fadeIn(tween(140))
            },
            exitTransition = {
                if (reduceMotion) ExitTransition.None else fadeOut(tween(90))
            },
            popEnterTransition = {
                if (reduceMotion) EnterTransition.None else fadeIn(tween(120))
            },
            popExitTransition = {
                if (reduceMotion) ExitTransition.None else fadeOut(tween(80))
            },
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
                    openPlanner = openAssistantPlanner,
                    onPlannerOpened = { openAssistantPlanner = false },
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
                    credentialRecoveryEnabled = credentialRecoveryEnabled,
                    credentialRecoveryAvailable = credentialRecoveryAvailable,
                    onCredentialRecoveryChanged = viewModel::setCredentialRecoveryEnabled,
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
                    onPlanManually = {
                        navController.navigate(HomeDestination.PLAN.route) {
                            popUpTo(starterPlanRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onAskCoach = {
                        openAssistantPlanner = true
                        navController.navigate(HomeDestination.COACH.route) {
                            popUpTo(starterPlanRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }

    editorMode?.let { mode ->
        ProfileEditorDialog(
            title = if (mode == ProfileEditorMode.Add) "Add profile" else "Edit profile",
            initialName = if (mode == ProfileEditorMode.Edit) profile.displayName else "",
            initialHeight = if (mode == ProfileEditorMode.Edit) profile.heightCm?.toString().orEmpty() else "",
            initialBirthDate = if (mode == ProfileEditorMode.Edit) profile.birthDate else null,
            validationMessage = profileValidationMessage,
            onDismiss = {
                editorMode = null
                onDismissProfileMessage()
            },
            onSave = { name, height, birthDate ->
                if (mode == ProfileEditorMode.Add) onAddProfile(name, height, birthDate)
                else onEditProfile(profile.id, name, height, birthDate)
                if (
                    ProfileInputValidator.validate(
                        displayName = name,
                        heightCm = height,
                        birthDate = birthDate,
                    ) is ProfileValidationResult.Valid
                ) {
                    editorMode = null
                }
            },
        )
    }
    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Archive ${profile.displayName}?") },
            text = { Text("Their history stays on this device and is excluded from the profile switcher.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmArchive = false
                    onArchiveProfile(profile.id)
                }) { Text("Archive") }
            },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancel") } },
        )
    }
    }
}

private enum class ProfileEditorMode { Add, Edit }

@Composable
internal fun ProfileMenuItems(
    profile: BodyProfile,
    profiles: List<BodyProfile>,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onSettings: () -> Unit,
) {
    profiles.forEach { candidate ->
        DropdownMenuItem(
            text = {
                val label = if (candidate.id == profile.id) {
                    "${candidate.displayName} (active)"
                } else {
                    candidate.displayName
                }
                Text(label)
            },
            onClick = { onSelect(candidate.id) },
        )
    }
    DropdownMenuItem(text = { Text("Add profile") }, onClick = onAdd)
    DropdownMenuItem(text = { Text("Edit ${profile.displayName}") }, onClick = onEdit)
    if (profiles.size > 1) {
        DropdownMenuItem(text = { Text("Archive ${profile.displayName}") }, onClick = onArchive)
    }
    DropdownMenuItem(
        text = { Text("Profile and settings") },
        onClick = onSettings,
        leadingIcon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditorDialog(
    title: String,
    initialName: String,
    initialHeight: String,
    initialBirthDate: LocalDate?,
    validationMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate?) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var height by remember(initialHeight) { mutableStateOf(initialHeight) }
    var birthDate by remember(initialBirthDate) { mutableStateOf(initialBirthDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") })
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showDatePicker = true }) {
                    Text(birthDate?.let { "Birth date: $it" } ?: "Add birth date (optional)")
                }
                if (birthDate != null) {
                    TextButton(onClick = { birthDate = null }) { Text("Remove birth date") }
                }
                validationMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, height, birthDate) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            birthDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) { Text("Use date") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        KeepfitSectionHeader(
            title = if (largeText) "Hello, ${profile.displayName}." else "Good to see you, ${profile.displayName}.",
            supportingText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
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
