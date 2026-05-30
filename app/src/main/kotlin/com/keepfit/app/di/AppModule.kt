package com.keepfit.app.di

import android.content.Context
import com.keepfit.app.profile.ProfileRepository
import com.keepfit.app.profile.RoomProfileRepository
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.KeepfitDatabaseFactory
import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.core.database.workout.WorkoutDao
import com.keepfit.core.media.ExerciseMediaStore
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
    fun provideWorkoutDao(database: KeepfitDatabase): WorkoutDao =
        database.workoutDao()

    @Provides
    @Singleton
    fun provideExerciseMediaStore(@ApplicationContext context: Context): ExerciseMediaStore =
        ExerciseMediaStore(context)

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        dao: WorkoutDao,
        mediaStore: ExerciseMediaStore,
    ): WorkoutRepository = RoomWorkoutRepository(dao, mediaStore)
}
