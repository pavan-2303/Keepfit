package com.keepfit.core.media

data class GuidancePoint(
    val x: Float,
    val y: Float,
)

data class GuidancePose(
    val head: GuidancePoint,
    val shoulder: GuidancePoint,
    val leftElbow: GuidancePoint,
    val leftHand: GuidancePoint,
    val rightElbow: GuidancePoint,
    val rightHand: GuidancePoint,
    val hip: GuidancePoint,
    val leftKnee: GuidancePoint,
    val leftAnkle: GuidancePoint,
    val rightKnee: GuidancePoint,
    val rightAnkle: GuidancePoint,
) {
    val joints: List<GuidancePoint>
        get() = listOf(
            head,
            shoulder,
            leftElbow,
            leftHand,
            rightElbow,
            rightHand,
            hip,
            leftKnee,
            leftAnkle,
            rightKnee,
            rightAnkle,
        )

    fun interpolateTo(other: GuidancePose, progress: Float): GuidancePose {
        val amount = progress.coerceIn(0f, 1f)
        return GuidancePose(
            head = head.interpolateTo(other.head, amount),
            shoulder = shoulder.interpolateTo(other.shoulder, amount),
            leftElbow = leftElbow.interpolateTo(other.leftElbow, amount),
            leftHand = leftHand.interpolateTo(other.leftHand, amount),
            rightElbow = rightElbow.interpolateTo(other.rightElbow, amount),
            rightHand = rightHand.interpolateTo(other.rightHand, amount),
            hip = hip.interpolateTo(other.hip, amount),
            leftKnee = leftKnee.interpolateTo(other.leftKnee, amount),
            leftAnkle = leftAnkle.interpolateTo(other.leftAnkle, amount),
            rightKnee = rightKnee.interpolateTo(other.rightKnee, amount),
            rightAnkle = rightAnkle.interpolateTo(other.rightAnkle, amount),
        )
    }
}

private fun GuidancePoint.interpolateTo(other: GuidancePoint, amount: Float) = GuidancePoint(
    x = x + (other.x - x) * amount,
    y = y + (other.y - y) * amount,
)

data class GuidancePosePair(
    val start: GuidancePose,
    val finish: GuidancePose,
)

enum class GuidanceMotion {
    BENCH_PRESS,
    PUSH_UP,
    INCLINE_PUSH_UP,
    OVERHEAD_PRESS,
    LATERAL_RAISE,
    BENT_ROW,
    SEATED_ROW,
    LAT_PULLDOWN,
    PULL_UP,
    SQUAT,
    DEADLIFT,
    LUNGE,
    STEP_UP,
    GLUTE_BRIDGE,
    LEG_PRESS,
    CALF_RAISE,
    BICEPS_CURL,
    TRICEPS_PUSHDOWN,
    OVERHEAD_TRICEPS_EXTENSION,
    PLANK,
    DEAD_BUG,
    MOUNTAIN_CLIMBER,
}

enum class GuidanceEquipment {
    NONE,
    BARBELL,
    DUMBBELLS,
    BENCH,
    CABLE,
    PULL_UP_BAR,
    STEP,
    LEG_PRESS,
    WEIGHT_PLATE,
}

enum class GuidanceTarget {
    CHEST,
    SHOULDERS,
    UPPER_BACK,
    LATS,
    GLUTES,
    QUADS,
    POSTERIOR_CHAIN,
    CALVES,
    BICEPS,
    TRICEPS,
    CORE,
    FULL_BODY,
}

data class GuidanceRights(
    val artworkFamily: String,
    val creator: String,
    val rightsBasis: String,
    val reviewedOn: String,
)

data class ExerciseGuidance(
    val exerciseId: String,
    val sourceId: String,
    val exerciseName: String,
    val motion: GuidanceMotion,
    val equipment: GuidanceEquipment,
    val target: GuidanceTarget,
    val primaryCue: String,
    val safetyCue: String,
    val rights: GuidanceRights,
) {
    val startPose: GuidancePose get() = GuidancePoseLibrary.forMotion(motion).start
    val finishPose: GuidancePose get() = GuidancePoseLibrary.forMotion(motion).finish
}

object CoreExerciseGuidanceCatalog {
    private val originalRights = GuidanceRights(
        artworkFamily = "Keepfit movement figures v1",
        creator = "Keepfit",
        rightsBasis = "Original code-native artwork",
        reviewedOn = "2026-09-15",
    )

    val entries: List<ExerciseGuidance> = listOf(
        guide("5ca9f46f-1ae9-5ff8-9627-32cd73c56a13", "0025", "Barbell bench press", GuidanceMotion.BENCH_PRESS, GuidanceEquipment.BARBELL, GuidanceTarget.CHEST, "Plant your feet and keep your shoulder blades supported.", "Use a spotter or safety arms when lifting near your limit."),
        guide("b4a97e4a-61ec-5c4d-81b7-1a043aa7a599", "0289", "Dumbbell bench press", GuidanceMotion.BENCH_PRESS, GuidanceEquipment.DUMBBELLS, GuidanceTarget.CHEST, "Lower both dumbbells with control, then press over the shoulders.", "Choose a load you can bring safely into and out of position."),
        guide("76a597e5-4c1e-5d5c-8119-a0bde7b77981", "0662", "Push-up", GuidanceMotion.PUSH_UP, GuidanceEquipment.NONE, GuidanceTarget.CHEST, "Keep one long line from head to heels as the chest lowers.", "Use an incline if you cannot keep the trunk steady."),
        guide("b9430a44-8fba-5b3e-871e-990ae9519cc9", "0493", "Incline push-up", GuidanceMotion.INCLINE_PUSH_UP, GuidanceEquipment.BENCH, GuidanceTarget.CHEST, "Brace the body and lower the chest toward the stable surface.", "Confirm the surface cannot slide or tip before starting."),
        guide("d33d986b-1046-52d8-89c4-b622fdc8dfc5", "0405", "Dumbbell seated shoulder press", GuidanceMotion.OVERHEAD_PRESS, GuidanceEquipment.DUMBBELLS, GuidanceTarget.SHOULDERS, "Keep the ribs stacked while pressing the weights overhead.", "Stop the range before the lower back starts to arch."),
        guide("75099793-4b69-527a-8cdc-d2114d298725", "0334", "Dumbbell lateral raise", GuidanceMotion.LATERAL_RAISE, GuidanceEquipment.DUMBBELLS, GuidanceTarget.SHOULDERS, "Lead with the elbows and lift only to a comfortable height.", "Use a lighter load if you need momentum to raise it."),
        guide("044e1e2b-38be-5e18-b318-842e0940a4fa", "0027", "Barbell bent over row", GuidanceMotion.BENT_ROW, GuidanceEquipment.BARBELL, GuidanceTarget.UPPER_BACK, "Hold a long spine and pull the bar toward the lower ribs.", "Reduce the load if the torso cannot stay still."),
        guide("9eedbccb-509e-569b-ba9f-266e90021893", "0861", "Cable seated row", GuidanceMotion.SEATED_ROW, GuidanceEquipment.CABLE, GuidanceTarget.UPPER_BACK, "Sit tall and drive the elbows back without leaning away.", "Return the handle slowly without rounding forward."),
        guide("9949981e-fc2e-5acc-bc69-8bcd53db97ef", "2330", "Cable lat pulldown full range of motion", GuidanceMotion.LAT_PULLDOWN, GuidanceEquipment.CABLE, GuidanceTarget.LATS, "Pull the elbows down while keeping the chest comfortably tall.", "Do not pull the bar behind the neck."),
        guide("bb222adb-b2eb-549f-9f57-9fdcd8d68a1f", "0652", "Pull-up", GuidanceMotion.PULL_UP, GuidanceEquipment.PULL_UP_BAR, GuidanceTarget.LATS, "Start from controlled shoulders and drive the elbows toward the ribs.", "Use secure assistance rather than swinging for extra height."),
        guide("46632848-5ff2-54d4-887a-e23d56c8d195", "0043", "Barbell full squat", GuidanceMotion.SQUAT, GuidanceEquipment.BARBELL, GuidanceTarget.GLUTES, "Keep the whole foot planted as the hips descend between the legs.", "Use rack safeties and a depth you can control."),
        guide("6109caee-8f5a-56d8-9fbc-63608a1237a7", "1760", "Dumbbell goblet squat", GuidanceMotion.SQUAT, GuidanceEquipment.DUMBBELLS, GuidanceTarget.QUADS, "Hold the weight close and keep the whole foot grounded.", "Choose a depth that keeps the knees and back comfortable."),
        guide("902c8ae3-c298-5a9c-b5a3-3237d2e9906a", "0032", "Barbell deadlift", GuidanceMotion.DEADLIFT, GuidanceEquipment.BARBELL, GuidanceTarget.POSTERIOR_CHAIN, "Brace before lifting and keep the bar close to the legs.", "Reset the position if the back shape changes under load."),
        guide("c2c1d965-ed7a-5fa4-8d76-436a0cc84442", "1459", "Dumbbell romanian deadlift", GuidanceMotion.DEADLIFT, GuidanceEquipment.DUMBBELLS, GuidanceTarget.POSTERIOR_CHAIN, "Push the hips back while the weights stay close to the legs.", "Stop when the hamstrings limit the hinge; do not chase the floor."),
        guide("01aac1d3-12c0-526b-9a16-3978c10c2bb0", "1460", "Walking lunge", GuidanceMotion.LUNGE, GuidanceEquipment.NONE, GuidanceTarget.GLUTES, "Take a stable step and lower mostly straight down.", "Shorten the range or use support if balance is uncertain."),
        guide("c9e43998-9b82-5501-9ad3-e30d0a24a230", "0431", "Dumbbell step-up", GuidanceMotion.STEP_UP, GuidanceEquipment.STEP, GuidanceTarget.GLUTES, "Place the whole foot on the step and drive through that leg.", "Use a lower stable step if the knee or balance feels strained."),
        guide("f635ff87-e394-5404-9f0b-d054b584cbf8", "3013", "Low glute bridge on floor", GuidanceMotion.GLUTE_BRIDGE, GuidanceEquipment.NONE, GuidanceTarget.GLUTES, "Squeeze the glutes to lift the hips without flaring the ribs.", "Stop before the lower back takes over the movement."),
        guide("9cfb3bcf-6367-55d6-89ac-2c41150dd7ef", "0760", "Smith leg press", GuidanceMotion.LEG_PRESS, GuidanceEquipment.LEG_PRESS, GuidanceTarget.GLUTES, "Keep the hips supported and press through the whole foot.", "Do not lower so far that the pelvis rolls off the pad."),
        guide("9e04873f-54e6-52b3-b729-2c1d48139a3b", "0417", "Dumbbell standing calf raise", GuidanceMotion.CALF_RAISE, GuidanceEquipment.DUMBBELLS, GuidanceTarget.CALVES, "Rise smoothly, pause at the top, and lower under control.", "Use light support if balance limits the movement."),
        guide("29314612-9bbb-5c61-b04b-7b0457ea27bd", "0416", "Dumbbell standing biceps curl", GuidanceMotion.BICEPS_CURL, GuidanceEquipment.DUMBBELLS, GuidanceTarget.BICEPS, "Keep the upper arms quiet while the forearms curl upward.", "Reduce the load if the torso needs to swing."),
        guide("9041ce96-f296-5934-a769-d7688b1bad28", "0201", "Cable pushdown", GuidanceMotion.TRICEPS_PUSHDOWN, GuidanceEquipment.CABLE, GuidanceTarget.TRICEPS, "Pin the upper arms by the sides and straighten the elbows.", "Keep the cable attachment and pin secure."),
        guide("6e579c24-9ed1-5417-a4b3-2c9700415f8a", "0430", "Dumbbell standing triceps extension", GuidanceMotion.OVERHEAD_TRICEPS_EXTENSION, GuidanceEquipment.DUMBBELLS, GuidanceTarget.TRICEPS, "Keep the elbows comfortably forward as the weight moves overhead.", "Use a load you can control behind the head."),
        guide("7c44fed5-775b-5b0a-9ee5-56fbeb2dd5b6", "2135", "Weighted front plank", GuidanceMotion.PLANK, GuidanceEquipment.WEIGHT_PLATE, GuidanceTarget.CORE, "Brace into a straight line and continue breathing quietly.", "Use an unweighted plank first and stop if the back sags."),
        guide("f9c7869a-4e52-5650-a184-a457b9b0d6d7", "0276", "Dead bug", GuidanceMotion.DEAD_BUG, GuidanceEquipment.NONE, GuidanceTarget.CORE, "Keep the trunk still while opposite limbs reach away.", "Shorten the reach if the lower back lifts from the floor."),
        guide("1e544139-12c7-5bef-9079-f7c1d5f03c85", "0630", "Mountain climber", GuidanceMotion.MOUNTAIN_CLIMBER, GuidanceEquipment.NONE, GuidanceTarget.FULL_BODY, "Hold a strong plank while bringing one knee forward at a time.", "Slow down before the hips start bouncing or sagging."),
    )

    private val byExerciseId = entries.associateBy(ExerciseGuidance::exerciseId)

    fun find(exerciseId: String): ExerciseGuidance? = byExerciseId[exerciseId]

    private fun guide(
        exerciseId: String,
        sourceId: String,
        exerciseName: String,
        motion: GuidanceMotion,
        equipment: GuidanceEquipment,
        target: GuidanceTarget,
        primaryCue: String,
        safetyCue: String,
    ) = ExerciseGuidance(
        exerciseId = exerciseId,
        sourceId = sourceId,
        exerciseName = exerciseName,
        motion = motion,
        equipment = equipment,
        target = target,
        primaryCue = primaryCue,
        safetyCue = safetyCue,
        rights = originalRights,
    )
}

object GuidancePoseLibrary {
    private val standing = GuidancePose(
        head = point(.50f, .12f),
        shoulder = point(.50f, .28f),
        leftElbow = point(.42f, .39f),
        leftHand = point(.44f, .52f),
        rightElbow = point(.58f, .39f),
        rightHand = point(.56f, .52f),
        hip = point(.50f, .55f),
        leftKnee = point(.44f, .73f),
        leftAnkle = point(.42f, .93f),
        rightKnee = point(.56f, .73f),
        rightAnkle = point(.58f, .93f),
    )

    private val pairs: Map<GuidanceMotion, GuidancePosePair> = mapOf(
        GuidanceMotion.BENCH_PRESS to pair(
            lying(
                shoulder = point(.34f, .53f), hip = point(.61f, .58f),
                leftElbow = point(.33f, .35f), leftHand = point(.45f, .32f),
                rightElbow = point(.48f, .36f), rightHand = point(.55f, .32f),
            ),
            lying(
                shoulder = point(.34f, .53f), hip = point(.61f, .58f),
                leftElbow = point(.40f, .30f), leftHand = point(.45f, .15f),
                rightElbow = point(.50f, .30f), rightHand = point(.55f, .15f),
            ),
        ),
        GuidanceMotion.PUSH_UP to pair(plank(.58f), plank(.70f)),
        GuidanceMotion.INCLINE_PUSH_UP to pair(inclinePlank(.46f), inclinePlank(.57f)),
        GuidanceMotion.OVERHEAD_PRESS to pair(
            standing.copy(leftElbow = point(.38f, .31f), leftHand = point(.43f, .25f), rightElbow = point(.62f, .31f), rightHand = point(.57f, .25f)),
            standing.copy(leftElbow = point(.43f, .18f), leftHand = point(.45f, .05f), rightElbow = point(.57f, .18f), rightHand = point(.55f, .05f)),
        ),
        GuidanceMotion.LATERAL_RAISE to pair(
            standing,
            standing.copy(leftElbow = point(.31f, .29f), leftHand = point(.16f, .30f), rightElbow = point(.69f, .29f), rightHand = point(.84f, .30f)),
        ),
        GuidanceMotion.BENT_ROW to pair(hinge(point(.54f, .53f)), hinge(point(.45f, .37f))),
        GuidanceMotion.SEATED_ROW to pair(seatedRow(.76f), seatedRow(.54f)),
        GuidanceMotion.LAT_PULLDOWN to pair(overheadPull(.10f), overheadPull(.30f)),
        GuidanceMotion.PULL_UP to pair(hanging(.36f), hanging(.20f)),
        GuidanceMotion.SQUAT to pair(
            standing,
            standing.copy(head = point(.50f, .30f), shoulder = point(.50f, .43f), hip = point(.50f, .65f), leftKnee = point(.38f, .72f), leftAnkle = point(.32f, .92f), rightKnee = point(.62f, .72f), rightAnkle = point(.68f, .92f)),
        ),
        GuidanceMotion.DEADLIFT to pair(hinge(point(.48f, .72f)), standing),
        GuidanceMotion.LUNGE to pair(
            standing,
            standing.copy(head = point(.50f, .20f), shoulder = point(.50f, .34f), hip = point(.50f, .58f), leftKnee = point(.34f, .70f), leftAnkle = point(.24f, .91f), rightKnee = point(.65f, .77f), rightAnkle = point(.77f, .91f)),
        ),
        GuidanceMotion.STEP_UP to pair(stepUp(.76f), stepUp(.54f)),
        GuidanceMotion.GLUTE_BRIDGE to pair(bridge(.68f), bridge(.50f)),
        GuidanceMotion.LEG_PRESS to pair(legPress(.60f), legPress(.35f)),
        GuidanceMotion.CALF_RAISE to pair(standing, standing.copy(leftAnkle = point(.42f, .87f), rightAnkle = point(.58f, .87f), head = point(.50f, .06f), shoulder = point(.50f, .22f), hip = point(.50f, .49f), leftKnee = point(.44f, .67f), rightKnee = point(.56f, .67f))),
        GuidanceMotion.BICEPS_CURL to pair(
            standing,
            standing.copy(leftElbow = point(.42f, .39f), leftHand = point(.37f, .28f), rightElbow = point(.58f, .39f), rightHand = point(.63f, .28f)),
        ),
        GuidanceMotion.TRICEPS_PUSHDOWN to pair(pushdown(.33f), pushdown(.54f)),
        GuidanceMotion.OVERHEAD_TRICEPS_EXTENSION to pair(overheadExtension(.28f), overheadExtension(.08f)),
        GuidanceMotion.PLANK to pair(plank(.58f), plank(.56f)),
        GuidanceMotion.DEAD_BUG to pair(deadBug(false), deadBug(true)),
        GuidanceMotion.MOUNTAIN_CLIMBER to pair(mountainClimber(false), mountainClimber(true)),
    )

    fun forMotion(motion: GuidanceMotion): GuidancePosePair = pairs.getValue(motion)

    private fun pair(start: GuidancePose, finish: GuidancePose) = GuidancePosePair(start, finish)

    private fun point(x: Float, y: Float) = GuidancePoint(x, y)

    private fun lying(
        shoulder: GuidancePoint,
        hip: GuidancePoint,
        leftElbow: GuidancePoint,
        leftHand: GuidancePoint,
        rightElbow: GuidancePoint,
        rightHand: GuidancePoint,
    ) = GuidancePose(
        head = point(.20f, .49f), shoulder = shoulder,
        leftElbow = leftElbow, leftHand = leftHand,
        rightElbow = rightElbow, rightHand = rightHand,
        hip = hip,
        leftKnee = point(.74f, .70f), leftAnkle = point(.86f, .88f),
        rightKnee = point(.68f, .71f), rightAnkle = point(.75f, .90f),
    )

    private fun plank(bodyY: Float) = GuidancePose(
        head = point(.16f, bodyY - .09f), shoulder = point(.28f, bodyY),
        leftElbow = point(.36f, bodyY + .12f), leftHand = point(.25f, .82f),
        rightElbow = point(.39f, bodyY + .10f), rightHand = point(.38f, .82f),
        hip = point(.58f, bodyY + .02f),
        leftKnee = point(.75f, bodyY + .05f), leftAnkle = point(.90f, .82f),
        rightKnee = point(.72f, bodyY + .03f), rightAnkle = point(.83f, .82f),
    )

    private fun inclinePlank(bodyY: Float) = GuidancePose(
        head = point(.28f, bodyY - .15f), shoulder = point(.37f, bodyY - .07f),
        leftElbow = point(.30f, bodyY + .07f), leftHand = point(.20f, .73f),
        rightElbow = point(.39f, bodyY + .08f), rightHand = point(.32f, .73f),
        hip = point(.59f, bodyY + .07f),
        leftKnee = point(.74f, bodyY + .14f), leftAnkle = point(.88f, .88f),
        rightKnee = point(.70f, bodyY + .12f), rightAnkle = point(.80f, .88f),
    )

    private fun hinge(hand: GuidancePoint) = standing.copy(
        head = point(.28f, .34f), shoulder = point(.39f, .39f),
        leftElbow = point(.44f, .49f), leftHand = hand,
        rightElbow = point(.48f, .49f), rightHand = hand.copy(x = hand.x + .08f),
        hip = point(.58f, .53f), leftKnee = point(.60f, .72f), leftAnkle = point(.57f, .93f),
        rightKnee = point(.69f, .73f), rightAnkle = point(.72f, .93f),
    )

    private fun seatedRow(handX: Float) = GuidancePose(
        head = point(.34f, .22f), shoulder = point(.38f, .36f),
        leftElbow = point((handX + .38f) / 2f, .46f), leftHand = point(handX, .47f),
        rightElbow = point((handX + .42f) / 2f, .40f), rightHand = point(handX, .42f),
        hip = point(.42f, .62f),
        leftKnee = point(.64f, .70f), leftAnkle = point(.78f, .90f),
        rightKnee = point(.59f, .74f), rightAnkle = point(.70f, .91f),
    )

    private fun overheadPull(handY: Float) = standing.copy(
        leftElbow = point(.38f, (handY + .30f) / 2f), leftHand = point(.41f, handY),
        rightElbow = point(.62f, (handY + .30f) / 2f), rightHand = point(.59f, handY),
    )

    private fun hanging(headY: Float) = GuidancePose(
        head = point(.50f, headY), shoulder = point(.50f, headY + .13f),
        leftElbow = point(.39f, .26f), leftHand = point(.31f, .08f),
        rightElbow = point(.61f, .26f), rightHand = point(.69f, .08f),
        hip = point(.50f, headY + .40f),
        leftKnee = point(.46f, headY + .57f), leftAnkle = point(.43f, .94f),
        rightKnee = point(.54f, headY + .57f), rightAnkle = point(.57f, .94f),
    )

    private fun stepUp(kneeY: Float) = standing.copy(
        leftKnee = point(.37f, kneeY), leftAnkle = point(.28f, .72f),
        rightKnee = point(.57f, .73f), rightAnkle = point(.60f, .92f),
        head = if (kneeY < .6f) point(.50f, .07f) else standing.head,
    )

    private fun bridge(hipY: Float) = GuidancePose(
        head = point(.18f, .63f), shoulder = point(.29f, .68f),
        leftElbow = point(.26f, .77f), leftHand = point(.19f, .84f),
        rightElbow = point(.37f, .77f), rightHand = point(.31f, .84f),
        hip = point(.53f, hipY),
        leftKnee = point(.70f, .58f), leftAnkle = point(.82f, .85f),
        rightKnee = point(.66f, .61f), rightAnkle = point(.74f, .85f),
    )

    private fun legPress(kneeX: Float) = GuidancePose(
        head = point(.22f, .32f), shoulder = point(.29f, .43f),
        leftElbow = point(.24f, .56f), leftHand = point(.34f, .62f),
        rightElbow = point(.32f, .55f), rightHand = point(.40f, .61f),
        hip = point(.40f, .64f),
        leftKnee = point(kneeX, .54f), leftAnkle = point(.80f, .34f),
        rightKnee = point(kneeX + .04f, .62f), rightAnkle = point(.82f, .43f),
    )

    private fun pushdown(handY: Float) = standing.copy(
        leftElbow = point(.42f, .37f), leftHand = point(.42f, handY),
        rightElbow = point(.58f, .37f), rightHand = point(.58f, handY),
    )

    private fun overheadExtension(handY: Float) = standing.copy(
        leftElbow = point(.44f, .20f), leftHand = point(.50f, handY),
        rightElbow = point(.56f, .20f), rightHand = point(.50f, handY),
    )

    private fun deadBug(alternate: Boolean) = GuidancePose(
        head = point(.18f, .67f), shoulder = point(.30f, .66f),
        leftElbow = point(if (alternate) .42f else .31f, if (alternate) .41f else .45f),
        leftHand = point(if (alternate) .55f else .32f, if (alternate) .20f else .25f),
        rightElbow = point(if (alternate) .25f else .42f, if (alternate) .45f else .41f),
        rightHand = point(if (alternate) .20f else .55f, if (alternate) .25f else .20f),
        hip = point(.50f, .69f),
        leftKnee = point(if (alternate) .65f else .57f, if (alternate) .82f else .52f),
        leftAnkle = point(if (alternate) .82f else .69f, if (alternate) .85f else .52f),
        rightKnee = point(if (alternate) .57f else .65f, if (alternate) .52f else .82f),
        rightAnkle = point(if (alternate) .69f else .82f, if (alternate) .52f else .85f),
    )

    private fun mountainClimber(alternate: Boolean): GuidancePose {
        val base = plank(.56f)
        return if (alternate) {
            base.copy(leftKnee = point(.49f, .71f), leftAnkle = point(.63f, .81f))
        } else {
            base.copy(rightKnee = point(.49f, .71f), rightAnkle = point(.63f, .81f))
        }
    }
}
