# Keepfit Android Architecture Overview

## 1. Purpose

Keepfit is a private Android fitness tracker for one person. It replaces several
complex subscription applications with a focused offline experience:

- create exercises with optional animated demonstrations;
- assemble workout templates and weekly plans;
- log sets, repetitions, weight, notes, and personal progress;
- track meals, calories, and macronutrients;
- record weekly measurements and transformation photos;
- compare transformation photos between selected weeks;
- export and restore a private backup.

The first release must work without an account, backend, or internet connection.
Step tracking and an AI assistant are optional follow-up features.

## 2. Product Boundaries

### Included in the first release

- One local user profile.
- Android 12 and newer.
- Manual exercise creation and local media import.
- Weekly workout planning and completed workout history.
- Personal foods, reusable meals, and quick food logging.
- Weight, BMI inputs, optional body measurements, and transformation photos.
- Local notifications for workout reminders, a rest timer, and weekly photo
  reminders.
- Encrypted backup export and restore.

### Deliberately deferred

- Cloud accounts and multi-device sync.
- Social feeds, friends, leaderboards, and community exercise catalogs.
- Barcode scanning and remote food databases.
- Advanced lifting features such as supersets, plate calculators, and RPE.
- AI-generated medical guidance.

## 3. Architecture Decision

Build a native modular Android application using Kotlin and Jetpack Compose.
Use a single-activity UI with Compose Navigation, Room for structured data,
Hilt for dependency injection, coroutines and `Flow` for asynchronous state,
and WorkManager for persistent background reminders and backup-related work.

This choice fits the Android-only requirement and gives later Health Connect
integration a direct platform path. Feature modules keep optional integrations
isolated from the offline core.

### Recommended Gradle modules

| Module | Responsibility |
| --- | --- |
| `app` | Application entry point, navigation graph, dashboard composition, dependency wiring |
| `core:model` | Shared domain models and value types |
| `core:database` | Room database, entities, DAOs, migrations, and repository implementations |
| `core:media` | Import, validate, store, retrieve, export, and restore private media |
| `core:preferences` | DataStore-backed app settings and reminder scheduling |
| `core:designsystem` | Theme, reusable Compose components, and application icons |
| `feature:workouts` | Exercise library, templates, plans, workout sessions, history, records, timer |
| `feature:nutrition` | Personal foods, saved meals, diary entries, and daily totals |
| `feature:transformation` | Measurements, weekly photo capture/import, and comparison |
| `feature:settings` | User goals, reminder preferences, backup export, and restore |
| `feature:steps` | Phase-2 Health Connect availability, permission, and daily step reads |
| `feature:assistant` | Phase-2 optional Ollama settings, chat, summaries, and draft plan proposals |

For the first implementation increment, modules may be introduced as features
are built. The dependency direction remains fixed:

```text
app -> feature:* -> core:model
                -> core:database
                -> core:media
                -> core:designsystem

feature:steps     -> Health Connect SDK
feature:assistant -> Ollama HTTP API
```

Feature modules must not depend on each other directly. Shared behavior belongs
in a focused `core:*` module or is coordinated by `app`.

## 4. Data and Media Flow

### Structured data

1. A Compose screen sends user actions to a feature ViewModel.
2. The ViewModel validates the input and calls a repository interface.
3. The repository writes through a Room DAO.
4. Room exposes observable queries as `Flow`.
5. The ViewModel converts repository data into immutable UI state.

Room is the source of truth. The UI never stores authoritative workout,
nutrition, or transformation state.

### Exercise demonstrations

Exercise animations are user-imported MP4, WebM, or GIF files. `core:media`
copies an approved document URI into app-private storage and stores only a
stable relative path in Room. The exercise editor can replace or remove an
attachment without changing exercise history.

Short videos should be preferred over GIF files because they are generally
smaller and more efficient to play. Video playback should use Android Media3.

### Transformation photos

Transformation photos are imported or captured for a week and a fixed angle:
front, left, right, back, or legs. Files live in app-private storage. The
comparison view loads the same angle from two selected weeks side by side.
Missing angles show an empty state instead of blocking comparison.

### Private file layout

```text
files/
  media/
    exercises/<exercise-media-id>.<extension>
    transformation/<week-id>/<angle>.<extension>
  backups/
    staging/
```

File names are generated identifiers. Original user file names are not used as
paths.

## 5. Privacy, Backup, and Restore

Keepfit is local-first, not cloud-backed. Room data and private media remain in
the application sandbox. Android removes app-private files when the app is
uninstalled, so the settings screen must make backup export easy to find.

An exported backup is a single encrypted archive containing:

```text
manifest.json
database.sqlite
settings.json
media/
```

The manifest contains a backup format version, export timestamp, application
schema version, and checksums. The user selects the destination with Android's
Storage Access Framework. A restore validates the archive and checksums before
replacing current data. The restore screen warns that current local data will
be replaced and requires explicit confirmation.

The encryption passphrase is entered during export and restore. It is never
stored by the app.

## 6. Optional Integrations

### Health Connect steps

`feature:steps` is a phase-2 adapter. It must:

- detect whether Health Connect is available on the device;
- request read permission for step data only;
- read daily totals and expose an unavailable state when unsupported or denied;
- keep the dashboard functional when the integration is disabled.

Steps are a supplementary dashboard metric. They are not required for workout
or nutrition tracking.

### Ollama assistant

`feature:assistant` is a phase-2 adapter over Ollama's chat API. It supports a
user-configured endpoint, model name, and optional token stored with Android
secure credential storage.

The assistant may:

- summarize recent workouts, nutrition totals, weight, and step trends;
- draft a weekly workout plan;
- suggest general adjustments based on user-entered goals and measurements.

The assistant must not directly modify a weekly plan. It returns a draft that
the user reviews and explicitly applies. It must present fitness suggestions as
general guidance, not diagnosis or medical advice.

No AI dependency is permitted in core tracking flows.

## 7. Navigation

Use a bottom navigation bar with five destinations:

| Destination | Main content |
| --- | --- |
| Today | Planned workout, food summary, reminders, and phase-2 steps |
| Workouts | Exercises, templates, weekly plan, session history, personal records |
| Nutrition | Daily diary, foods, saved meals, and recent entries |
| Progress | Measurements, weekly photos, photo comparison, and weekly summary |
| Settings | Goals, reminders, backup and restore, optional integrations |

The active workout screen is a dedicated focused flow launched from Today or
Workouts. It should keep its state when the app is backgrounded.

## 8. Error Handling

- Reject unsupported or unreadable media imports with a clear message.
- Preserve existing media until a replacement file is copied successfully.
- Validate required numeric values before saving; allow zero weight for
  bodyweight exercises.
- Show recoverable empty states for missing photos, missing Health Connect
  support, and disabled AI.
- Validate a backup completely before replacing current data.
- Keep the previous database and media until restore succeeds.

## 9. Quality Strategy

- Unit test repositories, daily totals, plan scheduling, personal-record
  calculations, BMI calculations, and backup manifest validation.
- Use Room instrumentation tests for DAO queries and migrations.
- Use Compose UI tests for the primary logging and comparison workflows.
- Use fake adapters for Health Connect and Ollama so optional integrations do
  not make core tests depend on device services or a network.

## 10. References

- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Room persistence library](https://developer.android.com/training/data-storage/room)
- [App-specific storage](https://developer.android.com/training/data-storage/app-specific)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Health Connect availability](https://developer.android.com/health-and-fitness/health-connect/availability)
- [Read Health Connect data](https://developer.android.com/health-and-fitness/health-connect/read-data)
- [Ollama API](https://docs.ollama.com/api)
- [Ollama authentication](https://docs.ollama.com/api/authentication)
