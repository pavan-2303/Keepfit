# Keepfit Phase 1B Nutrition Diary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a complete offline nutrition workflow from personal food creation through reusable meals, daily diary logging, duplicate-yesterday, and derived totals.

**Architecture:** Add `feature:nutrition` for domain repositories, ViewModels, and Compose screens. Extend Room schema to version `3` with nutrition entities, diary queries, and explicit migration support.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, coroutines, `Flow`.

---

### Task 1: Nutrition Schema and Migration

**Files:**
- Modify: `core/database/src/main/kotlin/com/keepfit/core/database/KeepfitDatabase.kt`
- Modify: `core/database/src/main/kotlin/com/keepfit/core/database/KeepfitMigrations.kt`
- Modify: `core/database/src/main/kotlin/com/keepfit/core/database/KeepfitTypeConverters.kt`
- Modify: `core/database/src/main/kotlin/com/keepfit/core/database/profile/BodyProfileEntity.kt`
- Create: `core/database/src/main/kotlin/com/keepfit/core/database/nutrition/NutritionEntities.kt`
- Create: `core/database/src/main/kotlin/com/keepfit/core/database/nutrition/NutritionDao.kt`
- Modify: `core/database/src/androidTest/kotlin/com/keepfit/core/database/KeepfitMigrationTest.kt`
- Create: `core/database/src/androidTest/kotlin/com/keepfit/core/database/nutrition/NutritionDaoTest.kt`

- [x] Write failing DAO instrumentation tests for foods, favorites, recent-food queries, saved meals, diary totals, duplicate-yesterday, and archived-food readability.
- [x] Write a failing migration test that opens schema `2`, preserves the existing profile, and adds nutrition-goal columns and nutrition tables in schema `3`.
- [x] Implement nutrition entities, DAO queries, database version `3`, and migration.
- [x] Run database instrumentation tests on the Android emulator.

### Task 2: Food Library and Validation

**Files:**
- Create: `feature/nutrition/build.gradle.kts`
- Create: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/FoodInputValidator.kt`
- Create: `feature/nutrition/src/test/kotlin/com/keepfit/feature/nutrition/FoodInputValidatorTest.kt`
- Create: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/data/NutritionRepository.kt`
- Create: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/data/RoomNutritionRepository.kt`
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/kotlin/com/keepfit/app/di/AppModule.kt`

- [x] Write failing JVM validation tests for food name, serving label, serving amount, and macro value rules.
- [x] Implement the validator, repository models, and repository mapping.
- [x] Add food create, search, favorite toggle, recent-food query, and archive behavior.
- [x] Run focused tests and compile the nutrition module.

### Task 3: Saved Meals

**Files:**
- Create: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/MealInputValidator.kt`
- Create: `feature/nutrition/src/test/kotlin/com/keepfit/feature/nutrition/MealInputValidatorTest.kt`
- Modify: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/data/NutritionRepository.kt`
- Modify: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/data/RoomNutritionRepository.kt`

- [x] Write failing JVM validation tests for saved-meal names and servings.
- [x] Implement saved-meal creation, ordered items, and expansion into diary entries.
- [x] Surface saved meals alongside foods in the add-entry workflow.

### Task 4: Diary Logging and Duplicate Yesterday

**Files:**
- Create: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/NutritionViewModel.kt`
- Create: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/ui/NutritionScreen.kt`
- Modify: `app/src/main/kotlin/com/keepfit/app/ui/home/HomeShell.kt`

- [x] Build a selected-date diary with breakfast, lunch, dinner, and snacks sections.
- [x] Add food and saved-meal logging with editable servings.
- [x] Add duplicate-yesterday for the selected date as a transaction-backed command.
- [x] Replace the Nutrition placeholder route with the real screen.

### Task 5: Totals and Today Summary

**Files:**
- Modify: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/data/NutritionRepository.kt`
- Modify: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/data/RoomNutritionRepository.kt`
- Modify: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/NutritionViewModel.kt`
- Modify: `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/ui/NutritionScreen.kt`
- Modify: `app/src/main/kotlin/com/keepfit/app/ui/home/HomeShell.kt`

- [x] Derive daily calorie and macro totals from diary entries.
- [x] Compare totals against stored profile nutrition goals when present.
- [x] Surface today's nutrition summary on Today.

### Task 6: Verification and Documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/architecture/keepfit-data-model.md`
- Modify: `docs/architecture/keepfit-roadmap.md`

- [x] Update docs for favorites, nutrition goals, and Phase 1B status.
- [x] Run unit tests, Room instrumentation tests, lint, debug APK assembly, and emulator workflow verification.
