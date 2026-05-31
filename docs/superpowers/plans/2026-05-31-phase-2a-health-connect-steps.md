# Keepfit Phase 2A Health Connect Steps Implementation Plan

**Goal:** Add optional Health Connect step tracking without coupling it to the
offline MVP.

**Architecture:** Add `feature:steps` for Health Connect availability,
permission handling, aggregate reads, and Today dashboard UI.

---

### Task 1: Branding

- [x] Add launcher icons from `assets/logo-icon.png`.
- [x] Use the same asset as lightweight in-app branding.

### Task 2: Health Connect Steps

- [x] Add `feature:steps` module and Health Connect dependency.
- [x] Implement availability detection and permission status mapping.
- [x] Implement read-only daily and seven-day step aggregation.
- [x] Add Today dashboard steps UI with unavailable, denied, and connected states.
- [x] Verify compile, tests, lint, build, install, and emulator launch behavior.

### Task 3: Health Connect Visibility Fix

- [x] Register Health Connect package visibility in the manifest.
- [x] Add Health Connect onboarding and permission-rationale entry activities.
- [x] Add a direct manage-access intent path alongside the in-app permission launcher.

### Task 4: Workout Cleanup Controls

- [x] Add delete controls for exercises with dependency guards.
- [x] Add delete controls for templates with history protection.
- [x] Add clear-day controls for the weekly plan.

### Task 5: UI Refresh

- [x] Refresh the app theme with a more modern color system.
- [x] Add motion and richer visual hierarchy to the Today and steps surfaces.
- [x] Refresh the Workouts screen header and card presentation.
