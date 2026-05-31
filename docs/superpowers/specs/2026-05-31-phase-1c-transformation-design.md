# Keepfit Phase 1C Transformation Design

## Scope

Phase 1C adds private progress tracking around measurements, weekly
transformation photos, BMI display, and week-to-week comparison.

## Decisions

- Add `feature:transformation` for repository, ViewModel, and Compose UI.
- Extend Room schema from version `3` to `4`.
- Add `BodyMeasurement`, `TransformationWeek`, and `TransformationPhoto`
  entities to `core:database`.
- Add a `TransformationPhotoStore` in `core:media` for app-private JPEG, PNG,
  and WebP imports.
- Use week-start dates normalized to Monday for transformation entries.
- Support photo import in Phase 1C. Camera capture can remain a later
  enhancement without blocking the private photo workflow.
- Derive BMI from the most recent weight measurement and profile height.
- Derive weekly summaries from completed workouts, nutrition diary totals, and
  measurement history already stored locally.

## User Flow

The Progress tab will have four compact sections:

1. Current body metrics:
   latest weight, BMI, and recent measurement snapshot.
2. Measurement log:
   add a dated body measurement entry and review history.
3. Weekly photos:
   add or update a transformation week, attach photos by angle, and review week
   cards with summary data.
4. Comparison:
   choose two saved weeks and one angle to compare side by side, with an empty
   state when one side is missing.

## Validation and Constraints

- At least one numeric value or a note must be present for a measurement entry.
- Numeric measurement values must be greater than `0` when provided.
- A transformation week is keyed by normalized Monday `weekStartDate`.
- Only one photo per week and angle is allowed. Re-import replaces the stored
  file reference after the new file is copied successfully.
- Imported photos stay under `files/media/transformation/`.

## Testing Strategy

- Room instrumentation tests for measurements, weeks, photo replacement, and
  schema migration from `3` to `4`.
- JVM tests for measurement validation and BMI derivation helpers.
- Emulator verification for measurement entry, week creation, photo import,
  angle comparison, and missing-angle empty states.
