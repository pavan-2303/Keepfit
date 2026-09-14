package com.keepfit.feature.workouts.catalog

enum class CatalogOrigin {
    KEEPFIT_OFFLINE,
    EXERCISE_DB_LIVE,
}

data class CatalogQuery(
    val text: String = "",
    val movementArea: String? = null,
    val targetMuscle: String? = null,
    val equipment: String? = null,
)

data class CatalogExercise(
    val id: String,
    val name: String,
    val movementArea: String,
    val bodyParts: List<String>,
    val targetMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: List<String>,
    val instructions: List<String>,
    val isBodyweight: Boolean,
    val demoUrl: String?,
    val origin: CatalogOrigin,
    val attribution: String,
)

enum class CatalogFailureKind {
    OFFLINE,
    TIMEOUT,
    MALFORMED_RESPONSE,
    PROVIDER_ERROR,
}

sealed interface CatalogSearchResult {
    data class Success(
        val items: List<CatalogExercise>,
        val total: Int,
        val hasMore: Boolean,
    ) : CatalogSearchResult

    data class Failure(
        val kind: CatalogFailureKind,
        val message: String,
    ) : CatalogSearchResult
}

fun interface ExerciseCatalogProvider {
    suspend fun search(query: CatalogQuery): CatalogSearchResult
}
