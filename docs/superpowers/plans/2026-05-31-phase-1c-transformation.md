# Keepfit Phase 1C Transformation Tracking Implementation Plan

**Goal:** Deliver private measurement logging, weekly progress photos, BMI
display, and week comparison in the Progress tab.

**Architecture:** Add `feature:transformation`, extend `core:database` to
schema `4`, and extend `core:media` for transformation-photo import.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, coroutines, `Flow`.

---

### Task 1: Schema and Media Storage

- [x] Add failing DAO and migration tests for measurements, weeks, and photos.
- [x] Implement Room schema `4`, transformation DAO, and migration.
- [x] Implement transformation photo import into app-private storage.

### Task 2: Repository and Validation

- [x] Add validation tests for measurements and BMI helper behavior.
- [x] Implement transformation repository models and mappings.
- [x] Derive weekly summaries from workout, nutrition, and measurement data.

### Task 3: Progress UI

- [x] Build measurement entry and history UI.
- [x] Build weekly transformation week management and photo import UI.
- [x] Build two-week angle comparison with missing-photo empty states.
- [x] Replace the Progress placeholder route with the real screen.

### Task 4: Verification and Docs

- [x] Update docs and phase status.
- [x] Run unit tests, Room instrumentation tests, lint, debug build, and
  emulator verification.
