# Changelog

Notable public changes are recorded here. Detailed engineering records remain
under `docs/done/versions/`.

## 0.23.0 - 2026-09-17

- Replaced the preloaded exercise dataset with an empty personal catalogue.
- Expanded exercise creation and editing to include equipment, targets,
  multiline instructions, notes, bodyweight state, and private demo media.
- Changed template creation to start with a name and add exercises from the
  template detail page; empty templates remain editable but unschedulable.
- Added exercise-detail access from template and weekly-plan views.
- Added a safe upgrade migration that preserves edited, referenced, and
  media-backed legacy exercises while removing untouched seed records.

## 0.22.0 - 2026-09-17

First public GitHub release.

### Added

- Local multi-profile fitness tracking, workouts, nutrition, progress photos,
  encrypted backups, optional Health Connect steps, and optional OpenRouter
  Coaches.
- A bundled searchable offline catalogue of 1,316 exercises and original
  code-native movement guidance for 25 common exercises.
- Direct template rename, exercise addition, set and repetition editing,
  per-exercise removal, and atomic multi-template deletion.
- Apache-2.0 source licensing, explicit media boundaries, and packaged
  third-party notices.

### Changed

- Reorganized Plan, Log, Progress, Settings, and Coach around their primary
  workflows with compact headers and navigable secondary tools.
- Delayed Coach conversation persistence until the first question is sent and
  separated failed-message retry state from the composer.

### Security and Privacy

- Kept structured data and media local by default.
- Excluded provider credentials and private media from Android platform backup.
- Preserved explicit, validated encrypted backup and restore workflows.
