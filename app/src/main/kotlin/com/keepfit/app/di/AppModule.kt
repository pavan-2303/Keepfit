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
import com.keepfit.feature.assistant.data.AssistantNutritionSnapshot
import com.keepfit.feature.assistant.data.AssistantPlanApplier
import com.keepfit.feature.assistant.data.AssistantProgressSnapshot
import com.keepfit.feature.assistant.data.AssistantRecordSummary
import com.keepfit.feature.assistant.data.AssistantRecentWorkoutSummary
import com.keepfit.feature.assistant.data.AssistantStepsSnapshotSummary
import com.keepfit.feature.assistant.data.AssistantSummaryDataSource
import com.keepfit.feature.assistant.data.AssistantTransformationCycleSnapshot
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
import com.keepfit.feature.workouts.ExerciseInput
import com.keepfit.feature.workouts.data.RoomWorkoutRepository
import com.keepfit.feature.workouts.data.WorkoutRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

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
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideAssistantSummaryDataSource(
        workoutRepository: WorkoutRepository,
        nutritionRepository: NutritionRepository,
        transformationRepository: TransformationRepository,
        stepsRepository: StepsRepository,
    ): AssistantSummaryDataSource = object : AssistantSummaryDataSource {
        override suspend fun readRecentWorkouts(): List<AssistantRecentWorkoutSummary> =
            workoutRepository.observeHistory()
                .first()
                .sortedByDescending { it.workoutDate }
                .take(5)
                .map { workout ->
                    AssistantRecentWorkoutSummary(
                        workoutDate = workout.workoutDate,
                        exerciseNames = workout.exerciseNames,
                    )
                }

        override suspend fun readRecords(): List<AssistantRecordSummary> =
            workoutRepository.observeRecords()
                .first()
                .sortedByDescending { it.highestWeightKg }
                .take(5)
                .map { record ->
                    AssistantRecordSummary(
                        exerciseName = record.exerciseName,
                        highestWeightKg = record.highestWeightKg,
                        highestRepetitions = record.highestRepetitions,
                    )
                }

        override suspend fun readNutritionSnapshot(onDate: java.time.LocalDate): AssistantNutritionSnapshot {
            val summary = nutritionRepository.observeDailySummary(onDate).first()
            val goals = summary.goals
            return AssistantNutritionSnapshot(
                date = summary.date,
                calories = summary.totals.calories,
                proteinGrams = summary.totals.proteinGrams,
                carbohydrateGrams = summary.totals.carbohydrateGrams,
                fatGrams = summary.totals.fatGrams,
                calorieGoal = goals?.calorieGoal,
                proteinGoalGrams = goals?.proteinGoalGrams,
                carbohydrateGoalGrams = goals?.carbohydrateGoalGrams,
                fatGoalGrams = goals?.fatGoalGrams,
                hasEntries = summary.hasEntries,
            )
        }

        override suspend fun readProgressSnapshot(): AssistantProgressSnapshot {
            val overview = transformationRepository.observeCurrentOverview().first()
            val activeCycle = transformationRepository.observeTimeline().first().activeCycle
            return AssistantProgressSnapshot(
                latestMeasurementDate = overview.latestMeasurement?.measurementDate,
                latestWeightKg = overview.latestMeasurement?.weightKg,
                heightCm = overview.heightCm,
                bmi = overview.bmi,
                activeCycle = activeCycle?.let { cycle ->
                    AssistantTransformationCycleSnapshot(
                        startDate = cycle.startDate,
                        latestCaptureDate = cycle.latestCaptureDate,
                        latestDayNumber = cycle.defaultComparison.rightDay.dayNumber,
                        workoutsCompleted = cycle.summary.workoutsCompleted,
                        averageCalories = cycle.summary.averageCalories,
                        averageProteinGrams = cycle.summary.averageProteinGrams,
                        averageCarbohydrateGrams = cycle.summary.averageCarbohydrateGrams,
                        averageFatGrams = cycle.summary.averageFatGrams,
                        weightChangeKg = cycle.summary.weightChangeKg,
                    )
                },
            )
        }

        override suspend fun readStepsSnapshot(): AssistantStepsSnapshotSummary? =
            runCatching { stepsRepository.loadSnapshot() }.getOrNull()?.let { snapshot ->
                when (snapshot) {
                    is com.keepfit.feature.steps.data.StepsSnapshot.Connected -> AssistantStepsSnapshotSummary(
                        todaySteps = snapshot.summary.todaySteps,
                        sevenDayTotal = snapshot.summary.sevenDayTotal,
                    )

                    else -> null
                }
            }
    }

    @Provides
    @Singleton
    fun provideAssistantPlanApplier(
        workoutRepository: WorkoutRepository,
    ): AssistantPlanApplier = object : AssistantPlanApplier {
        override suspend fun applyDraftPlan(draft: com.keepfit.feature.assistant.data.AssistantDraftWorkoutPlan): Result<Unit> =
            runCatching {
                java.time.DayOfWeek.entries.forEach { day ->
                    workoutRepository.clearPlannedWorkout(day)
                }
                val createdTemplateIdsByKey = linkedMapOf<String, String>()

                draft.days.forEach { day ->
                    val templateKey = buildString {
                        append(day.templateName.trim().lowercase())
                        append("|")
                        append(day.exercises.joinToString("|") { it.name.trim().lowercase() })
                    }
                    val templateId = createdTemplateIdsByKey.getOrPut(templateKey) {
                        val exerciseIds = day.exercises.map { exercise ->
                            resolveExerciseId(workoutRepository, exercise.name)
                        }
                        createTemplateAndResolveId(
                            workoutRepository = workoutRepository,
                            templateName = day.templateName,
                            exerciseIds = exerciseIds,
                        )
                    }
                    workoutRepository.assignTemplate(day.dayOfWeek, templateId)
                }
            }

        private suspend fun resolveExerciseId(
            workoutRepository: WorkoutRepository,
            exerciseName: String,
        ): String {
            val normalizedName = exerciseName.trim()
            workoutRepository.observeExercises("")
                .first()
                .firstOrNull { it.name.equals(normalizedName, ignoreCase = true) }
                ?.let { return it.id }

            val beforeIds = workoutRepository.observeExercises("").first().map { it.id }.toSet()
            workoutRepository.saveExercise(
                id = null,
                input = ExerciseInput(
                    name = normalizedName,
                    muscleGroup = "General",
                    instructions = null,
                    notes = "Created from assistant draft plan.",
                    isBodyweight = false,
                ),
                mediaUri = null,
            )

            return workoutRepository.observeExercises("")
                .first()
                .firstOrNull { exercise ->
                    exercise.id !in beforeIds && exercise.name.equals(normalizedName, ignoreCase = true)
                }
                ?.id
                ?: workoutRepository.observeExercises("")
                    .first()
                    .firstOrNull { it.name.equals(normalizedName, ignoreCase = true) }
                    ?.id
                ?: error("Assistant draft plan could not create exercise '$normalizedName'.")
        }

        private suspend fun createTemplateAndResolveId(
            workoutRepository: WorkoutRepository,
            templateName: String,
            exerciseIds: List<String>,
        ): String {
            val beforeIds = workoutRepository.observeTemplates().first().map { it.id }.toSet()
            workoutRepository.createTemplate(templateName, exerciseIds)
            return workoutRepository.observeTemplates()
                .first()
                .firstOrNull { template ->
                    template.id !in beforeIds && template.name == templateName
                }
                ?.id
                ?: workoutRepository.observeTemplates()
                    .first()
                    .lastOrNull { it.name == templateName }
                    ?.id
                ?: error("Assistant draft plan could not create template '$templateName'.")
        }
    }

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
