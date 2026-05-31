# Keepfit Phase 1D Backup, Restore, and Reminders Implementation Plan

**Goal:** Finish the offline MVP with real settings, local reminders, and
eventually encrypted backup and restore.

**Architecture:** Add `core:preferences` for DataStore-backed app settings and
WorkManager reminder scheduling. Add `feature:settings` for goals, units,
reminders, and rest timer editing.

---

### Task 1: Settings Persistence and Reminders

- [x] Add settings validation tests.
- [x] Implement DataStore-backed app settings and reminder scheduler.
- [x] Implement a real Settings screen with goals, units, reminders, and rest timer controls.
- [x] Wire the workout rest timer to the saved duration.
- [x] Verify settings compile, tests, lint, build, install, and app launch.

### Task 2: Backup and Restore

- [ ] Design encrypted backup archive format and manifest.
- [ ] Implement backup export flow.
- [ ] Implement restore preview, validation, and replace-data flow.
- [ ] Verify backup and restore end to end.
