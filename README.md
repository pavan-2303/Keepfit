# Keepfit

Keepfit is a private, local-first Android fitness tracker for personal use. It
is intended to provide a focused alternative to subscription-based fitness
apps: useful daily tracking without accounts, social feeds, or unnecessary
complexity.

## Product Capabilities

- Browse 40 offline exercise guides, create personal exercises, and optionally
  view live animated demonstrations.
- Build workout templates and weekly workout plans.
- Log sets, repetitions, weight, notes, history, and personal records.
- Track personal foods, reusable meals, calories, and macronutrients.
- Record weight, BMI inputs, optional measurements, and transformation-cycle photos.
- Compare transformation photos between cycle day 0 and later updates.
- Export and restore an encrypted local backup.
- Optional Health Connect step tracking and a user-authorized OpenRouter assistant.

## Project Status

The active development branch contains a runnable Android application with:

- a first-run local profile form persisted with Room;
- a five-destination Compose Navigation shell organized as Today, Plan, Log,
  Progress, and Coach, with Settings behind the profile action;
- a searchable exercise library with edit, archive, and optional private demo
  media import;
- a source-aware exercise browser with 40 original offline guides, explicit
  duplicate-safe addition, and a transient view-only ExerciseDB prototype;
- current live GIF demonstrations with attribution, cache-disabled loading, and
  recoverable offline, timeout, malformed-response, and provider states;
- an offline guided setup that creates an editable starter week from a goal,
  experience, available days, time, equipment, exercises to avoid, and a
  maintainable nutrition-tracking preference;
- reusable workout templates and a recurring weekday plan;
- a decisive Today workout card that starts or resumes the next action and
  supports reviewed full, shorter, minimum, substitution, reschedule, skip,
  and restore decisions for one date without editing the reusable template;
- recovery for the most recent unresolved planned workout from the previous
  seven days;
- focused active workout execution with target progress, editable previous-set
  defaults, one-tap repeat, automatic configurable rest timing, safe minimum
  and substitution adaptations, and optional completion feedback;
- explained offline progression suggestions that either repeat the last
  completed performance or propose one bounded weight or repetition increase;
- a two-minute offline weekly review with a seven-day completion ribbon,
  evidence-based encouragement, optional nutrition and step trends, and at
  most two small coming-week drafts;
- explicit approve, edit, dismiss, pause, and resume review controls, with an
  approved change stored as one dated occurrence rather than a template or
  recurring-plan rewrite;
- completed workout history and derived personal records;
- a personal food library with favorites, recents, archive, and reusable saved
  meals;
- a selected-date nutrition diary with breakfast, lunch, dinner, and snack
  sections;
- duplicate-yesterday nutrition logging and derived calorie and macro totals;
- selectable full-macro, calorie-and-protein, meal-quality, and disabled
  nutrition lenses that preserve all existing history when changed;
- 5%, 10%, or 15% goal ranges, serving presets, scoped previous-meal repeat,
  and saved-meal expansion that stays independent from later meal edits;
- a mode-specific Today nutrition summary and direct logging or check-in action;
- dated body measurement logging with BMI derived from profile height and the
  latest weight;
- transformation cycles with private photo imports for front, left, right, and
  back angles;
- cycle-day comparison with empty states when a photo is missing;
- cycle summaries with workout counts, nutrition averages, and weight change;
- categorized Settings for goals, training preferences, connections, private
  data, and safety information;
- DataStore-backed settings persistence and WorkManager-based local reminder
  scheduling;
- encrypted backup export plus restore preview, checksum validation, and
  replace-data restore flow;
- launcher branding from the repository logo asset plus an in-app Today header
  brand mark;
- an optional Today steps card backed by Health Connect availability checks,
  permission request flow, and daily plus seven-day step aggregates;
- a dedicated query-only Coach authorized through the user's own OpenRouter
  account, with Keystore-encrypted credentials, an explicit privacy disclosure,
  general chat, and automatic compact local-progress context for relevant
  questions; Keepfit adds no local request cap;
- Hilt dependency injection;
- Room schema export with explicit migrations through schema version `10`;
- unit tests plus Room DAO, migration, Compose UI, and end-to-end backup
  instrumentation coverage.

The approved product direction and phase-wise delivery plan are documented in
the [Practical Fitness Journey and Optional AI Coach](docs/backlog/items/practical-fitness-journey-and-ai-coach.md)
initiative. Slice A was delivered in
[v0.2.0 - Offline Starter Week](docs/done/versions/v0.2.0.md), and Slice B was
delivered in [v0.3.0 - Decisive Today](docs/done/versions/v0.3.0.md). Slice C
was delivered in
[v0.4.0 - Low-Friction Workout Execution](docs/done/versions/v0.4.0.md).
Slice D was delivered in
[v0.5.0 - Weekly Review and Flexible Motivation](docs/done/versions/v0.5.0.md).
Slice E was delivered in
[v0.6.0 - Sustainable Nutrition](docs/done/versions/v0.6.0.md). Slice F was
delivered in [v0.7.0 - Exercise Guidance and Live Demo Prototype](docs/done/versions/v0.7.0.md).
Slice G was delivered in
[v0.8.0 - Private OpenRouter Access](docs/done/versions/v0.8.0.md). Slice H was
delivered as the pre-beta
[v0.9.0 - Bounded Coaching Workflows](docs/done/versions/v0.9.0.md). The next
approved slice is
[v0.10.0 - Guided Journey and Interface Reorganization](docs/versions/v0.10.0.md).
Approved follow-on work for recovery, multiple profiles, transformation pose
sets, an audited offline catalogue, persistent named Coaches, optional AI plan
creation, and visual polish is organized in the
[Durable Personalized Fitness Companion](docs/backlog/items/durable-personalized-fitness-companion.md)
initiative. It remains planning-only until the current version closes and the
next sprint specification is approved.

## Architecture

Keepfit will be a native Android application using Kotlin, Jetpack Compose,
Room, Hilt, coroutines, `Flow`, DataStore, WorkManager, and app-private media
storage. Android 12 is the minimum supported version.

Read these references before implementation:

- [Architecture overview](docs/architecture/keepfit-overview.md)
- [Data model](docs/architecture/keepfit-data-model.md)
- [Documentation guide](docs/README.md)
- [Forward roadmap](docs/ROADMAP.md)
- [Backlog index](docs/backlog/INDEX.md)
- [User manual](docs/user-manual/README.md)
- [Exercise catalogue rights register](docs/references/exercise-catalogue-rights-register.md)
- [AI agent rules](AGENTS.md)

## Product Direction

Forward planning follows this lifecycle:

```text
backlog initiative
  -> approved sprint version
  -> execution log
  -> immutable done record
```

Themes and sequencing live in [docs/ROADMAP.md](docs/ROADMAP.md). Historical
phase plans are intentionally not retained in the working tree; Git history is
the archive for superseded planning.

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
|   |-- review/
|   |-- assistant/
|   |-- settings/
|   |-- steps/
|   |-- transformation/
|   `-- workouts/
|-- docs/
|   |-- architecture/
|   |-- backlog/
|   |-- done/
|   |-- execution/
|   |-- references/
|   |-- user-manual/
|   `-- versions/
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

Coach contains no packaged provider key and does not read one from
`local.properties`. It remains inactive until a user explicitly authorizes
their own OpenRouter account from the system browser.

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

For a locally signed release build, create an ignored `keystore.properties`
file in the repo root and point it at an ignored `.jks` file:

```properties
storeFile=keepfit-release.jks
storePassword=your-password
keyAlias=keepfit
keyPassword=your-password
```

Then build:

```powershell
.\gradlew.bat :app:assembleRelease
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The signed release APK is written to:

```text
app/build/outputs/apk/release/app-release.apk
```

Run Room instrumentation tests on a connected Android emulator or device:

```powershell
.\gradlew.bat :core:database:connectedDebugAndroidTest
```

## User Guides

End-user setup and usage documentation lives in [docs/user-manual](docs/user-manual/README.md).

- [Getting started](docs/user-manual/getting-started.md)
- [Workouts](docs/user-manual/workouts.md)
- [Weekly review](docs/user-manual/weekly-review.md)
- [Nutrition](docs/user-manual/nutrition.md)
- [Progress](docs/user-manual/progress.md)
- [Settings, backup, and restore](docs/user-manual/settings-backup-and-restore.md)
- [Optional integrations](docs/user-manual/integrations.md)

## License

No license has been selected. Treat the repository as private unless a license
file is added explicitly.
