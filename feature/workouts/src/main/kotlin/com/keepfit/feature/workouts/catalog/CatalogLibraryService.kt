package com.keepfit.feature.workouts.catalog

import com.keepfit.feature.workouts.ExerciseInput
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.feature.workouts.data.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class NewCatalogExercise(
    val name: String,
    val muscleGroup: String,
    val instructions: String,
    val isBodyweight: Boolean,
)

enum class CatalogAddOutcome {
    ADDED,
    ALREADY_PRESENT,
}

interface ExerciseLibraryGateway {
    suspend fun activeExercises(): List<Exercise>
    suspend fun add(exercise: NewCatalogExercise)
}

class WorkoutExerciseLibraryGateway(
    private val repository: WorkoutRepository,
) : ExerciseLibraryGateway {
    override suspend fun activeExercises(): List<Exercise> = repository.observeExercises("").first()

    override suspend fun add(exercise: NewCatalogExercise) {
        repository.saveExercise(
            id = null,
            input = ExerciseInput(
                name = exercise.name,
                muscleGroup = exercise.muscleGroup,
                instructions = exercise.instructions,
                notes = "Added from the Keepfit offline guide.",
                isBodyweight = exercise.isBodyweight,
            ),
            mediaUri = null,
        )
    }
}

class CatalogLibraryService(
    private val gateway: ExerciseLibraryGateway,
) {
    private val addMutex = Mutex()

    suspend fun addOfflineGuide(guide: CatalogExercise): CatalogAddOutcome = addMutex.withLock {
        require(guide.origin == CatalogOrigin.KEEPFIT_OFFLINE) {
            "Live catalogue exercises are view-only."
        }
        if (gateway.activeExercises().any { it.name.equals(guide.name, ignoreCase = true) }) {
            return CatalogAddOutcome.ALREADY_PRESENT
        }
        gateway.add(
            NewCatalogExercise(
                name = guide.name,
                muscleGroup = guide.bodyParts.firstOrNull() ?: guide.movementArea,
                instructions = guide.instructions.joinToString("\n"),
                isBodyweight = guide.isBodyweight,
            ),
        )
        CatalogAddOutcome.ADDED
    }
}
