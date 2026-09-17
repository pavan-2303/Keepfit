# Keepfit

Keepfit is a private, local-first Android fitness tracker for personal use. It
is intended to provide a focused alternative to subscription-based fitness
apps: useful daily tracking without accounts, social feeds, or unnecessary
complexity.

## Product Capabilities

- Browse 1,316 bundled exercises with English instructions entirely offline,
  use original movement guidance for 25 common exercises, create personal
  exercises, attach private demonstration media, and read technique in a
  structured movement sheet without repeated legal text.
- Build workout templates and weekly workout plans.
- Log sets, repetitions, weight, notes, history, and personal records.
- Track personal foods, reusable meals, calories, and macronutrients.
- Record weight, BMI inputs, optional measurements, and transformation-cycle photos.
- Keep separate local profiles for family members while sharing the offline exercise and food catalogues.
- Compare transformation photos between cycle day 0 and later updates.
- Use device-dependent Android recovery for structured records and non-secret
  settings, or export and restore a complete encrypted local backup.
- Optional Health Connect step tracking and a user-authorized OpenRouter assistant.

## Project Status

The active development branch contains a runnable Android application with:

- a progressive first-run profile intake with optional birth date, height,
  starting weight, locally derived age/BMI context, and transactional Room
  persistence;
- a five-destination Compose Navigation shell organized as Today, Plan, Log,
  Progress, and Coach, with compact single-title headers and Settings behind
  the profile action;
- one searchable Room-owned library containing 1,316 pinned bundled exercises
  alongside editable user-created exercises and optional private demo media;
- structured exercise details with media-first guidance, a compact movement
  profile, numbered technique steps, and one consolidated About notice;
- offline search across exercise name, body area, equipment, target, secondary
  muscles, and English instructions, with no runtime catalogue provider;
- 25 original code-native movement figures with user-triggered animation from
  supported exercise details and active workouts;
- an offline guided setup that creates an editable starter week from a goal,
  experience, available days, time, equipment, exercises to avoid, and a
  maintainable nutrition-tracking preference;
- a final setup choice between the offline starter preview, fully manual Plan,
  or a validated, review-first AI draft in Coach;
- a weekly-plan-first Plan destination with navigable template, exercise
  catalogue, and history tools, including compact template details, direct
  name/membership/prescription maintenance, and atomic multi-template cleanup;
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
- transformation cycles with four default relaxed poses and eleven selectable
  profile-specific standard, flexed, or detailed poses;
- original neutral pose guidance, optional alignment lines, incomplete
  captured/enabled progress, and privacy-normalized private photo imports;
- cycle-day comparison with empty states when a photo is missing;
- cycle summaries with workout counts, nutrition averages, and weight change;
- categorized Settings for goals, training preferences, connections, private
  data, appearance/accessibility, and safety information;
- a shared training-field-guide visual system across Today, Plan, active
  workouts, Coach, and Settings, with restrained navigation/state transitions,
  success-only workout haptics, and an explicit reduced-motion preference;
- DataStore-backed settings persistence and WorkManager-based local reminder
  scheduling;
- encrypted backup export plus restore preview, checksum validation, and
  replace-data restore flow;
- device-dependent Android recovery allowlisted to the Room database and
  ordinary settings, with private media and secrets excluded;
- launcher branding from the repository logo asset plus an in-app Today header
  brand mark;
- an optional Today steps card backed by Health Connect availability checks,
  permission request flow, and daily plus seven-day step aggregates;
- persistent profile-owned Coach conversations with Mira, Rook, or Atlas,
  authorized through the user's own OpenRouter account, with bounded memory,
  Keystore-encrypted credentials, and automatic compact local-progress context
  for relevant questions; Keepfit adds no local request cap, and supported
  devices offer separate opt-in, verified Block Store credential recovery;
  choosing a Coach alone creates no history item, and sent prompts clear from
  the composer while remaining separately retryable after a failure;
- catalogue-backed AI workout drafts that use saved journey constraints,
  reject unknown exercises, and replace the active profile's week only after
  explicit atomic approval, plus review-first weekly and saved-food proposals;
- Hilt dependency injection;
- Room schema export with explicit migrations through schema version `14`;
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
[v0.9.0 - Bounded Coaching Workflows](docs/done/versions/v0.9.0.md). Slice I was
delivered in
[v0.10.0 - Guided Journey and Interface Reorganization](docs/done/versions/v0.10.0.md).
Completed follow-on work for recovery, multiple profiles, transformation pose
sets, an audited offline catalogue, persistent named Coaches, optional AI plan
creation, and visual polish is organized in the
[Durable Personalized Fitness Companion](docs/backlog/items/durable-personalized-fitness-companion.md)
initiative. Its first working increment protects local data across reinstall
without weakening the explicit encrypted-backup path and is complete in
[v0.11.0 - Reinstall-Safe Data Continuity](docs/done/versions/v0.11.0.md).
Local multi-profile isolation is complete in
[v0.12.0 - Isolated Local Profiles](docs/done/versions/v0.12.0.md). Flexible
transformation pose sets are complete in
[v0.13.0 - Flexible Transformation Pose Sets](docs/done/versions/v0.13.0.md).
The bundled offline exercise catalogue is complete in
[v0.14.0 - Bundled Offline Exercise Catalogue](docs/done/versions/v0.14.0.md).
Original offline movement guidance for its common core is complete in
[v0.15.0 - Owned Core Exercise Guidance](docs/done/versions/v0.15.0.md).
Progressive onboarding and manual planning are complete in
[v0.16.0 - Progressive Onboarding and Manual Planning](docs/done/versions/v0.16.0.md).
Persistent named Coaches are complete in
[v0.17.0 - Persistent Named Coaches](docs/done/versions/v0.17.0.md). Validated
AI plan review is complete in
[v0.18.0 - Validated AI Plan Review](docs/done/versions/v0.18.0.md). The shared
training-field-guide identity, accessible motion policy, and reduced-motion
setting are complete in
[v0.19.0 - Training Field Guide Polish](docs/done/versions/v0.19.0.md), closing
the Durable Personalized Fitness Companion initiative.
Apache-2.0 code licensing, explicit media boundaries, consolidated catalogue
notices, and the exercise-detail rebuild are complete in
[v0.20.0 - Exercise Clarity and Licence Boundaries](docs/done/versions/v0.20.0.md).
The compact release-candidate hierarchy, plan-first navigation, editable
template details, and truthful Coach conversation creation are complete in
[v0.21.0 - Release-candidate Navigation](docs/done/versions/v0.21.0.md).
Direct template actions, per-exercise set and repetition editing, and safe
multi-template deletion are complete in
[v0.22.0 - Practical Template Maintenance](docs/done/versions/v0.22.0.md).

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

Keepfit source code is available under the
[Apache License 2.0](LICENSE). This does not automatically license third-party
catalogue content, trademarks, user-imported media, or future media explicitly
identified as restricted.

- [Media licensing boundaries](MEDIA-LICENSE.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)
- [Exercise catalogue rights register](docs/references/exercise-catalogue-rights-register.md)
