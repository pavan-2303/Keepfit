package com.keepfit.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.keepfit.core.database.assistant.AssistantConversationDao
import com.keepfit.core.database.assistant.AssistantConversationEntity
import com.keepfit.core.database.assistant.AssistantMessageEntity
import com.keepfit.core.database.journey.JourneyDao
import com.keepfit.core.database.journey.JourneyProfileEntity
import com.keepfit.core.database.nutrition.FoodDiaryEntryEntity
import com.keepfit.core.database.nutrition.FoodEntity
import com.keepfit.core.database.nutrition.MealQualityCheckInEntity
import com.keepfit.core.database.nutrition.NutritionDao
import com.keepfit.core.database.nutrition.SavedMealEntity
import com.keepfit.core.database.nutrition.SavedMealItemEntity
import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.database.profile.ProfileSetupDao
import com.keepfit.core.database.review.WeeklyReviewDao
import com.keepfit.core.database.review.WeeklyReviewOutcomeEntity
import com.keepfit.core.database.transformation.BodyMeasurementEntity
import com.keepfit.core.database.transformation.TransformationCycleEntity
import com.keepfit.core.database.transformation.TransformationDao
import com.keepfit.core.database.transformation.TransformationPhotoEntity
import com.keepfit.core.database.transformation.TransformationPosePreferenceEntity
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
import com.keepfit.core.database.workout.WorkoutOccurrenceEntity
import com.keepfit.core.database.workout.WorkoutOccurrenceExerciseEntity

@Database(
    entities = [
        BodyProfileEntity::class,
        FoodEntity::class,
        SavedMealEntity::class,
        SavedMealItemEntity::class,
        FoodDiaryEntryEntity::class,
        MealQualityCheckInEntity::class,
        BodyMeasurementEntity::class,
        TransformationCycleEntity::class,
        TransformationPhotoEntity::class,
        TransformationPosePreferenceEntity::class,
        ExerciseEntity::class,
        ExerciseMediaEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutTemplateExerciseEntity::class,
        WeeklyPlanEntity::class,
        PlannedWorkoutEntity::class,
        WorkoutSessionEntity::class,
        ExerciseLogEntity::class,
        SetLogEntity::class,
        JourneyProfileEntity::class,
        WorkoutOccurrenceEntity::class,
        WorkoutOccurrenceExerciseEntity::class,
        WeeklyReviewOutcomeEntity::class,
        AssistantConversationEntity::class,
        AssistantMessageEntity::class,
    ],
    version = 16,
    exportSchema = true,
)
@TypeConverters(KeepfitTypeConverters::class)
abstract class KeepfitDatabase : RoomDatabase() {
    abstract fun bodyProfileDao(): BodyProfileDao
    abstract fun profileSetupDao(): ProfileSetupDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun transformationDao(): TransformationDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun journeyDao(): JourneyDao
    abstract fun weeklyReviewDao(): WeeklyReviewDao
    abstract fun assistantConversationDao(): AssistantConversationDao

    companion object {
        const val VERSION = 16
    }
}
