# Keepfit Phase 1A Workout Tracker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a complete offline gym-log workflow from exercise creation through scheduled workout completion and derived history.

**Architecture:** Add `feature:workouts` for domain repositories, ViewModels, and Compose screens. Add `core:media` for private exercise media imports. Extend Room schema to version `2` with a tested explicit migration.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, coroutines, `Flow`, Android Storage Access Framework, Android vibration APIs.

---

### Task 1: Workout Schema and Migration

**Files:**
- Modify: `core/database/src/main/kotlin/com/keepfit/core/database/KeepfitDatabase.kt`
- Create: `core/database/src/main/kotlin/com/keepfit/core/database/workout/WorkoutEntities.kt`
- Create: `core/database/src/main/kotlin/com/keepfit/core/database/workout/WorkoutDao.kt`
- Create: `core/database/src/main/kotlin/com/keepfit/core/database/KeepfitMigrations.kt`
- Create: `core/database/src/androidTest/kotlin/com/keepfit/core/database/workout/WorkoutDaoTest.kt`
- Create: `core/database/src/androidTest/kotlin/com/keepfit/core/database/KeepfitMigrationTest.kt`

- [x] Write failing DAO instrumentation tests for create, search, archive,
  template order, weekday schedule lookup, session completion, history, and
  records.
- [x] Write a failing migration test that opens the Phase 0 schema and verifies
  its profile after migration.
- [x] Implement entities, DAO queries, database version `2`, and migration.
- [x] Run database instrumentation tests on the Android emulator.

### Task 2: Private Exercise Media

**Files:**
- Create: `core/media/build.gradle.kts`
- Create: `core/media/src/main/kotlin/com/keepfit/core/media/ExerciseMediaStore.kt`
- Modify: `settings.gradle.kts`

- [x] Implement a focused importer that validates MP4, WebM, or GIF MIME types,
  copies to `files/media/exercises/`, and leaves existing files untouched on
  failure.
- [x] Register `core:media` without adding network dependencies.

### Task 3: Exercise Library

**Files:**
- Create: `feature/workouts/build.gradle.kts`
- Create: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/data/WorkoutRepository.kt`
- Create: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/data/RoomWorkoutRepository.kt`
- Create: `feature/workouts/src/test/kotlin/com/keepfit/feature/workouts/ExerciseInputValidatorTest.kt`
- Create: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/ExerciseInputValidator.kt`
- Create: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/WorkoutViewModel.kt`
- Create: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/ui/WorkoutsScreen.kt`
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/kotlin/com/keepfit/app/di/AppModule.kt`

- [x] Write failing JVM validation tests.
- [x] Implement minimal validation and repository mapping.
- [x] Build exercise search, create, edit, archive, and optional file-picker
  media import.
- [x] Run focused tests and assemble the app.

### Task 4: Templates and Weekly Plan

**Files:**
- Modify: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/data/WorkoutRepository.kt`
- Modify: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/data/RoomWorkoutRepository.kt`
- Modify: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/WorkoutViewModel.kt`
- Modify: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/ui/WorkoutsScreen.kt`
- Modify: `app/src/main/kotlin/com/keepfit/app/ui/home/HomeShell.kt`

- [x] Add template create flow with ordered exercises, target sets, target reps,
  and notes.
- [x] Add recurring weekday assignment for the active plan.
- [x] Surface today's planned workout on Today.

### Task 5: Active Session, History, and Records

**Files:**
- Create: `feature/workouts/src/test/kotlin/com/keepfit/feature/workouts/SetInputValidatorTest.kt`
- Create: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/SetInputValidator.kt`
- Create: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/ui/ActiveWorkoutScreen.kt`
- Modify: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/WorkoutViewModel.kt`
- Modify: `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/ui/WorkoutsScreen.kt`
- Modify: `app/src/main/kotlin/com/keepfit/app/ui/home/HomeShell.kt`

- [x] Write failing set-validation tests.
- [x] Implement start or resume session, add completed sets, notes, latest
  previous values, and workout completion.
- [x] Add 90-second vibration rest timer.
- [x] Add completed-session history, date markers, and derived records.

### Task 6: Verification and Documentation

**Files:**
- Modify: `README.md`

- [x] Update project status and workout feature notes.
- [x] Run unit tests, Room instrumentation tests, lint, debug APK assembly, and
  emulator workflow verification.
