package com.keepfit.feature.nutrition.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.keepfit.feature.nutrition.NutritionViewModel
import com.keepfit.feature.nutrition.data.DailyNutritionSummary
import com.keepfit.feature.nutrition.data.DiaryEntry
import com.keepfit.feature.nutrition.data.Food
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
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFoodEditor by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<Food?>(null) }
    var showSavedMealEditor by remember { mutableStateOf(false) }
    var addMealType by remember { mutableStateOf<MealType?>(null) }
    var selectedLibraryTab by rememberSaveable { mutableStateOf(LibraryTab.FOODS) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = "NUTRITION",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Food diary",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(18.dp))
            DateHeader(
                date = dailySummary.date,
                onMoveBack = { viewModel.moveDate(-1) },
                onMoveForward = { viewModel.moveDate(1) },
                onDuplicateYesterday = viewModel::duplicateYesterday,
            )
            Spacer(modifier = Modifier.height(12.dp))
            NutritionSummaryCard(summary = dailySummary)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "DIARY",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            MealType.entries.forEach { mealType ->
                MealSection(
                    mealType = mealType,
                    entries = diaryEntries.filter { it.mealType == mealType },
                    onAdd = { addMealType = mealType },
                    onDeleteEntry = viewModel::deleteDiaryEntry,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "LIBRARY",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PrimaryTabRow(selectedTabIndex = selectedLibraryTab.ordinal) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedLibraryTab == tab,
                        onClick = { selectedLibraryTab = tab },
                        text = { Text(tab.label) },
                    )
                }
            }
            when (selectedLibraryTab) {
                LibraryTab.FOODS -> FoodLibrarySection(
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
                LibraryTab.SAVED_MEALS -> SavedMealsSection(
                    foods = foods,
                    meals = savedMeals,
                    onCreate = { showSavedMealEditor = true },
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
fun TodayNutritionSection(
    modifier: Modifier = Modifier,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val summary by viewModel.todaySummary.collectAsStateWithLifecycle()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.RestaurantMenu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "TODAY'S NUTRITION",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (!summary.hasEntries) {
                Text("No meals logged", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Daily calories and macros will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "${summary.totals.calories.toInt()} kcal",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SummaryMetrics(summary = summary)
            }
        }
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    onMoveBack: () -> Unit,
    onMoveForward: () -> Unit,
    onDuplicateYesterday: () -> Unit,
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
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Previous day")
                }
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onMoveForward) {
                    Icon(Icons.Outlined.ArrowForward, contentDescription = "Next day")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onDuplicateYesterday) {
                Text("Duplicate yesterday")
            }
        }
    }
}

@Composable
private fun NutritionSummaryCard(summary: DailyNutritionSummary) {
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
                    "Set nutrition goals later to compare calories and macros."
                } else {
                    "Compared against your saved daily goals."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            SummaryMetrics(summary = summary)
        }
    }
}

@Composable
private fun SummaryMetrics(summary: DailyNutritionSummary) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricCard(
            label = "Calories",
            value = summary.totals.calories.toInt().toString(),
            goal = summary.goals?.calorieGoal?.toInt()?.toString(),
            unit = "kcal",
        )
        MetricCard(
            label = "Protein",
            value = summary.totals.proteinGrams.formatQuantity(),
            goal = summary.goals?.proteinGoalGrams?.formatQuantity(),
            unit = "g",
        )
        MetricCard(
            label = "Carbs",
            value = summary.totals.carbohydrateGrams.formatQuantity(),
            goal = summary.goals?.carbohydrateGoalGrams?.formatQuantity(),
            unit = "g",
        )
        MetricCard(
            label = "Fat",
            value = summary.totals.fatGrams.formatQuantity(),
            goal = summary.goals?.fatGoalGrams?.formatQuantity(),
            unit = "g",
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    goal: String?,
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
                text = if (goal == null) "$value $unit" else "$value / $goal $unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MealSection(
    mealType: MealType,
    entries: List<DiaryEntry>,
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
                FilledTonalButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
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
                            "${entry.calories.toInt()} kcal  ${entry.proteinGrams.formatQuantity()}P  ${entry.carbohydrateGrams.formatQuantity()}C  ${entry.fatGrams.formatQuantity()}F",
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

private enum class LibraryTab(val label: String) {
    FOODS("Foods"),
    SAVED_MEALS("Saved meals"),
}

private enum class EntryMode {
    FOOD,
    MEAL,
}

private val MealType.label: String
    get() = when (this) {
        MealType.BREAKFAST -> "Breakfast"
        MealType.LUNCH -> "Lunch"
        MealType.DINNER -> "Dinner"
        MealType.SNACK -> "Snacks"
    }
