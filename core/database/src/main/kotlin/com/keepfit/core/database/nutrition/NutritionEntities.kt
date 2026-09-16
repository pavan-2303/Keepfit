package com.keepfit.core.database.nutrition

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.keepfit.core.database.profile.BodyProfileEntity
import java.time.LocalDate

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
}

enum class MealQuality {
    BALANCED,
    ONE_FOCUS,
    FLEXIBLE,
}

@Entity(
    tableName = "meal_quality_check_ins",
    foreignKeys = [
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
        ),
    ],
    indices = [
        Index("bodyProfileId"),
        Index(value = ["bodyProfileId", "diaryDate", "mealType"], unique = true),
    ],
)
data class MealQualityCheckInEntity(
    @PrimaryKey val id: String,
    val diaryDate: LocalDate,
    val mealType: MealType,
    val quality: MealQuality,
    val loggedAt: Long,
    val bodyProfileId: String = "",
)

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

@Entity(
    tableName = "saved_meals",
    foreignKeys = [
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
        ),
    ],
    indices = [Index("bodyProfileId")],
)
data class SavedMealEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val bodyProfileId: String = "",
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
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
        ),
    ],
    indices = [Index("bodyProfileId"), Index("diaryDate"), Index("foodId"), Index("savedMealId")],
)
data class FoodDiaryEntryEntity(
    @PrimaryKey val id: String,
    val diaryDate: LocalDate,
    val mealType: MealType,
    val foodId: String,
    val savedMealId: String?,
    val servings: Double,
    val loggedAt: Long,
    val bodyProfileId: String = "",
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

data class DailyNutritionTotalsByDateRow(
    val diaryDate: LocalDate,
    val calories: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
)

data class RecentFoodRow(
    @Embedded val food: FoodEntity,
    val lastUsedAt: Long,
)
