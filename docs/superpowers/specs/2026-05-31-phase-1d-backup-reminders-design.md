# Keepfit Phase 1D Backup, Restore, and Reminders Design

## Scope

Phase 1D finishes the offline MVP with three related capabilities:

1. persistent app settings and reminder preferences,
2. local workout and transformation reminders,
3. encrypted backup and restore.

This first implementation slice covers settings persistence, reminder
scheduling, and configurable rest timer behavior. Backup and restore remain the
next slice inside the same phase.

## Decisions

- Add `core:preferences` for DataStore-backed application settings and reminder
  scheduling.
- Add `feature:settings` for goals, units, reminders, and rest-timer UI.
- Keep nutrition goals on the existing `BodyProfile` row and edit them from the
  Settings screen.
- Use WorkManager unique periodic work for workout and weekly transformation
  reminders.
- Use local notification channels owned by the application.
- Defer encrypted backup export and restore to the next 1D slice after
  settings and reminder plumbing is stable.

## Settings Covered In This Slice

- weight unit;
- measurement unit;
- rest timer duration;
- workout reminder enabled state and time;
- weekly transformation reminder enabled state, weekday, and time;
- daily calorie and macro goals.

## Testing Strategy

- JVM tests for settings input validation.
- App compile verification for timer integration and Settings routing.
- Emulator verification for settings persistence, timer duration change, and
  reminder scheduling installation path.
