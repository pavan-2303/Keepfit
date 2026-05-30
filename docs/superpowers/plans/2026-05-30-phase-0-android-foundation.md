# Keepfit Phase 0 Android Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable Android 12+ Keepfit foundation with modular Compose navigation and a Room-backed local profile onboarding flow.

**Architecture:** Use a native Kotlin Android application with AGP 9 built-in Kotlin, Jetpack Compose, Hilt, Room, DataStore, WorkManager, coroutines, and `Flow`. Add only the modules required by the Phase 0 behavior: `app`, `core:model`, `core:database`, and `core:designsystem`.

**Tech Stack:** AGP 9.2.1, Gradle 9.4.1, Kotlin/Compose compiler 2.3.21, Compose BOM 2026.05.00, Room 2.8.4, Hilt 2.59.2, DataStore 1.2.1, WorkManager 2.11.1.

---

### Task 1: Bootstrap Build Configuration

**Files:**
- Create: `.gitignore`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/build.gradle.kts`
- Create: `core/model/build.gradle.kts`
- Create: `core/database/build.gradle.kts`
- Create: `core/designsystem/build.gradle.kts`

- [x] Install Android SDK command-line tools, platform 36, build tools 36.0.0, and platform tools outside the repository.
- [x] Create the Gradle wrapper pinned to Gradle 9.4.1.
- [x] Configure repositories, version catalog entries, modules, Java 17 bytecode, Android 12 minimum SDK 31, and Room schema export.
- [x] Run `./gradlew projects` and confirm all four modules are visible.

### Task 2: Define and Test Local Profile Persistence

**Files:**
- Create: `core/model/src/main/kotlin/com/keepfit/core/model/BodyProfile.kt`
- Create: `core/database/src/main/kotlin/com/keepfit/core/database/profile/BodyProfileEntity.kt`
- Create: `core/database/src/main/kotlin/com/keepfit/core/database/profile/BodyProfileDao.kt`
- Create: `core/database/src/main/kotlin/com/keepfit/core/database/KeepfitDatabase.kt`
- Create: `core/database/src/androidTest/kotlin/com/keepfit/core/database/profile/BodyProfileDaoTest.kt`

- [x] Write a Room instrumentation test that inserts a local profile and reads it back as a `Flow`.
- [x] Run the focused test compilation and confirm it fails because the profile persistence classes do not exist.
- [x] Implement the Room entity, DAO, and database.
- [x] Compile the instrumentation tests and confirm the profile persistence test compiles.

### Task 3: Define and Test Onboarding State

**Files:**
- Create: `app/src/test/kotlin/com/keepfit/app/profile/ProfileInputValidatorTest.kt`
- Create: `app/src/main/kotlin/com/keepfit/app/profile/ProfileInputValidator.kt`
- Create: `app/src/main/kotlin/com/keepfit/app/profile/ProfileRepository.kt`
- Create: `app/src/main/kotlin/com/keepfit/app/profile/RoomProfileRepository.kt`
- Create: `app/src/main/kotlin/com/keepfit/app/profile/ProfileViewModel.kt`
- Create: `app/src/main/kotlin/com/keepfit/app/di/AppModule.kt`

- [x] Write JVM tests for valid profile input and blank-name rejection.
- [x] Run the focused JVM tests and confirm they fail because validation is missing.
- [x] Implement minimal validation, repository mapping, ViewModel state, and Hilt bindings.
- [x] Run the focused JVM tests and confirm they pass.

### Task 4: Build the Compose Shell

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/com/keepfit/app/KeepfitApplication.kt`
- Create: `app/src/main/kotlin/com/keepfit/app/MainActivity.kt`
- Create: `app/src/main/kotlin/com/keepfit/app/ui/KeepfitApp.kt`
- Create: `app/src/main/kotlin/com/keepfit/app/ui/onboarding/ProfileSetupScreen.kt`
- Create: `app/src/main/kotlin/com/keepfit/app/ui/home/HomeShell.kt`
- Create: `core/designsystem/src/main/kotlin/com/keepfit/core/designsystem/KeepfitTheme.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`

- [x] Implement a profile setup form shown until the Room profile exists.
- [x] Implement a five-destination bottom navigation shell: Today, Workouts, Nutrition, Progress, Settings.
- [x] Give each destination a useful Phase 0 empty state and keep the visual language quiet, compact, and fitness-focused.
- [x] Run `./gradlew testDebugUnitTest assembleDebug lintDebug`.

### Task 5: Document Local Development

**Files:**
- Modify: `README.md`

- [x] Add SDK environment, Gradle wrapper, test, lint, and debug build commands.
- [x] Run the full Phase 0 verification command and inspect `git status`.
