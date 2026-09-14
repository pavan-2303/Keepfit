package com.keepfit.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keepfit.core.database.nutrition.MealType
import com.keepfit.core.database.nutrition.MealQuality
import com.keepfit.core.preferences.AppSettings
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.NutritionTrackingDepth
import com.keepfit.feature.nutrition.data.DailyNutritionSummary
import com.keepfit.feature.nutrition.data.DiaryEntry
import com.keepfit.feature.nutrition.data.Food
import com.keepfit.feature.nutrition.data.MealQualityCheckIn
import com.keepfit.feature.nutrition.data.NutritionRepository
import com.keepfit.feature.nutrition.data.SavedMeal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val repository: NutritionRepository,
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val foodSearchQuery = MutableStateFlow("")

    val foods: StateFlow<List<Food>> = foodSearchQuery
        .flatMapLatest(repository::observeFoods)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteFoods: StateFlow<List<Food>> = repository.observeFavoriteFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentFoods: StateFlow<List<Food>> = repository.observeRecentFoods(limit = 8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedMeals: StateFlow<List<SavedMeal>> = repository.observeSavedMeals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val diaryEntries: StateFlow<List<DiaryEntry>> = selectedDate
        .flatMapLatest(repository::observeDiaryEntries)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailySummary: StateFlow<DailyNutritionSummary> = selectedDate
        .flatMapLatest(repository::observeDailySummary)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DailyNutritionSummary(
                date = LocalDate.now(),
                totals = com.keepfit.feature.nutrition.data.NutritionTotals(0.0, 0.0, 0.0, 0.0),
                goals = null,
                hasEntries = false,
            ),
        )

    val todaySummary: StateFlow<DailyNutritionSummary> = repository.observeDailySummary(LocalDate.now())
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DailyNutritionSummary(
                date = LocalDate.now(),
                totals = com.keepfit.feature.nutrition.data.NutritionTotals(0.0, 0.0, 0.0, 0.0),
                goals = null,
                hasEntries = false,
            ),
        )

    val appSettings: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val mealQualityCheckIns: StateFlow<List<MealQualityCheckIn>> = selectedDate
        .flatMapLatest(repository::observeMealQualityCheckIns)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayMealQualityCheckIns: StateFlow<List<MealQualityCheckIn>> =
        repository.observeMealQualityCheckIns(LocalDate.now())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val previousMealTypes: StateFlow<List<MealType>> = selectedDate
        .flatMapLatest(repository::observePreviousMealTypes)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun moveDate(days: Long) {
        selectedDate.value = selectedDate.value.plusDays(days)
    }

    fun searchFoods(query: String) {
        foodSearchQuery.value = query
    }

    fun saveFood(
        id: String?,
        name: String,
        servingLabel: String,
        servingAmount: String,
        calories: String,
        proteinGrams: String,
        carbohydrateGrams: String,
        fatGrams: String,
    ) {
        when (
            val result = FoodInputValidator.validate(
                name = name,
                servingLabel = servingLabel,
                servingAmount = servingAmount,
                calories = calories,
                proteinGrams = proteinGrams,
                carbohydrateGrams = carbohydrateGrams,
                fatGrams = fatGrams,
            )
        ) {
            is FoodValidationResult.Invalid -> _message.value = result.message
            is FoodValidationResult.Valid -> launchWrite("Food saved.") {
                repository.saveFood(id, result.input)
            }
        }
    }

    fun toggleFavorite(food: Food) = launchWrite(
        if (food.isFavorite) "Removed from favorites." else "Added to favorites.",
    ) {
        repository.toggleFavorite(food.id, !food.isFavorite)
    }

    fun archiveFood(foodId: String) = launchWrite("Food archived.") {
        repository.archiveFood(foodId)
    }

    fun createSavedMeal(name: String, items: List<Pair<String, String>>) {
        MealInputValidator.validateSavedMeal(name, items)
            .onSuccess { input ->
                launchWrite("Saved meal created.") {
                    repository.createSavedMeal(input)
                }
            }
            .onFailure { _message.value = it.message }
    }

    fun addFoodToDiary(mealType: MealType, foodId: String, servings: String) {
        MealInputValidator.validateServings(servings)
            .onSuccess { normalizedServings ->
                launchWrite("Entry added.") {
                    repository.addFoodToDiary(selectedDate.value, mealType, foodId, normalizedServings)
                }
            }
            .onFailure { _message.value = it.message }
    }

    fun addSavedMealToDiary(mealType: MealType, savedMealId: String, multiplier: String) {
        MealInputValidator.validateServings(multiplier)
            .onSuccess { normalizedMultiplier ->
                launchWrite("Saved meal added.") {
                    repository.addSavedMealToDiary(selectedDate.value, mealType, savedMealId, normalizedMultiplier)
                }
            }
            .onFailure { _message.value = it.message }
    }

    fun deleteDiaryEntry(entryId: String) = launchWrite("Entry removed.") {
        repository.deleteDiaryEntry(entryId)
    }

    fun duplicateYesterday() = launchWrite("Copied yesterday's meals.") {
        repository.duplicatePreviousDay(selectedDate.value)
    }

    fun repeatYesterdayMeal(mealType: MealType) = launchWrite("Repeated yesterday's ${mealType.label.lowercase()}.") {
        repository.repeatPreviousMeal(selectedDate.value, mealType)
    }

    fun setMealQuality(mealType: MealType, quality: MealQuality) = launchWrite(null) {
        repository.setMealQuality(selectedDate.value, mealType, quality)
    }

    fun updateTrackingDepth(depth: NutritionTrackingDepth) = launchWrite("Nutrition view updated.") {
        settingsRepository.updateNutritionTracking(depth, appSettings.value.nutritionTargetRangePercent)
    }

    fun updateTargetRangePercent(percent: Int) = launchWrite("Target range updated.") {
        settingsRepository.updateNutritionTracking(appSettings.value.nutritionTrackingDepth, percent)
    }

    fun dismissMessage() {
        _message.value = null
    }

    private fun launchWrite(successMessage: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _message.value = successMessage }
                .onFailure { _message.value = it.message ?: "Something went wrong." }
        }
    }
}

private val MealType.label: String
    get() = name.lowercase().replaceFirstChar(Char::uppercase)
