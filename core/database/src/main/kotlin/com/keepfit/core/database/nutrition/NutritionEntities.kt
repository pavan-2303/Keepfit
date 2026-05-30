package com.keepfit.core.database.nutrition

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
}

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val id: String,
    val name: String,
    val servingLabel: String,
    val servingAmount: Double,
    val calories: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long?,
)

@Entity(tableName = "saved_meals")
data class SavedMealEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "saved_meal_items",
    foreignKeys = [
        ForeignKey(
            entity = SavedMealEntity::class,
            parentColumns = ["id"],
            childColumns = ["savedMealId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
        ),
    ],
    indices = [Index("savedMealId"), Index("foodId")],
)
data class SavedMealItemEntity(
    @PrimaryKey val id: String,
    val savedMealId: String,
    val foodId: String,
    val servings: Double,
    val position: Int,
)

@Entity(
    tableName = "food_diary_entries",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
        ),
        ForeignKey(
            entity = SavedMealEntity::class,
            parentColumns = ["id"],
            childColumns = ["savedMealId"],
        ),
    ],
    indices = [Index("diaryDate"), Index("foodId"), Index("savedMealId")],
)
data class FoodDiaryEntryEntity(
    @PrimaryKey val id: String,
    val diaryDate: LocalDate,
    val mealType: MealType,
    val foodId: String,
    val savedMealId: String?,
    val servings: Double,
    val loggedAt: Long,
)

data class SavedMealItemWithFood(
    @Embedded val item: SavedMealItemEntity,
    @Relation(parentColumn = "foodId", entityColumn = "id")
    val food: FoodEntity,
) {
    val foodName: String get() = food.name
}

data class SavedMealDetails(
    @Embedded val meal: SavedMealEntity,
    @Relation(
        entity = SavedMealItemEntity::class,
        parentColumn = "id",
        entityColumn = "savedMealId",
    )
    val items: List<SavedMealItemWithFood>,
)

data class FoodDiaryEntryDetails(
    @Embedded val entry: FoodDiaryEntryEntity,
    @Relation(parentColumn = "foodId", entityColumn = "id")
    val food: FoodEntity,
) {
    val foodName: String get() = food.name
    val mealType: MealType get() = entry.mealType
    val servings: Double get() = entry.servings
}

data class DailyNutritionTotalsRow(
    val calories: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
)

data class RecentFoodRow(
    @Embedded val food: FoodEntity,
    val lastUsedAt: Long,
)
