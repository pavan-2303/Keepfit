# Keepfit Phase 2B Ollama Assistant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an optional Ollama-backed assistant that can be configured safely, chat with the user, assemble local fitness summaries, and generate reviewable workout-plan drafts without mutating core data automatically.

**Architecture:** Introduce `feature:assistant` as the Phase 2B boundary, keep simple assistant settings in `core:preferences`, store Ollama Cloud API keys in Android secure storage behind an interface, and route all AI behavior through explicit user actions. Build the phase in vertical slices so each slice leaves the app runnable and testable.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, coroutines, `Flow`, DataStore, Android secure storage abstraction, Room-backed repositories, and Ollama HTTP `/api/chat`.

**Progress Update (2026-06-01):**

- Tasks 1 through 17 are complete.
- The app now includes build-time Ollama Cloud configuration, cloud-safe chat requests, a working Settings connection test, local-data-backed progress summaries, structured weekly-plan draft generation, draft review, and approved-plan application into the existing Workouts plan flow.
- Verification completed in-session:
  - `.\gradlew.bat :feature:assistant:testDebugUnitTest`
  - `.\gradlew.bat :feature:settings:testDebugUnitTest`
  - `.\gradlew.bat :app:testDebugUnitTest --tests com.keepfit.app.ui.home.HomeShellViewModelTest`
  - `.\gradlew.bat :app:assembleDebug`
  - `.\gradlew.bat :app:installDebug`
  - `.\gradlew.bat :feature:settings:connectedDebugAndroidTest`
  - `.\gradlew.bat :feature:assistant:connectedDebugAndroidTest`
  - `.\gradlew.bat :core:preferences:lintDebug`
  - `.\gradlew.bat lintDebug`
- Manual emulator verification completed for:
  - reachable assistant chat,
  - progress summary generation,
  - draft weekly-plan generation,
  - review-before-apply behavior,
  - approved draft appearing in `Workouts > Plan`,
  - assistant disabled leaving core workflows unchanged,
  - unreachable endpoint preserving drafted text after a real failed send.
- Residual limitation: final verification was completed on the emulator and not yet repeated on a physical device.

---

## File Map

### Create

- `feature/assistant/build.gradle.kts`
- `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/AssistantViewModel.kt`
- `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantRepository.kt`
- `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantModels.kt`
- `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/FakeAssistantRepository.kt`
- `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/OllamaAssistantRepository.kt`
- `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/OllamaDtos.kt`
- `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantPromptAssembler.kt`
- `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantSummaryRepository.kt`
- `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/ui/AssistantScreen.kt`
- `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/ui/AssistantRoute.kt`
- `feature/assistant/src/test/kotlin/com/keepfit/feature/assistant/AssistantSettingsValidationTest.kt`
- `feature/assistant/src/test/kotlin/com/keepfit/feature/assistant/AssistantViewModelTest.kt`
- `feature/assistant/src/test/kotlin/com/keepfit/feature/assistant/AssistantPromptAssemblerTest.kt`
- `feature/assistant/src/test/kotlin/com/keepfit/feature/assistant/OllamaResponseParsingTest.kt`
- `core/preferences/src/main/kotlin/com/keepfit/core/preferences/AssistantSettings.kt`
- `core/preferences/src/main/kotlin/com/keepfit/core/preferences/AssistantCredentialStore.kt`
- `core/preferences/src/main/kotlin/com/keepfit/core/preferences/AndroidAssistantCredentialStore.kt`
- `core/preferences/src/test/kotlin/com/keepfit/core/preferences/AssistantSettingsRepositoryTest.kt`

### Modify

- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/kotlin/com/keepfit/app/di/AppModule.kt`
- `core/preferences/src/main/kotlin/com/keepfit/core/preferences/AppSettings.kt`
- `core/preferences/src/main/kotlin/com/keepfit/core/preferences/AppSettingsRepository.kt`
- `core/preferences/src/main/kotlin/com/keepfit/core/preferences/SettingsPreferenceStore.kt`
- `feature/settings/src/main/kotlin/com/keepfit/feature/settings/SettingsViewModel.kt`
- `feature/settings/src/main/kotlin/com/keepfit/feature/settings/ui/SettingsScreen.kt`
- `app/src/main/kotlin/com/keepfit/app/ui/home/HomeShell.kt`
- `README.md`
- `docs/architecture/keepfit-overview.md`
- `docs/architecture/keepfit-roadmap.md`

### Reuse Read Models

- `feature/workouts/src/main/kotlin/com/keepfit/feature/workouts/data/WorkoutRepository.kt`
- `feature/nutrition/src/main/kotlin/com/keepfit/feature/nutrition/data/NutritionRepository.kt`
- `feature/transformation/src/main/kotlin/com/keepfit/feature/transformation/data/TransformationRepository.kt`
- `feature/steps/src/main/kotlin/com/keepfit/feature/steps/data/StepsRepository.kt`

---

### Task 1: Introduce Assistant Module Skeleton

**Files:**
- Create: `feature/assistant/build.gradle.kts`
- Create: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantModels.kt`
- Create: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantRepository.kt`
- Modify: `settings.gradle.kts`
- Modify: `gradle/libs.versions.toml`

- [x] Add `:feature:assistant` to `settings.gradle.kts`.
- [x] Add any missing network or JSON dependencies needed by the assistant module to `gradle/libs.versions.toml`.
- [x] Create `feature/assistant/build.gradle.kts` using the same Android library, Compose, Hilt, and test pattern as `feature:steps`.
- [x] Define assistant domain models for:
  - connection status,
  - chat message role/content,
  - pending draft input,
  - assistant error state,
  - progress summary request result,
  - draft workout proposal.
- [x] Define `AssistantRepository` with explicit methods for:
  - testing the configured connection,
  - sending a chat turn,
  - generating a progress summary,
  - requesting a draft plan.
- [x] Run: `.\gradlew.bat :feature:assistant:compileDebugKotlin`
- [x] Expected: the new module compiles even before it is wired into the app.

### Task 2: Extend Settings Persistence for Assistant Basics

**Files:**
- Create: `core/preferences/src/main/kotlin/com/keepfit/core/preferences/AssistantSettings.kt`
- Modify: `core/preferences/src/main/kotlin/com/keepfit/core/preferences/AppSettings.kt`
- Modify: `core/preferences/src/main/kotlin/com/keepfit/core/preferences/AppSettingsRepository.kt`
- Modify: `core/preferences/src/main/kotlin/com/keepfit/core/preferences/SettingsPreferenceStore.kt`
- Test: `core/preferences/src/test/kotlin/com/keepfit/core/preferences/AssistantSettingsRepositoryTest.kt`

- [x] Write failing tests for assistant settings defaults and round-trip persistence.
- [x] Run the focused preference test task and confirm the new tests fail for missing assistant fields.
- [x] Add an `AssistantSettings` value type with:
  - `enabled: Boolean`
  - `baseUrl: String`
  - `modelName: String`
- [x] Add assistant settings to `AppSettings`.
- [x] Extend `AppSettingsRepository` with an update method for assistant settings.
- [x] Persist assistant settings in `SettingsPreferenceStore.kt` using new DataStore keys with conservative defaults:
  - disabled by default,
  - base URL defaulting to Ollama Cloud,
  - empty model name by default.
- [x] Re-run the focused preference tests until they pass.
- [x] Run: `.\gradlew.bat :core:preferences:testDebugUnitTest`

### Task 3: Add Secure Token Storage Abstraction

**Files:**
- Create: `core/preferences/src/main/kotlin/com/keepfit/core/preferences/AssistantCredentialStore.kt`
- Create: `core/preferences/src/main/kotlin/com/keepfit/core/preferences/AndroidAssistantCredentialStore.kt`
- Modify: `app/src/main/kotlin/com/keepfit/app/di/AppModule.kt`

- [x] Define `AssistantCredentialStore` with:
  - `suspend fun readToken(): String?`
  - `suspend fun writeToken(token: String?)`
- [x] Implement the Android-backed store behind a single file-scoped responsibility.
- [x] Keep the interface fakeable for unit tests and offline development.
- [x] Wire the credential store into `AppModule.kt` as a singleton dependency.
- [x] Run: `.\gradlew.bat :app:compileDebugKotlin`
- [x] Expected: DI compiles with the new assistant credential dependency.

### Task 4: Add Assistant Settings Validation

**Files:**
- Create: `feature/assistant/src/test/kotlin/com/keepfit/feature/assistant/AssistantSettingsValidationTest.kt`
- Create or modify: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantModels.kt`

- [x] Write failing tests for:
  - rejecting blank model names,
  - rejecting malformed URLs,
  - rejecting missing API keys for `https://ollama.com/api`,
  - rejecting `-cloud` model names for direct cloud access,
  - accepting `https://ollama.com/api` with an API key.
- [x] Run the focused assistant validation test and confirm failure.
- [x] Implement a small validation helper that normalizes assistant settings input and produces user-facing errors.
- [x] Re-run the focused assistant test until it passes.
- [x] Run: `.\gradlew.bat :feature:assistant:testDebugUnitTest --tests com.keepfit.feature.assistant.AssistantSettingsValidationTest`

### Task 5: Extend Settings ViewModel for Assistant Configuration

**Files:**
- Modify: `feature/settings/src/main/kotlin/com/keepfit/feature/settings/SettingsViewModel.kt`
- Modify: `app/src/main/kotlin/com/keepfit/app/di/AppModule.kt`

- [x] Inject assistant settings persistence and credential storage into `SettingsViewModel`.
- [x] Add save actions for:
  - assistant enabled toggle,
  - base URL,
  - model name,
  - token save/clear.
- [x] Add a connection-test action contract, but keep it unimplemented until the repository exists.
- [x] Keep all failure messages consistent with the current snackbar pattern.
- [x] Run: `.\gradlew.bat :feature:settings:compileDebugKotlin`

### Task 6: Add Assistant Controls to Settings UI

**Files:**
- Modify: `feature/settings/src/main/kotlin/com/keepfit/feature/settings/ui/SettingsScreen.kt`

- [x] Add a new assistant card below the existing settings sections.
- [x] Include:
  - enable toggle,
  - base URL text field,
  - model name text field,
  - token text field,
  - save button,
  - clear token button,
  - connection test button,
  - open assistant button placeholder disabled until chat route is wired.
- [x] Add guidance text that the assistant is optional and general fitness guidance only.
- [x] Keep the settings screen scroll behavior and snackbar behavior unchanged.
- [x] Run: `.\gradlew.bat :feature:settings:compileDebugKotlin`

### Task 7: Build Fake Assistant Repository First

**Files:**
- Create: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/FakeAssistantRepository.kt`
- Create: `feature/assistant/src/test/kotlin/com/keepfit/feature/assistant/AssistantViewModelTest.kt`
- Create: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/AssistantViewModel.kt`

- [x] Write failing tests for view model behavior using a fake repository:
  - connection test success,
  - connection test failure,
  - sending a message appends assistant reply,
  - failed send preserves the drafted user input.
- [x] Run the focused assistant view model test and confirm failure.
- [x] Implement `FakeAssistantRepository` with deterministic responses for tests.
- [x] Implement `AssistantViewModel` with state for:
  - settings status,
  - message list,
  - drafted input,
  - loading state,
  - error state.
- [x] Re-run the focused test until it passes.
- [x] Run: `.\gradlew.bat :feature:assistant:testDebugUnitTest --tests com.keepfit.feature.assistant.AssistantViewModelTest`

### Task 8: Add Assistant Screen and Route

**Files:**
- Create: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/ui/AssistantScreen.kt`
- Create: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/ui/AssistantRoute.kt`
- Modify: `app/src/main/kotlin/com/keepfit/app/ui/home/HomeShell.kt`

- [x] Add an assistant route launched from Settings instead of adding a sixth bottom-nav destination.
- [x] Build a simple chat layout with:
  - header,
  - general-guidance disclaimer,
  - scrolling message list,
  - input field,
  - send button,
  - retry affordance for failed sends.
- [x] Keep the route usable with the fake repository before the real Ollama client is wired.
- [x] Run: `.\gradlew.bat :app:assembleDebug`

### Task 9: Implement Real Ollama Chat Adapter

**Files:**
- Create: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/OllamaDtos.kt`
- Create: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/OllamaAssistantRepository.kt`
- Modify: `app/src/main/kotlin/com/keepfit/app/di/AppModule.kt`

- [x] Implement DTOs for `POST /api/chat`.
- [x] Use non-streaming chat responses first.
- [x] Support optional bearer-token auth for non-local endpoints.
- [x] Map network and parsing failures to user-facing assistant errors.
- [x] Wire the real repository in DI, while preserving a fake path for tests.
- [x] Run: `.\gradlew.bat :feature:assistant:testDebugUnitTest`
- [x] Run: `.\gradlew.bat :app:assembleDebug`

### Task 10: Add Connection Test Workflow

**Files:**
- Modify: `feature/settings/src/main/kotlin/com/keepfit/feature/settings/SettingsViewModel.kt`
- Modify: `feature/settings/src/main/kotlin/com/keepfit/feature/settings/ui/SettingsScreen.kt`
- Modify: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantRepository.kt`

- [x] Implement a dedicated connection test that performs a minimal assistant request using the configured endpoint/model/token.
- [x] Surface success and failure through the existing Settings snackbar behavior.
- [x] Ensure a bad endpoint or missing remote auth produces a clear failure without clearing the form fields.
- [x] Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.keepfit.app.ui.home.HomeShellViewModelTest`

### Task 11: Build Local Summary Assembly

**Files:**
- Create: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantSummaryRepository.kt`
- Create: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantPromptAssembler.kt`
- Create: `feature/assistant/src/test/kotlin/com/keepfit/feature/assistant/AssistantPromptAssemblerTest.kt`
- Modify: `app/src/main/kotlin/com/keepfit/app/di/AppModule.kt`

- [x] Write failing tests for prompt assembly from existing read models:
  - recent workout history and records,
  - current nutrition totals and goals,
  - latest weight/BMI/progress metrics,
  - optional steps summary when available.
- [x] Run the focused prompt-assembler test and confirm failure.
- [x] Implement a summary repository that reads existing feature repositories through app-wired interfaces.
- [x] Implement a prompt assembler that produces compact deterministic sections rather than raw dumps.
- [x] Re-run the focused tests until they pass.
- [x] Run: `.\gradlew.bat :feature:assistant:testDebugUnitTest --tests com.keepfit.feature.assistant.AssistantPromptAssemblerTest`

### Task 12: Add Explicit Progress Summary Action

**Files:**
- Modify: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/AssistantViewModel.kt`
- Modify: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/ui/AssistantScreen.kt`
- Modify: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantRepository.kt`

- [x] Add a dedicated assistant action to generate a local-data-backed progress summary.
- [x] Ensure summary generation only happens after explicit user action.
- [x] Render the returned summary as a normal assistant message with disclaimer text preserved on screen.
- [x] Run: `.\gradlew.bat :feature:assistant:testDebugUnitTest`

### Task 13: Define Draft Workout Plan Response Shape

**Files:**
- Modify: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantModels.kt`
- Create: `feature/assistant/src/test/kotlin/com/keepfit/feature/assistant/OllamaResponseParsingTest.kt`

- [x] Write failing tests for parsing a structured draft plan response into:
  - plan name,
  - weekdays,
  - template names,
  - ordered exercise names,
  - optional notes.
- [x] Run the focused parsing test and confirm failure.
- [x] Define a strict draft-plan model and parser that rejects malformed or partial responses.
- [x] Re-run the focused test until it passes.
- [x] Run: `.\gradlew.bat :feature:assistant:testDebugUnitTest --tests com.keepfit.feature.assistant.OllamaResponseParsingTest`

### Task 14: Add Draft Plan Request and Review UI

Status: completed on 2026-06-01.

**Files:**
- Modify: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/AssistantViewModel.kt`
- Modify: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/ui/AssistantScreen.kt`

- [ ] Add an explicit “Draft weekly plan” action in the assistant UI.
- [ ] Ask Ollama for a structured draft using locally assembled context.
- [ ] Render the draft in a review section distinct from normal chat messages.
- [ ] Add “Apply draft” and “Dismiss draft” actions.
- [ ] Ensure malformed model output becomes a recoverable error, not a crash.
- [ ] Run: `.\gradlew.bat :feature:assistant:testDebugUnitTest`

### Task 15: Apply Approved Drafts Through Existing Workouts APIs

Status: completed on 2026-06-01.

**Files:**
- Modify: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/data/AssistantRepository.kt`
- Modify: `feature/assistant/src/main/kotlin/com/keepfit/feature/assistant/AssistantViewModel.kt`
- Modify: `app/src/main/kotlin/com/keepfit/app/di/AppModule.kt`

- [ ] Add an app-wired write path that turns an approved draft into weekly plan updates using existing workout repository operations.
- [ ] Keep the assistant from writing directly to Room.
- [ ] Guard against partial apply by validating the full draft before any write begins.
- [ ] Show a success or failure message after apply.
- [ ] Run: `.\gradlew.bat :app:assembleDebug`

### Task 16: UI Tests and Broader Verification

**Files:**
- Add or modify relevant Compose UI test files for assistant settings, chat, and draft review.

- [x] Add Compose UI coverage for:
  - assistant settings form,
  - failed send retaining drafted input,
  - review-before-apply draft flow.
- [x] Run: `.\gradlew.bat :feature:assistant:testDebugUnitTest`
- [x] Run: `.\gradlew.bat :feature:settings:testDebugUnitTest`
- [x] Run: `.\gradlew.bat :app:assembleDebug`
- [x] Run: `.\gradlew.bat lintDebug`

### Task 17: Documentation and Manual Exit-Criteria Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/architecture/keepfit-overview.md`
- Modify: `docs/architecture/keepfit-roadmap.md`

- [x] Update README setup notes for the optional assistant, including Ollama Cloud defaults and API-key behavior.
- [x] Update architecture docs if any final assistant boundaries differ from the current phase description.
- [x] Manually verify on device or emulator:
  - assistant disabled leaves core workflows unchanged,
  - reachable Ollama endpoint completes a chat request,
  - unreachable endpoint preserves drafted text,
  - progress summary uses local data,
  - draft plan does not change the weekly plan before approval,
  - approved draft applies correctly.
- [x] Record any residual limitations discovered during device verification.

## Self-Review

- This plan covers all roadmap deliverables for Phase 2B by separating them
  into settings, chat, summary, draft-plan, and verification tasks.
- The plan avoids placeholders and keeps tasks vertically sliceable.
- The plan keeps the assistant optional, testable with fakes, and prevented
  from directly mutating Room data.
