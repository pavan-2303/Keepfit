package com.keepfit.app.di

import android.content.Context
import com.keepfit.app.profile.ProfileRepository
import com.keepfit.app.profile.RoomProfileRepository
import com.keepfit.app.BuildConfig
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.KeepfitDatabaseFactory
import com.keepfit.core.database.nutrition.NutritionDao
import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.core.database.transformation.TransformationDao
import com.keepfit.core.database.workout.WorkoutDao
import com.keepfit.core.media.ExerciseMediaStore
import com.keepfit.core.media.TransformationPhotoStore
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.DataStoreAppSettingsRepository
import com.keepfit.core.preferences.ReminderScheduler
import com.keepfit.core.preferences.WorkManagerReminderScheduler
import com.keepfit.feature.assistant.data.AssistantRepository
import com.keepfit.feature.assistant.data.AssistantRuntimeConfig
import com.keepfit.feature.assistant.data.OllamaAssistantRepository
import com.keepfit.feature.settings.data.BackupRepository
import com.keepfit.feature.settings.data.DeviceBackupRepository
import com.keepfit.feature.settings.data.RoomSettingsGoalsRepository
import com.keepfit.feature.settings.data.SettingsGoalsRepository
import com.keepfit.feature.steps.data.HealthConnectStepsRepository
import com.keepfit.feature.steps.data.StepsRepository
import com.keepfit.feature.nutrition.data.NutritionRepository
import com.keepfit.feature.nutrition.data.RoomNutritionRepository
import com.keepfit.feature.transformation.data.RoomTransformationRepository
import com.keepfit.feature.transformation.data.TransformationRepository
import com.keepfit.feature.workouts.data.RoomWorkoutRepository
import com.keepfit.feature.workouts.data.WorkoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KeepfitDatabase =
        KeepfitDatabaseFactory.create(context)

    @Provides
    fun provideBodyProfileDao(database: KeepfitDatabase): BodyProfileDao =
        database.bodyProfileDao()

    @Provides
    @Singleton
    fun provideProfileRepository(dao: BodyProfileDao): ProfileRepository =
        RoomProfileRepository(dao)

    @Provides
    @Singleton
    fun provideReminderScheduler(@ApplicationContext context: Context): ReminderScheduler =
        WorkManagerReminderScheduler(context)

    @Provides
    @Singleton
    fun provideAppSettingsRepository(
        @ApplicationContext context: Context,
        scheduler: ReminderScheduler,
    ): AppSettingsRepository = DataStoreAppSettingsRepository(context, scheduler)

    @Provides
    @Singleton
    fun provideAssistantRepository(
        repository: OllamaAssistantRepository,
    ): AssistantRepository = repository

    @Provides
    @Singleton
    fun provideAssistantBuildTimeConfig(): AssistantRuntimeConfig =
        AssistantRuntimeConfig(
            baseUrl = BuildConfig.OLLAMA_BASE_URL,
            generalChatModelName = BuildConfig.OLLAMA_GENERAL_CHAT_MODEL,
            reasoningModelName = BuildConfig.OLLAMA_REASONING_MODEL,
            apiKey = BuildConfig.OLLAMA_API_KEY,
        )

    @Provides
    @Singleton
    fun provideSettingsGoalsRepository(dao: BodyProfileDao): SettingsGoalsRepository =
        RoomSettingsGoalsRepository(dao)

    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
        database: KeepfitDatabase,
        settingsRepository: AppSettingsRepository,
    ): BackupRepository = DeviceBackupRepository(context, database, settingsRepository)

    @Provides
    @Singleton
    fun provideStepsRepository(
        @ApplicationContext context: Context,
    ): StepsRepository = HealthConnectStepsRepository(context)

    @Provides
    fun provideNutritionDao(database: KeepfitDatabase): NutritionDao =
        database.nutritionDao()

    @Provides
    @Singleton
    fun provideNutritionRepository(
        dao: NutritionDao,
        bodyProfileDao: BodyProfileDao,
    ): NutritionRepository = RoomNutritionRepository(dao, bodyProfileDao)

    @Provides
    fun provideTransformationDao(database: KeepfitDatabase): TransformationDao =
        database.transformationDao()

    @Provides
    fun provideWorkoutDao(database: KeepfitDatabase): WorkoutDao =
        database.workoutDao()

    @Provides
    @Singleton
    fun provideExerciseMediaStore(@ApplicationContext context: Context): ExerciseMediaStore =
        ExerciseMediaStore(context)

    @Provides
    @Singleton
    fun provideTransformationPhotoStore(@ApplicationContext context: Context): TransformationPhotoStore =
        TransformationPhotoStore(context)

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        dao: WorkoutDao,
        mediaStore: ExerciseMediaStore,
        settingsRepository: AppSettingsRepository,
    ): WorkoutRepository = RoomWorkoutRepository(dao, mediaStore, settingsRepository = settingsRepository)

    @Provides
    @Singleton
    fun provideTransformationRepository(
        dao: TransformationDao,
        bodyProfileDao: BodyProfileDao,
        nutritionDao: NutritionDao,
        workoutDao: WorkoutDao,
        photoStore: TransformationPhotoStore,
    ): TransformationRepository = RoomTransformationRepository(
        transformationDao = dao,
        bodyProfileDao = bodyProfileDao,
        nutritionDao = nutritionDao,
        workoutDao = workoutDao,
        photoStore = photoStore,
    )
}
