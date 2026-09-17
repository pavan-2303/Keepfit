package com.keepfit.feature.nutrition.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepfit.core.database.nutrition.MealType
import com.keepfit.core.database.nutrition.MealQuality
import com.keepfit.core.preferences.NutritionTrackingDepth
import com.keepfit.feature.nutrition.NutritionViewModel
import com.keepfit.feature.nutrition.NutritionMetric
import com.keepfit.feature.nutrition.NutritionSummaryRules
import com.keepfit.feature.nutrition.NutritionTargetRange
import com.keepfit.feature.nutrition.data.DailyNutritionSummary
import com.keepfit.feature.nutrition.data.DiaryEntry
import com.keepfit.feature.nutrition.data.Food
import com.keepfit.feature.nutrition.data.MealQualityCheckIn
import com.keepfit.feature.nutrition.data.SavedMeal
import com.keepfit.feature.nutrition.data.formatQuantity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun NutritionScreen(
    modifier: Modifier = Modifier,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val favoriteFoods by viewModel.favoriteFoods.collectAsStateWithLifecycle()
    val recentFoods by viewModel.recentFoods.collectAsStateWithLifecycle()
    val savedMeals by viewModel.savedMeals.collectAsStateWithLifecycle()
    val diaryEntries by viewModel.diaryEntries.collectAsStateWithLifecycle()
    val dailySummary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val qualityCheckIns by viewModel.mealQualityCheckIns.collectAsStateWithLifecycle()
    val previousMealTypes by viewModel.previousMealTypes.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFoodEditor by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<Food?>(null) }
    var showSavedMealEditor by remember { mutableStateOf(false) }
    var addMealType by remember { mutableStateOf<MealType?>(null) }
    var destination by rememberSaveable { mutableStateOf(NutritionDestination.DIARY) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (destination != NutritionDestination.DIARY) {
                NutritionSubpageHeader(
                    title = if (destination == NutritionDestination.FOODS) "Foods" else "Saved meals",
                    supportingText = if (destination == NutritionDestination.FOODS) {
                        "Your personal food library."
                    } else {
                        "Reusable combinations for faster logging."
                    },
                    onBack = { destination = NutritionDestination.DIARY },
                )
                when (destination) {
                    NutritionDestination.FOODS -> FoodLibrarySection(
                        foods = foods,
                        onSearch = viewModel::searchFoods,
                        onCreate = {
                            editingFood = null
                            showFoodEditor = true
                        },
                        onEdit = {
                            editingFood = it
                            showFoodEditor = true
                        },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onArchive = { viewModel.archiveFood(it.id) },
                    )
                    NutritionDestination.SAVED_MEALS -> SavedMealsSection(
                        foods = foods,
                        meals = savedMeals,
                        onCreate = { showSavedMealEditor = true },
                    )
                    NutritionDestination.DIARY -> Unit
                }
            } else {
            NutritionLensSelector(
                depth = appSettings.nutritionTrackingDepth,
                rangePercent = appSettings.nutritionTargetRangePercent,
                onDepthChange = viewModel::updateTrackingDepth,
                onRangeChange = viewModel::updateTargetRangePercent,
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (appSettings.nutritionTrackingDepth) {
                NutritionTrackingDepth.DISABLED -> NutritionDisabledCard(
                    onChooseDepth = { viewModel.updateTrackingDepth(NutritionTrackingDepth.CALORIES_PROTEIN) },
                )

                NutritionTrackingDepth.MEAL_QUALITY -> {
                    DateHeader(
                        date = dailySummary.date,
                        onMoveBack = { viewModel.moveDate(-1) },
                        onMoveForward = { viewModel.moveDate(1) },
                        onDuplicateYesterday = null,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MealQualityCheckInPanel(
                        checkIns = qualityCheckIns,
                        onSetQuality = viewModel::setMealQuality,
                    )
                }

                NutritionTrackingDepth.DETAILED_MACROS,
                NutritionTrackingDepth.CALORIES_PROTEIN,
                -> {
                    val depth = appSettings.nutritionTrackingDepth
                    DateHeader(
                        date = dailySummary.date,
                        onMoveBack = { viewModel.moveDate(-1) },
                        onMoveForward = { viewModel.moveDate(1) },
                        onDuplicateYesterday = viewModel::duplicateYesterday,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NutritionSummaryCard(
                        summary = dailySummary,
                        depth = depth,
                        rangePercent = appSettings.nutritionTargetRangePercent,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("DIARY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    MealType.entries.forEach { mealType ->
                        MealSection(
                            mealType = mealType,
                            entries = diaryEntries.filter { it.mealType == mealType },
                            depth = depth,
                            canRepeat = mealType in previousMealTypes,
                            onRepeat = { viewModel.repeatYesterdayMeal(mealType) },
                            onAdd = { addMealType = mealType },
                            onDeleteEntry = viewModel::deleteDiaryEntry,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text("Log tools", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            NutritionTools(
                onOpenFoods = { destination = NutritionDestination.FOODS },
                onOpenSavedMeals = { destination = NutritionDestination.SAVED_MEALS },
            )
            }
        }
    }

    if (showFoodEditor) {
        FoodEditorDialog(
            food = editingFood,
            onDismiss = {
                showFoodEditor = false
                editingFood = null
            },
            onSave = { id, name, label, amount, calories, protein, carbs, fat ->
                viewModel.saveFood(id, name, label, amount, calories, protein, carbs, fat)
                showFoodEditor = false
                editingFood = null
            },
        )
    }

    if (showSavedMealEditor) {
        SavedMealEditorDialog(
            foods = foods,
            onDismiss = { showSavedMealEditor = false },
            onSave = { name, items ->
                viewModel.createSavedMeal(name, items)
                showSavedMealEditor = false
            },
        )
    }

    addMealType?.let { mealType ->
        AddDiaryEntryDialog(
            mealType = mealType,
            foods = foods,
            favoriteFoods = favoriteFoods,
            recentFoods = recentFoods,
            savedMeals = savedMeals,
            onDismiss = { addMealType = null },
            onAddFood = { foodId, servings ->
                viewModel.addFoodToDiary(mealType, foodId, servings)
                addMealType = null
            },
            onAddSavedMeal = { savedMealId, multiplier ->
                viewModel.addSavedMealToDiary(mealType, savedMealId, multiplier)
                addMealType = null
            },
        )
    }
}

@Composable
private fun NutritionSubpageHeader(
    title: String,
    supportingText: String,
    onBack: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to food diary")
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NutritionToolRow(title: String, detail: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.RestaurantMenu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = "Open $title")
    }
    HorizontalDivider()
}

@Composable
internal fun NutritionTools(onOpenFoods: () -> Unit, onOpenSavedMeals: () -> Unit) {
    NutritionToolRow(
        title = "Foods",
        detail = "Create, edit, and favorite foods",
        onClick = onOpenFoods,
    )
    NutritionToolRow(
        title = "Saved meals",
        detail = "Build reusable meals from your foods",
        onClick = onOpenSavedMeals,
    )
}

@Composable
fun TodayNutritionSection(
    modifier: Modifier = Modifier,
    onOpenNutrition: () -> Unit = {},
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val summary by viewModel.todaySummary.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val checkIns by viewModel.todayMealQualityCheckIns.collectAsStateWithLifecycle()
    if (settings.nutritionTrackingDepth == NutritionTrackingDepth.DISABLED) return
    TodayNutritionCard(
        modifier = modifier,
        depth = settings.nutritionTrackingDepth,
        rangePercent = settings.nutritionTargetRangePercent,
        summary = summary,
        checkIns = checkIns,
        onOpenNutrition = onOpenNutrition,
    )
}

@Composable
internal fun TodayNutritionCard(
    depth: NutritionTrackingDepth,
    rangePercent: Int,
    summary: DailyNutritionSummary,
    checkIns: List<MealQualityCheckIn>,
    onOpenNutrition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.RestaurantMenu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Nutrition", style = MaterialTheme.typography.titleMedium)
            if (depth == NutritionTrackingDepth.MEAL_QUALITY) {
                Text("${checkIns.size} of 4 meals checked in", style = MaterialTheme.typography.bodySmall)
            } else if (!summary.hasEntries) {
                Text(
                    "No meals logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "${summary.totals.calories.toInt()} kcal · ${summary.totals.proteinGrams.toInt()} g protein",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            }
            TextButton(onClick = onOpenNutrition) {
                Text(if (depth == NutritionTrackingDepth.MEAL_QUALITY) "Check in" else "Log")
            }
        }
    }
}

@Composable
internal fun NutritionLensSelector(
    depth: NutritionTrackingDepth,
    rangePercent: Int,
    onDepthChange: (NutritionTrackingDepth) -> Unit,
    onRangeChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("YOUR NUTRITION LENS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Keep only the detail that helps you today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NutritionTrackingDepth.entries.forEach { option ->
                    FilterChip(
                        selected = depth == option,
                        onClick = { onDepthChange(option) },
                        label = { Text(option.label) },
                    )
                }
            }
            if (depth == NutritionTrackingDepth.DETAILED_MACROS || depth == NutritionTrackingDepth.CALORIES_PROTEIN) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Target flexibility", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15).forEach { percent ->
                        FilterChip(
                            selected = rangePercent == percent,
                            onClick = { onRangeChange(percent) },
                            label = { Text("$percent%") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NutritionDisabledCard(onChooseDepth: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Nutrition is off", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Your foods, saved meals, goals, check-ins, and diary history are still here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(onClick = onChooseDepth) { Text("Use calories + protein") }
        }
    }
}

@Composable
internal fun MealQualityCheckInPanel(
    checkIns: List<MealQualityCheckIn>,
    onSetQuality: (MealType, MealQuality) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Meal check-in", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Notice the meal without turning it into a score.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(14.dp))
            MealType.entries.forEachIndexed { index, mealType ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                val selected = checkIns.firstOrNull { it.mealType == mealType }?.quality
                Text(mealType.label, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MealQuality.entries.forEach { quality ->
                        FilterChip(
                            selected = selected == quality,
                            onClick = { onSetQuality(mealType, quality) },
                            label = { Text(quality.label) },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Balanced = protein + produce. One focus = either one. Flexible = something else or unsure.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    onMoveBack: () -> Unit,
    onMoveForward: () -> Unit,
    onDuplicateYesterday: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMoveBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Previous day")
                }
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onMoveForward) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Next day")
                }
            }
            onDuplicateYesterday?.let {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = it) {
                    Text("Replace day with yesterday")
                }
            }
        }
    }
}

@Composable
private fun NutritionSummaryCard(
    summary: DailyNutritionSummary,
    depth: NutritionTrackingDepth,
    rangePercent: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (summary.hasEntries) "Daily totals" else "No entries yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (summary.goals == null) {
                    "Set nutrition goals later to see your target range."
                } else {
                    "Your goals are shown as a $rangePercent% flexible range."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            SummaryMetrics(summary, depth, rangePercent)
        }
    }
}

@Composable
private fun SummaryMetrics(
    summary: DailyNutritionSummary,
    depth: NutritionTrackingDepth,
    rangePercent: Int,
) {
    val rules = NutritionSummaryRules()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rules.visibleMetrics(depth).forEach { metric ->
            val (label, value, goal, unit) = when (metric) {
                NutritionMetric.CALORIES -> MetricValues("Calories", summary.totals.calories, summary.goals?.calorieGoal, "kcal")
                NutritionMetric.PROTEIN -> MetricValues("Protein", summary.totals.proteinGrams, summary.goals?.proteinGoalGrams, "g")
                NutritionMetric.CARBOHYDRATES -> MetricValues("Carbs", summary.totals.carbohydrateGrams, summary.goals?.carbohydrateGoalGrams, "g")
                NutritionMetric.FAT -> MetricValues("Fat", summary.totals.fatGrams, summary.goals?.fatGoalGrams, "g")
            }
            MetricCard(label, value.formatQuantity(), rules.targetRange(goal, rangePercent), unit)
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    target: NutritionTargetRange?,
    unit: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .width(150.dp)
                .padding(12.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            target?.let {
                Text(
                    "Target ${it.minimum.formatQuantity()}-${it.maximum.formatQuantity()} $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MealSection(
    mealType: MealType,
    entries: List<DiaryEntry>,
    depth: NutritionTrackingDepth,
    canRepeat: Boolean,
    onRepeat: () -> Unit,
    onAdd: () -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mealType.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (canRepeat) {
                        OutlinedButton(onClick = onRepeat, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
                            Text("Repeat yesterday")
                        }
                    }
                    FilledTonalButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (entries.isEmpty()) {
                Text(
                    "Nothing logged.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.foodName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${entry.servings.formatQuantity()} x ${entry.servingLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            if (depth == NutritionTrackingDepth.CALORIES_PROTEIN) {
                                "${entry.calories.toInt()} kcal  ${entry.proteinGrams.formatQuantity()} g protein"
                            } else {
                                "${entry.calories.toInt()} kcal  ${entry.proteinGrams.formatQuantity()}P  ${entry.carbohydrateGrams.formatQuantity()}C  ${entry.fatGrams.formatQuantity()}F"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDeleteEntry(entry.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete ${entry.foodName}")
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodLibrarySection(
    foods: List<Food>,
    onSearch: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (Food) -> Unit,
    onToggleFavorite: (Food) -> Unit,
    onArchive: (Food) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                modifier = Modifier.weight(1f),
                label = { Text("Search foods") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(10.dp))
            FilledTonalButton(onClick = onCreate) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Food")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (foods.isEmpty()) {
            Text(
                "No foods yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        foods.forEach { food ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(food.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${food.servingLabel}  ${food.calories.toInt()} kcal",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onToggleFavorite(food) }) {
                        Icon(
                            imageVector = if (food.isFavorite) {
                                Icons.Outlined.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = "Favorite ${food.name}",
                        )
                    }
                    IconButton(onClick = { onEdit(food) }) {
                        Icon(Icons.Outlined.RestaurantMenu, contentDescription = "Edit ${food.name}")
                    }
                    IconButton(onClick = { onArchive(food) }) {
                        Icon(Icons.Outlined.Archive, contentDescription = "Archive ${food.name}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedMealsSection(
    foods: List<Food>,
    meals: List<SavedMeal>,
    onCreate: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Reusable meals",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = onCreate, enabled = foods.isNotEmpty()) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Meal")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (meals.isEmpty()) {
            Text(
                if (foods.isEmpty()) {
                    "Add foods first to create saved meals."
                } else {
                    "No saved meals yet."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        meals.forEach { meal ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(meal.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        meal.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodEditorDialog(
    food: Food?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, String, String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(food?.name.orEmpty()) }
    var servingLabel by remember { mutableStateOf(food?.servingLabel.orEmpty()) }
    var servingAmount by remember { mutableStateOf(food?.servingAmount?.formatQuantity().orEmpty()) }
    var calories by remember { mutableStateOf(food?.calories?.formatQuantity().orEmpty()) }
    var protein by remember { mutableStateOf(food?.proteinGrams?.formatQuantity().orEmpty()) }
    var carbohydrates by remember { mutableStateOf(food?.carbohydrateGrams?.formatQuantity().orEmpty()) }
    var fat by remember { mutableStateOf(food?.fatGrams?.formatQuantity().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (food == null) "Add food" else "Edit food") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food name") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = servingLabel, onValueChange = { servingLabel = it }, label = { Text("Serving label") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = servingAmount, onValueChange = { servingAmount = it }, label = { Text("Serving amount") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Calories") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = carbohydrates, onValueChange = { carbohydrates = it }, label = { Text("Carbs (g)") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(food?.id, name, servingLabel, servingAmount, calories, protein, carbohydrates, fat)
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SavedMealEditorDialog(
    foods: List<Food>,
    onDismiss: () -> Unit,
    onSave: (String, List<Pair<String, String>>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val selectedFoodIds = remember { mutableStateListOf<String>() }
    val servingsByFoodId = remember { mutableStateMapOf<String, String>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New saved meal") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal name") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
                foods.forEach { food ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = food.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (food.id in selectedFoodIds) {
                                            selectedFoodIds.remove(food.id)
                                        } else {
                                            selectedFoodIds.add(food.id)
                                            servingsByFoodId.putIfAbsent(food.id, "1")
                                        }
                                    },
                                ) {
                                    Text(if (food.id in selectedFoodIds) "Remove" else "Add")
                                }
                            }
                            if (food.id in selectedFoodIds) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = servingsByFoodId[food.id].orEmpty(),
                                    onValueChange = { servingsByFoodId[food.id] = it },
                                    label = { Text("Servings") },
                                    singleLine = true,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        selectedFoodIds.map { it to servingsByFoodId.getValue(it) },
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AddDiaryEntryDialog(
    mealType: MealType,
    foods: List<Food>,
    favoriteFoods: List<Food>,
    recentFoods: List<Food>,
    savedMeals: List<SavedMeal>,
    onDismiss: () -> Unit,
    onAddFood: (String, String) -> Unit,
    onAddSavedMeal: (String, String) -> Unit,
) {
    var mode by remember { mutableStateOf(EntryMode.FOOD) }
    var query by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("1") }
    val filteredFoods = foods.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val filteredMeals = savedMeals.filter { it.name.contains(query.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${mealType.label}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { mode = EntryMode.FOOD },
                        enabled = mode != EntryMode.FOOD,
                    ) { Text("Food") }
                    FilledTonalButton(
                        onClick = { mode = EntryMode.MEAL },
                        enabled = mode != EntryMode.MEAL,
                    ) { Text("Saved meal") }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(if (mode == EntryMode.FOOD) "Search foods" else "Search meals") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = servings,
                    onValueChange = { servings = it },
                    label = { Text(if (mode == EntryMode.FOOD) "Servings" else "Meal multiplier") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ServingPresetRow(value = servings, onValueChange = { servings = it })
                Spacer(modifier = Modifier.height(12.dp))
                if (mode == EntryMode.FOOD) {
                    if (query.isBlank() && favoriteFoods.isNotEmpty()) {
                        Text("Favorites", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        favoriteFoods.forEach { food ->
                            EntryPickerRow(
                                title = food.name,
                                subtitle = "${food.servingLabel}  ${food.calories.toInt()} kcal",
                                onSelect = { onAddFood(food.id, servings) },
                            )
                        }
                    }
                    if (query.isBlank() && recentFoods.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Recent", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        recentFoods.forEach { food ->
                            EntryPickerRow(
                                title = food.name,
                                subtitle = "${food.servingLabel}  ${food.calories.toInt()} kcal",
                                onSelect = { onAddFood(food.id, servings) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("All foods", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    filteredFoods.forEach { food ->
                        EntryPickerRow(
                            title = food.name,
                            subtitle = "${food.servingLabel}  ${food.calories.toInt()} kcal",
                            onSelect = { onAddFood(food.id, servings) },
                        )
                    }
                } else {
                    filteredMeals.forEach { meal ->
                        EntryPickerRow(
                            title = meal.name,
                            subtitle = meal.summary,
                            onSelect = { onAddSavedMeal(meal.id, servings) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun EntryPickerRow(
    title: String,
    subtitle: String,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        onClick = onSelect,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ServingPresetRow(
    value: String,
    onValueChange: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("0.5", "1", "1.5", "2").forEach { preset ->
            FilterChip(
                selected = value == preset,
                onClick = { onValueChange(preset) },
                label = { Text("${preset}x") },
            )
        }
    }
}

private data class MetricValues(
    val label: String,
    val value: Double,
    val goal: Double?,
    val unit: String,
)

private enum class NutritionDestination { DIARY, FOODS, SAVED_MEALS }

private enum class EntryMode {
    FOOD,
    MEAL,
}

private val NutritionTrackingDepth.label: String
    get() = when (this) {
        NutritionTrackingDepth.DETAILED_MACROS -> "Full macros"
        NutritionTrackingDepth.CALORIES_PROTEIN -> "Calories + protein"
        NutritionTrackingDepth.MEAL_QUALITY -> "Meal check-in"
        NutritionTrackingDepth.DISABLED -> "Off"
    }

private val MealQuality.label: String
    get() = when (this) {
        MealQuality.BALANCED -> "Balanced"
        MealQuality.ONE_FOCUS -> "One focus"
        MealQuality.FLEXIBLE -> "Flexible"
    }

private val MealType.label: String
    get() = when (this) {
        MealType.BREAKFAST -> "Breakfast"
        MealType.LUNCH -> "Lunch"
        MealType.DINNER -> "Dinner"
        MealType.SNACK -> "Snacks"
    }
