package com.keepfit.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.keepfit.core.database.nutrition.FoodDiaryEntryEntity
import com.keepfit.core.database.nutrition.FoodEntity
import com.keepfit.core.database.nutrition.NutritionDao
import com.keepfit.core.database.nutrition.SavedMealEntity
import com.keepfit.core.database.nutrition.SavedMealItemEntity
import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.database.transformation.BodyMeasurementEntity
import com.keepfit.core.database.transformation.TransformationDao
import com.keepfit.core.database.transformation.TransformationPhotoEntity
import com.keepfit.core.database.transformation.TransformationWeekEntity
import com.keepfit.core.database.workout.ExerciseEntity
import com.keepfit.core.database.workout.ExerciseLogEntity
import com.keepfit.core.database.workout.ExerciseMediaEntity
import com.keepfit.core.database.workout.PlannedWorkoutEntity
import com.keepfit.core.database.workout.SetLogEntity
import com.keepfit.core.database.workout.WeeklyPlanEntity
import com.keepfit.core.database.workout.WorkoutDao
import com.keepfit.core.database.workout.WorkoutSessionEntity
import com.keepfit.core.database.workout.WorkoutTemplateEntity
import com.keepfit.core.database.workout.WorkoutTemplateExerciseEntity

@Database(
    entities = [
        BodyProfileEntity::class,
        FoodEntity::class,
        SavedMealEntity::class,
        SavedMealItemEntity::class,
        FoodDiaryEntryEntity::class,
        BodyMeasurementEntity::class,
        TransformationWeekEntity::class,
        TransformationPhotoEntity::class,
        ExerciseEntity::class,
        ExerciseMediaEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutTemplateExerciseEntity::class,
        WeeklyPlanEntity::class,
        PlannedWorkoutEntity::class,
        WorkoutSessionEntity::class,
        ExerciseLogEntity::class,
        SetLogEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(KeepfitTypeConverters::class)
abstract class KeepfitDatabase : RoomDatabase() {
    abstract fun bodyProfileDao(): BodyProfileDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun transformationDao(): TransformationDao
    abstract fun workoutDao(): WorkoutDao
}
