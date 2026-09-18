package com.keepfit.feature.workouts.planning

import java.time.DayOfWeek

enum class JourneyGoal(val label: String) {
    GENERAL_FITNESS("General fitness"),
    CONSISTENCY("Build consistency"),
    STRENGTH("Get stronger"),
    MUSCLE_GAIN("Build muscle"),
    FAT_LOSS("Support fat loss"),
}

enum class ExperienceLevel(val label: String) {
    BEGINNER("New or returning"),
    INTERMEDIATE("Comfortable with the basics"),
    EXPERIENCED("Experienced"),
}

enum class EquipmentOption(val label: String) {
    BODYWEIGHT("Bodyweight"),
    DUMBBELLS("Dumbbells"),
    RESISTANCE_BANDS("Resistance bands"),
    FULL_GYM("Full gym"),
}

enum class ActivityLevel(val label: String) {
    MOSTLY_SEATED("Mostly seated"),
    LIGHTLY_ACTIVE("Lightly active"),
    ACTIVE("Active"),
    HIGHLY_ACTIVE("Highly active"),
}

enum class SleepDuration(val label: String) {
    UNDER_SIX_HOURS("Usually under 6 hours"),
    SIX_TO_SEVEN_HOURS("Usually 6–7 hours"),
    SEVEN_TO_EIGHT_HOURS("Usually 7–8 hours"),
    OVER_EIGHT_HOURS("Usually over 8 hours"),
    VARIABLE("It varies a lot"),
}

enum class SleepSchedule(val label: String) {
    REGULAR("Fairly regular"),
    IRREGULAR("Irregular"),
    SHIFT_BASED("Shift-based"),
}

enum class CurrentBuild(val label: String) {
    LEAN("Lean build"),
    AVERAGE("Average build"),
    MUSCULAR("Muscular build"),
    LARGER_BUILD("Larger build"),
    NOT_SURE("Not sure / skip"),
}

enum class RoutineChallenge(val label: String) {
    INCONSISTENT_SCHEDULE("Schedule changes"),
    LONG_WORKDAYS("Long workdays"),
    LOW_ENERGY("Low energy"),
    HIGH_STRESS("High stress"),
    FREQUENT_TRAVEL("Frequent travel"),
    STAYING_CONSISTENT("Staying consistent"),
}

enum class LimitationArea(val label: String) {
    SHOULDERS("Shoulders"),
    WRISTS("Wrists or hands"),
    BACK("Back"),
    HIPS("Hips"),
    KNEES("Knees"),
    ANKLES("Ankles or feet"),
    OTHER("Other"),
}

data class StarterPlanInput(
    val goal: JourneyGoal,
    val experienceLevel: ExperienceLevel,
    val preferredDays: Set<DayOfWeek>,
    val sessionMinutes: Int,
    val equipment: Set<EquipmentOption>,
    val avoidedExerciseKeys: Set<String>,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHTLY_ACTIVE,
    val sleepDuration: SleepDuration = SleepDuration.SEVEN_TO_EIGHT_HOURS,
    val sleepSchedule: SleepSchedule = SleepSchedule.REGULAR,
    val currentBuild: CurrentBuild = CurrentBuild.NOT_SURE,
    val routineChallenges: Set<RoutineChallenge> = emptySet(),
    val limitationAreas: Set<LimitationArea> = emptySet(),
    val limitationNotes: String? = null,
)

data class StarterPlanExercise(
    val key: String,
    val name: String,
    val muscleGroup: String,
    val instructions: String,
    val isBodyweight: Boolean,
    val targetSets: Int,
    val targetReps: String,
)

data class StarterPlanDay(
    val dayOfWeek: DayOfWeek,
    val templateName: String,
    val exercises: List<StarterPlanExercise>,
)

data class StarterWeekDraft(
    val name: String,
    val rationale: String,
    val days: List<StarterPlanDay>,
)

enum class MovementPattern {
    SQUAT,
    SINGLE_LEG,
    HINGE,
    PUSH,
    PULL,
    CORE,
}

data class StarterExerciseDefinition(
    val key: String,
    val name: String,
    val muscleGroup: String,
    val instructions: String,
    val isBodyweight: Boolean,
    val movementPattern: MovementPattern,
    val equipment: Set<EquipmentOption>,
)

object StarterExerciseCatalog {
    val exercises: List<StarterExerciseDefinition> = listOf(
        StarterExerciseDefinition(
            key = "chair-squat",
            name = "Chair Squat",
            muscleGroup = "Legs",
            instructions = "Stand in front of a stable chair, sit back under control, touch lightly, and stand tall.",
            isBodyweight = true,
            movementPattern = MovementPattern.SQUAT,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "bodyweight-squat",
            name = "Bodyweight Squat",
            muscleGroup = "Legs",
            instructions = "Brace gently, sit the hips down between the feet, keep the whole foot planted, and stand tall.",
            isBodyweight = true,
            movementPattern = MovementPattern.SQUAT,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "reverse-lunge",
            name = "Reverse Lunge",
            muscleGroup = "Legs",
            instructions = "Step one foot back, lower with control, keep the front foot planted, and return to standing.",
            isBodyweight = true,
            movementPattern = MovementPattern.SINGLE_LEG,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "goblet-squat",
            name = "Goblet Squat",
            muscleGroup = "Legs",
            instructions = "Hold one dumbbell close to the chest, squat with a stable torso, and drive through the whole foot.",
            isBodyweight = false,
            movementPattern = MovementPattern.SQUAT,
            equipment = setOf(EquipmentOption.DUMBBELLS, EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "leg-press",
            name = "Leg Press",
            muscleGroup = "Legs",
            instructions = "Set the seat for a comfortable depth, keep the hips supported, press smoothly, and avoid locking the knees.",
            isBodyweight = false,
            movementPattern = MovementPattern.SQUAT,
            equipment = setOf(EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "glute-bridge",
            name = "Glute Bridge",
            muscleGroup = "Glutes",
            instructions = "Lie with knees bent, brace gently, squeeze the glutes to lift the hips, and lower with control.",
            isBodyweight = true,
            movementPattern = MovementPattern.HINGE,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "dumbbell-romanian-deadlift",
            name = "Dumbbell Romanian Deadlift",
            muscleGroup = "Hamstrings",
            instructions = "Keep the dumbbells close, push the hips back with a long spine, then stand by driving the hips forward.",
            isBodyweight = false,
            movementPattern = MovementPattern.HINGE,
            equipment = setOf(EquipmentOption.DUMBBELLS, EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "incline-push-up",
            name = "Incline Push-Up",
            muscleGroup = "Chest",
            instructions = "Place hands on a stable raised surface, keep a straight body line, lower the chest, and press away.",
            isBodyweight = true,
            movementPattern = MovementPattern.PUSH,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "dumbbell-bench-press",
            name = "Dumbbell Bench Press",
            muscleGroup = "Chest",
            instructions = "Keep the feet planted and shoulders supported, lower the dumbbells with control, and press smoothly.",
            isBodyweight = false,
            movementPattern = MovementPattern.PUSH,
            equipment = setOf(EquipmentOption.DUMBBELLS, EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "dumbbell-overhead-press",
            name = "Dumbbell Overhead Press",
            muscleGroup = "Shoulders",
            instructions = "Brace the torso, begin with dumbbells near shoulder height, press overhead, and lower under control.",
            isBodyweight = false,
            movementPattern = MovementPattern.PUSH,
            equipment = setOf(EquipmentOption.DUMBBELLS, EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "one-arm-dumbbell-row",
            name = "One-Arm Dumbbell Row",
            muscleGroup = "Back",
            instructions = "Support the torso, keep a long spine, pull the elbow toward the hip, and lower without twisting.",
            isBodyweight = false,
            movementPattern = MovementPattern.PULL,
            equipment = setOf(EquipmentOption.DUMBBELLS, EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "resistance-band-row",
            name = "Resistance-Band Row",
            muscleGroup = "Back",
            instructions = "Anchor the band securely, sit or stand tall, pull the elbows back, and return slowly.",
            isBodyweight = false,
            movementPattern = MovementPattern.PULL,
            equipment = setOf(EquipmentOption.RESISTANCE_BANDS, EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "lat-pulldown",
            name = "Lat Pulldown",
            muscleGroup = "Back",
            instructions = "Sit securely, pull the bar toward the upper chest without leaning back, and return with control.",
            isBodyweight = false,
            movementPattern = MovementPattern.PULL,
            equipment = setOf(EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "prone-w-raise",
            name = "Prone W Raise",
            muscleGroup = "Upper back",
            instructions = "Lie face down, bend the elbows into a W, lift the hands and elbows slightly, then lower slowly.",
            isBodyweight = true,
            movementPattern = MovementPattern.PULL,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "dead-bug",
            name = "Dead Bug",
            muscleGroup = "Core",
            instructions = "Keep the lower back gently supported, extend opposite arm and leg slowly, and return without rushing.",
            isBodyweight = true,
            movementPattern = MovementPattern.CORE,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "bird-dog",
            name = "Bird Dog",
            muscleGroup = "Core",
            instructions = "From hands and knees, reach opposite arm and leg long, keep the hips level, and return with control.",
            isBodyweight = true,
            movementPattern = MovementPattern.CORE,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "step-up",
            name = "Step-Up",
            muscleGroup = "Legs",
            instructions = "Place the whole foot on a stable step, drive through that foot to stand tall, and lower with control.",
            isBodyweight = true,
            movementPattern = MovementPattern.SINGLE_LEG,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "split-squat",
            name = "Split Squat",
            muscleGroup = "Legs",
            instructions = "Take a comfortable staggered stance, lower straight down with both feet planted, and stand smoothly.",
            isBodyweight = true,
            movementPattern = MovementPattern.SINGLE_LEG,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "standing-calf-raise",
            name = "Standing Calf Raise",
            muscleGroup = "Calves",
            instructions = "Stand tall with light support, rise onto the balls of the feet, pause, and lower the heels slowly.",
            isBodyweight = true,
            movementPattern = MovementPattern.SQUAT,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "hip-hinge-drill",
            name = "Hip Hinge Drill",
            muscleGroup = "Hamstrings",
            instructions = "Keep a long spine, push the hips back until the hamstrings tighten, then stand by bringing the hips forward.",
            isBodyweight = true,
            movementPattern = MovementPattern.HINGE,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "single-leg-glute-bridge",
            name = "Single-Leg Glute Bridge",
            muscleGroup = "Glutes",
            instructions = "Lie with one foot planted, keep the hips level, squeeze the planted-side glute to lift, and lower slowly.",
            isBodyweight = true,
            movementPattern = MovementPattern.HINGE,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "kneeling-push-up",
            name = "Kneeling Push-Up",
            muscleGroup = "Chest",
            instructions = "Form a straight line from knees to head, lower the chest between the hands, and press the floor away.",
            isBodyweight = true,
            movementPattern = MovementPattern.PUSH,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "push-up",
            name = "Push-Up",
            muscleGroup = "Chest",
            instructions = "Brace into a straight body line, lower the chest between the hands, and press back without letting the hips sag.",
            isBodyweight = true,
            movementPattern = MovementPattern.PUSH,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "pike-push-up",
            name = "Pike Push-Up",
            muscleGroup = "Shoulders",
            instructions = "Lift the hips into an inverted V, bend the elbows to lower the head between the hands, and press away.",
            isBodyweight = true,
            movementPattern = MovementPattern.PUSH,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "resistance-band-chest-press",
            name = "Resistance-Band Chest Press",
            muscleGroup = "Chest",
            instructions = "Anchor the band securely behind you, press the hands forward from chest height, and return with control.",
            isBodyweight = false,
            movementPattern = MovementPattern.PUSH,
            equipment = setOf(EquipmentOption.RESISTANCE_BANDS),
        ),
        StarterExerciseDefinition(
            key = "resistance-band-overhead-press",
            name = "Resistance-Band Overhead Press",
            muscleGroup = "Shoulders",
            instructions = "Stand securely on the band, brace the torso, press the handles overhead, and lower to the shoulders slowly.",
            isBodyweight = false,
            movementPattern = MovementPattern.PUSH,
            equipment = setOf(EquipmentOption.RESISTANCE_BANDS),
        ),
        StarterExerciseDefinition(
            key = "inverted-row",
            name = "Inverted Row",
            muscleGroup = "Back",
            instructions = "Hold a secure waist-high bar, keep the body straight, pull the chest toward the bar, and lower smoothly.",
            isBodyweight = true,
            movementPattern = MovementPattern.PULL,
            equipment = setOf(EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "assisted-pull-up",
            name = "Assisted Pull-Up",
            muscleGroup = "Back",
            instructions = "Use secure assistance, begin with long arms, pull the chest upward by driving the elbows down, and lower slowly.",
            isBodyweight = false,
            movementPattern = MovementPattern.PULL,
            equipment = setOf(EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "seated-cable-row",
            name = "Seated Cable Row",
            muscleGroup = "Back",
            instructions = "Sit tall with a stable torso, pull the handle toward the lower ribs, and return until the arms are long.",
            isBodyweight = false,
            movementPattern = MovementPattern.PULL,
            equipment = setOf(EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "dumbbell-lateral-raise",
            name = "Dumbbell Lateral Raise",
            muscleGroup = "Shoulders",
            instructions = "Hold light dumbbells, raise the arms out to a comfortable shoulder height, and lower without swinging.",
            isBodyweight = false,
            movementPattern = MovementPattern.PUSH,
            equipment = setOf(EquipmentOption.DUMBBELLS, EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "dumbbell-biceps-curl",
            name = "Dumbbell Biceps Curl",
            muscleGroup = "Arms",
            instructions = "Keep the upper arms quiet, curl the dumbbells without leaning back, and lower until the elbows are straight.",
            isBodyweight = false,
            movementPattern = MovementPattern.PULL,
            equipment = setOf(EquipmentOption.DUMBBELLS, EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "dumbbell-triceps-extension",
            name = "Dumbbell Triceps Extension",
            muscleGroup = "Arms",
            instructions = "Hold one dumbbell securely overhead, bend the elbows to lower it behind the head, and extend without arching.",
            isBodyweight = false,
            movementPattern = MovementPattern.PUSH,
            equipment = setOf(EquipmentOption.DUMBBELLS, EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "resistance-band-pull-apart",
            name = "Resistance-Band Pull-Apart",
            muscleGroup = "Upper back",
            instructions = "Hold the band at chest height, pull the hands apart while keeping the ribs down, and return slowly.",
            isBodyweight = false,
            movementPattern = MovementPattern.PULL,
            equipment = setOf(EquipmentOption.RESISTANCE_BANDS),
        ),
        StarterExerciseDefinition(
            key = "face-pull",
            name = "Face Pull",
            muscleGroup = "Upper back",
            instructions = "Set a rope near face height, pull toward the face with elbows wide, pause, and return under control.",
            isBodyweight = false,
            movementPattern = MovementPattern.PULL,
            equipment = setOf(EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "forearm-plank",
            name = "Forearm Plank",
            muscleGroup = "Core",
            instructions = "Place elbows below the shoulders, make a straight line from head to heels, and breathe while holding steady.",
            isBodyweight = true,
            movementPattern = MovementPattern.CORE,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "side-plank",
            name = "Side Plank",
            muscleGroup = "Core",
            instructions = "Stack the shoulder over the elbow, lift the hips into a long line, and keep breathing without rotating.",
            isBodyweight = true,
            movementPattern = MovementPattern.CORE,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "bear-hold",
            name = "Bear Hold",
            muscleGroup = "Core",
            instructions = "From hands and knees, brace gently and hover the knees just above the floor while keeping the back steady.",
            isBodyweight = true,
            movementPattern = MovementPattern.CORE,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "mountain-climber",
            name = "Mountain Climber",
            muscleGroup = "Core",
            instructions = "Hold a strong high plank and alternate bringing one knee forward while keeping the hips controlled.",
            isBodyweight = true,
            movementPattern = MovementPattern.CORE,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
        ),
        StarterExerciseDefinition(
            key = "dumbbell-deadlift",
            name = "Dumbbell Deadlift",
            muscleGroup = "Glutes",
            instructions = "Stand over the dumbbells, brace with a long spine, push through the floor to stand, and lower with the hips back.",
            isBodyweight = false,
            movementPattern = MovementPattern.HINGE,
            equipment = setOf(EquipmentOption.DUMBBELLS, EquipmentOption.FULL_GYM),
        ),
        StarterExerciseDefinition(
            key = "dumbbell-reverse-lunge",
            name = "Dumbbell Reverse Lunge",
            muscleGroup = "Legs",
            instructions = "Hold dumbbells at the sides, step one foot back, lower with control, and drive through the front foot to stand.",
            isBodyweight = false,
            movementPattern = MovementPattern.SINGLE_LEG,
            equipment = setOf(EquipmentOption.DUMBBELLS, EquipmentOption.FULL_GYM),
        ),
    )

    val byKey: Map<String, StarterExerciseDefinition> = exercises.associateBy { it.key }
}

class StarterWeekPlanner(
    private val catalog: List<StarterExerciseDefinition> = StarterExerciseCatalog.exercises,
) {
    fun createDraft(input: StarterPlanInput): Result<StarterWeekDraft> = runCatching {
        validate(input)
        val normalizedEquipment = input.equipment + EquipmentOption.BODYWEIGHT
        val available = catalog.filter { definition ->
            definition.key !in input.avoidedExerciseKeys &&
                definition.equipment.any { it in normalizedEquipment }
        }
        val exerciseCount = when (input.sessionMinutes) {
            15 -> 3
            30 -> 5
            45 -> 6
            60 -> 7
            else -> error("Choose a supported session length.")
        }
        require(available.size >= exerciseCount) {
            "Choose fewer exercises to avoid or add equipment so a balanced session can be created."
        }
        val sets = when (input.experienceLevel) {
            ExperienceLevel.BEGINNER -> 2
            ExperienceLevel.INTERMEDIATE -> 3
            ExperienceLevel.EXPERIENCED -> if (input.sessionMinutes >= 45) 4 else 3
        }
        val repetitions = when (input.goal) {
            JourneyGoal.STRENGTH -> "5-8"
            JourneyGoal.MUSCLE_GAIN -> "8-12"
            JourneyGoal.FAT_LOSS -> "10-15"
            JourneyGoal.CONSISTENCY -> "8-10"
            JourneyGoal.GENERAL_FITNESS -> "8-12"
        }
        val rotations = listOf(
            listOf(
                MovementPattern.SQUAT,
                MovementPattern.PUSH,
                MovementPattern.PULL,
                MovementPattern.HINGE,
                MovementPattern.CORE,
                MovementPattern.SINGLE_LEG,
                MovementPattern.PUSH,
            ),
            listOf(
                MovementPattern.HINGE,
                MovementPattern.PULL,
                MovementPattern.SINGLE_LEG,
                MovementPattern.PUSH,
                MovementPattern.CORE,
                MovementPattern.SQUAT,
                MovementPattern.PULL,
            ),
            listOf(
                MovementPattern.SINGLE_LEG,
                MovementPattern.PUSH,
                MovementPattern.PULL,
                MovementPattern.SQUAT,
                MovementPattern.CORE,
                MovementPattern.HINGE,
                MovementPattern.PUSH,
            ),
            listOf(
                MovementPattern.SQUAT,
                MovementPattern.PULL,
                MovementPattern.PUSH,
                MovementPattern.HINGE,
                MovementPattern.CORE,
                MovementPattern.SINGLE_LEG,
                MovementPattern.PULL,
            ),
        )
        val days = input.preferredDays.sortedBy(DayOfWeek::getValue).mapIndexed { dayIndex, day ->
            val selected = selectExercises(
                available = available,
                patterns = rotations[dayIndex % rotations.size],
                count = exerciseCount,
                rotation = dayIndex,
            )
            StarterPlanDay(
                dayOfWeek = day,
                templateName = "Foundation ${('A'.code + dayIndex).toChar()}",
                exercises = selected.map { definition ->
                    StarterPlanExercise(
                        key = definition.key,
                        name = definition.name,
                        muscleGroup = definition.muscleGroup,
                        instructions = definition.instructions,
                        isBodyweight = definition.isBodyweight,
                        targetSets = sets,
                        targetReps = repetitions,
                    )
                },
            )
        }
        StarterWeekDraft(
            name = "${input.goal.label} starter week",
            rationale = buildRationale(input, days.size),
            days = days,
        )
    }

    private fun validate(input: StarterPlanInput) {
        require(input.preferredDays.isNotEmpty()) { "Choose at least one training day." }
        require(input.preferredDays.size <= 4) { "Choose no more than four training days for a starter week." }
        require(input.sessionMinutes in setOf(15, 30, 45, 60)) { "Choose a supported session length." }
        require(input.avoidedExerciseKeys.all(StarterExerciseCatalog.byKey::containsKey)) {
            "One or more avoided exercises are unavailable."
        }
    }

    private fun selectExercises(
        available: List<StarterExerciseDefinition>,
        patterns: List<MovementPattern>,
        count: Int,
        rotation: Int,
    ): List<StarterExerciseDefinition> {
        val selected = mutableListOf<StarterExerciseDefinition>()
        patterns.take(count).forEachIndexed { slot, pattern ->
            val candidates = available.filter { it.movementPattern == pattern && it !in selected }
            if (candidates.isNotEmpty()) {
                selected += candidates[(rotation + slot) % candidates.size]
            }
        }
        available.filterNot(selected::contains).forEach { candidate ->
            if (selected.size < count) selected += candidate
        }
        return selected.take(count)
    }

    private fun buildRationale(input: StarterPlanInput, dayCount: Int): String {
        val dayLabel = if (dayCount == 1) "one day" else "$dayCount days"
        return "A ${input.experienceLevel.label.lowercase()} plan for ${input.goal.label.lowercase()}, " +
            "using $dayLabel of about ${input.sessionMinutes} minutes. Exercises are limited to the equipment you selected."
    }
}
