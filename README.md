# Keepfit

Keepfit is a private, local-first Android fitness tracker for personal use. It
is intended to provide a focused alternative to subscription-based fitness
apps: useful daily tracking without accounts, social feeds, or unnecessary
complexity.

## Planned Capabilities

- Create exercises with optional offline animated demonstrations.
- Build workout templates and weekly workout plans.
- Log sets, repetitions, weight, notes, history, and personal records.
- Track personal foods, reusable meals, calories, and macronutrients.
- Record weight, BMI inputs, optional measurements, and weekly progress photos.
- Compare transformation photos between selected weeks.
- Export and restore an encrypted local backup.
- Optional Health Connect step tracking and an Ollama assistant.

## Project Status

Phase 0, Phase 1A, Phase 1B, and Phase 1C are implemented on the active
development branch. Phase 1D implementation is complete, with final device
backup roundtrip verification still pending. Phase 2A is in progress. The
project now includes a runnable Android application with:

- a first-run local profile form persisted with Room;
- a five-destination Compose Navigation shell;
- a searchable exercise library with edit, archive, and optional private demo
  media import;
- reusable workout templates and a recurring weekday plan;
- a Today card that starts the planned workout;
- active workout logging with sets, repetitions, kilograms, exercise notes,
  previous values, and a 90-second vibration timer;
- completed workout history and derived personal records;
- a personal food library with favorites, recents, archive, and reusable saved
  meals;
- a selected-date nutrition diary with breakfast, lunch, dinner, and snack
  sections;
- duplicate-yesterday nutrition logging and derived calorie and macro totals;
- a Today nutrition summary backed by the same diary data;
- dated body measurement logging with BMI derived from profile height and the
  latest weight;
- weekly transformation weeks with private photo imports for front, left,
  right, back, and legs angles;
- two-week angle comparison with empty states when a photo is missing;
- weekly progress summaries with workout counts, nutrition averages, and weight
  change;
- a real Settings screen for calorie and macro goals, units, workout reminder
  time, weekly progress reminder time, and configurable rest timer duration;
- DataStore-backed settings persistence and WorkManager-based local reminder
  scheduling;
- encrypted backup export plus restore preview, checksum validation, and
  replace-data restore flow;
- launcher branding from the repository logo asset plus an in-app Today header
  brand mark;
- an optional Today steps card backed by Health Connect availability checks,
  permission request flow, and daily plus seven-day step aggregates;
- Hilt dependency injection;
- Room schema export with explicit version `1` to `2`, `2` to `3`, and `3` to
  `4` migrations;
- unit tests plus Room DAO and migration instrumentation tests.

The current implementation target is supported-device verification for the
Health Connect steps flow and the later optional Ollama assistant phase.

## Architecture

Keepfit will be a native Android application using Kotlin, Jetpack Compose,
Room, Hilt, coroutines, `Flow`, DataStore, WorkManager, and app-private media
storage. Android 12 is the minimum supported version.

Read these references before implementation:

- [Architecture overview](docs/architecture/keepfit-overview.md)
- [Data model](docs/architecture/keepfit-data-model.md)
- [Delivery roadmap](docs/architecture/keepfit-roadmap.md)
- [AI agent rules](AGENTS.md)

## Delivery Sequence

| Phase | Outcome |
| --- | --- |
| Phase 0 | Android foundation, navigation, Room setup, and local profile |
| Phase 1A | Exercise library, workout plans, logging, history, and records |
| Phase 1B | Personal foods, reusable meals, nutrition diary, and today summary |
| Phase 1C | Measurements, BMI, weekly transformation photos, comparison, and summaries |
| Phase 1D | Encrypted backup, restore, reminders, and complete offline MVP |
| Phase 2A | Optional read-only Health Connect steps |
| Phase 2B | Optional Ollama assistant with reviewable plan suggestions |

## Privacy Principles

- Core features must work offline.
- Fitness data and media stay in app-private storage by default.
- No account, backend, or analytics service is required for the MVP.
- AI is opt-in and must not be required for tracking.
- AI output is general fitness guidance, never medical diagnosis.
- Imported photos and exercise media are exported only when the user explicitly
  creates a backup.

## Repository Layout

```text
.
|-- AGENTS.md
|-- README.md
|-- app/
|-- core/
|   |-- database/
|   |-- designsystem/
|   |-- media/
|   |-- preferences/
|   `-- model/
|-- feature/
|   |-- nutrition/
|   |-- settings/
|   |-- steps/
|   |-- transformation/
|   `-- workouts/
|-- docs/
|   |-- architecture/
|   `-- superpowers/
|       `-- plans/
|-- gradle/
|   |-- libs.versions.toml
|   `-- wrapper/
|-- build.gradle.kts
|-- settings.gradle.kts
|-- gradlew
`-- gradlew.bat
```

Additional feature modules will be introduced when their roadmap phases begin.
The planned module boundaries are documented in the architecture overview.

## Development

### Requirements

- JDK 21 installed and exposed through `JAVA_HOME`.
- Android SDK platform `36`.
- Android SDK build tools `36.0.0`.
- Android SDK platform tools.

Set the local SDK path in an ignored `local.properties` file:

```properties
sdk.dir=C\:\\Users\\your-name\\AppData\\Local\\Android\\Sdk
```

### Commands

Use the Gradle wrapper from PowerShell:

```powershell
.\gradlew.bat projects
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :feature:nutrition:testDebugUnitTest
.\gradlew.bat :core:database:compileDebugAndroidTestKotlin
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Run Room instrumentation tests on a connected Android emulator or device:

```powershell
.\gradlew.bat :core:database:connectedDebugAndroidTest
```

## License

No license has been selected. Treat the repository as private unless a license
file is added explicitly.
