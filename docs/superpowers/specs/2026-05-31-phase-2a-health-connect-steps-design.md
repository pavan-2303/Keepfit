# Keepfit Phase 2A Health Connect Steps Design

## Scope

Phase 2A adds optional daily step visibility through Health Connect without
making steps a core dependency for workouts, nutrition, or progress tracking.

This slice covers:

1. Health Connect SDK availability detection,
2. read-only `StepsRecord` permission request,
3. aggregated daily and seven-day step summaries,
4. Today dashboard presentation with clear unavailable and denied states.

## Decisions

- Add a dedicated `feature:steps` module.
- Keep step data read-only and out of Room.
- Use `HealthConnectClient.getSdkStatus(context)` to detect support before
  creating a client.
- Request only `HealthPermission.getReadPermission(StepsRecord::class)`.
- Use `aggregate()` with `StepsRecord.COUNT_TOTAL` to avoid double counting
  records from multiple sources.
- Do not filter by `DataOrigin` so Android 14+ on-device steps remain included
  automatically in aggregate totals.
- Expose steps as optional UI state. The Today screen remains fully usable when
  Health Connect is unavailable, not installed, outdated, denied, or empty.

## UI

- Add a Today steps card below nutrition and progress.
- Show one of these states:
  - unavailable on this device,
  - provider update required,
  - permission required,
  - no step data yet,
  - steps available with today total and seven-day summary.
- Keep the card action-focused:
  - install or update guidance when needed,
  - connect button for permission request,
  - refresh button after permission has been granted.

## Testing Strategy

- JVM tests for state mapping and aggregate summary formatting.
- Compile verification for Health Connect dependency and Today wiring.
- Emulator verification for unsupported or unavailable state on the current
  Android 12 test device.
