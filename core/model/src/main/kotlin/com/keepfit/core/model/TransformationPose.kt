package com.keepfit.core.model

enum class TransformationPoseGroup(val label: String) {
    BASIC("Basic"),
    STANDARD("Standard"),
    FLEXED("Flexed"),
    DETAILED("Detailed"),
}

enum class TransformationPose(
    val key: String,
    val label: String,
    val group: TransformationPoseGroup,
    val guidance: String,
) {
    FRONT_RELAXED(
        key = "front_relaxed",
        label = "Front relaxed",
        group = TransformationPoseGroup.BASIC,
        guidance = "Face the camera, stand naturally, and keep your arms relaxed at your sides.",
    ),
    RIGHT_SIDE_RELAXED(
        key = "right_side_relaxed",
        label = "Right side relaxed",
        group = TransformationPoseGroup.BASIC,
        guidance = "Turn your right side to the camera and look straight ahead without flexing.",
    ),
    BACK_RELAXED(
        key = "back_relaxed",
        label = "Back relaxed",
        group = TransformationPoseGroup.BASIC,
        guidance = "Face away from the camera with level shoulders and arms resting naturally.",
    ),
    LEFT_SIDE_RELAXED(
        key = "left_side_relaxed",
        label = "Left side relaxed",
        group = TransformationPoseGroup.BASIC,
        guidance = "Turn your left side to the camera and look straight ahead without flexing.",
    ),
    FRONT_HANDS_ON_HIPS(
        key = "front_hands_on_hips",
        label = "Front - hands on hips",
        group = TransformationPoseGroup.STANDARD,
        guidance = "Face forward, place both hands lightly on your hips, and keep your stance natural.",
    ),
    FRONT_DOUBLE_BICEPS(
        key = "front_double_biceps",
        label = "Front double biceps",
        group = TransformationPoseGroup.FLEXED,
        guidance = "Face forward and raise both elbows to shoulder height before flexing comfortably.",
    ),
    BACK_DOUBLE_BICEPS(
        key = "back_double_biceps",
        label = "Back double biceps",
        group = TransformationPoseGroup.FLEXED,
        guidance = "Face away and raise both elbows to shoulder height before flexing comfortably.",
    ),
    RIGHT_SIDE_FLEXED(
        key = "right_side_flexed",
        label = "Right side flexed",
        group = TransformationPoseGroup.FLEXED,
        guidance = "Show your right side, bring your hands together gently, and flex without twisting.",
    ),
    LEFT_SIDE_FLEXED(
        key = "left_side_flexed",
        label = "Left side flexed",
        group = TransformationPoseGroup.FLEXED,
        guidance = "Show your left side, bring your hands together gently, and flex without twisting.",
    ),
    ABS_AND_CORE(
        key = "abs_and_core",
        label = "Abs and core",
        group = TransformationPoseGroup.FLEXED,
        guidance = "Face forward, place your hands behind your head, and tighten your core comfortably.",
    ),
    CHEST_FOCUSED(
        key = "chest_focused",
        label = "Chest focused",
        group = TransformationPoseGroup.DETAILED,
        guidance = "Face forward, clasp your hands low, and gently contract your chest.",
    ),
    BACK_LAT_SPREAD(
        key = "back_lat_spread",
        label = "Back lat spread",
        group = TransformationPoseGroup.DETAILED,
        guidance = "Face away, rest your hands at your waist, and widen your back without shrugging.",
    ),
    SIDE_CHEST_RIGHT(
        key = "side_chest_right",
        label = "Side chest - right",
        group = TransformationPoseGroup.DETAILED,
        guidance = "Show your right side, clasp your hands, and lift your chest without over-rotating.",
    ),
    SIDE_CHEST_LEFT(
        key = "side_chest_left",
        label = "Side chest - left",
        group = TransformationPoseGroup.DETAILED,
        guidance = "Show your left side, clasp your hands, and lift your chest without over-rotating.",
    ),
    LEGS_FOCUSED(
        key = "legs_focused",
        label = "Legs focused",
        group = TransformationPoseGroup.DETAILED,
        guidance = "Frame from waist to feet, stand evenly, and tighten your legs comfortably.",
    );

    val isDefault: Boolean
        get() = group == TransformationPoseGroup.BASIC

    companion object {
        val defaultPoses: List<TransformationPose> = entries.filter(TransformationPose::isDefault)

        fun fromKey(key: String): TransformationPose? = entries.firstOrNull { it.key == key }
    }
}
