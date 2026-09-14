# Keepfit Android Architecture Overview

## 1. Purpose

Keepfit is a private Android fitness tracker for one person. It replaces several
complex subscription applications with a focused offline experience:

- create exercises with optional animated demonstrations;
- assemble workout templates and weekly plans;
- create an editable offline starter week from practical constraints;
- log sets, repetitions, weight, notes, and personal progress;
- track meals, calories, and macronutrients;
- record measurements and transformation-cycle photos;
- compare transformation photos between selected cycle days;
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
- Social feeds, friends, leaderboards, and any mandatory remote exercise
  catalog.
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
| `feature:workouts` | Guided starter-week setup, source-aware exercise catalogue, personal library, templates, plans, workout sessions, history, records, timer |
| `feature:nutrition` | Selectable nutrition depth, personal foods, saved meals, diary entries, meal-quality check-ins, ranges, and daily totals |
| `feature:review` | Offline weekly evidence, motivation rules, bounded coming-week drafts, review decisions, and focused Compose flow |
| `feature:transformation` | Measurements, transformation cycle photo capture/import, and comparison |
| `feature:settings` | User goals, reminder preferences, backup export, and restore |
| `feature:steps` | Phase-2 Health Connect availability, permission, and daily plus seven-day step summaries |
| `feature:assistant` | Optional OpenRouter authorization, query-only Coach chat, selective local-summary context, and local safety/privacy controls |

For the first implementation increment, modules may be introduced as features
are built. The dependency direction remains fixed:

```text
app -> feature:* -> core:model
                -> core:database
                -> core:media
                -> core:designsystem

feature:steps     -> Health Connect SDK
app               -> feature:review WeeklyActivityProvider -> feature:steps
feature:assistant -> OpenRouter OAuth and chat APIs
feature:workouts  -> optional ExerciseCatalogProvider adapter
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

Nutrition depth and target flexibility are local DataStore preferences. The
default detailed mode preserves the existing experience; calorie/protein mode
hides the unused macro detail, meal-quality mode writes one replaceable Room
check-in per date and meal, and disabled mode hides Today nutrition prompts.
Switching modes never deletes foods, meals, diary entries, goals, or check-ins.
Target bands are derived from saved goal midpoints using the selected 5%, 10%,
or 15% flexibility.

Saved meals expand into ordinary diary rows. Repeating yesterday's selected
meal replaces only that target meal in one Room transaction, and an empty
source cannot erase the target. Copied rows keep their own servings and food
references, so later saved-meal item changes do not rewrite prior diary rows.

Weekly review follows the same boundary. `feature:review` reads workout,
nutrition, and review-outcome DAOs, while an app-level `WeeklyActivityProvider`
supplies optional Health Connect aggregates without creating a feature-to-
feature dependency. Opening or editing a review is read-only. Approval writes
one dated workout occurrence and one review outcome in a transaction; dismissal
writes only the outcome. The pause toggle is a simple DataStore preference.

### Dated workout decisions

The recurring weekly plan remains the reusable source of intent. A confirmed
Today change creates a separate dated workout occurrence with an exercise
snapshot, so shortening, minimum sessions, substitutions, rescheduling, and
skipping never rewrite the source template. Preview state is kept in memory and
writes nothing until explicit confirmation.

Today resolves unfinished sessions first, then completion, dated occurrences,
the recurring plan, and finally a rest-day state. It may also offer the most
recent unresolved workout from the previous seven local dates as a secondary
recovery action. Starting a dated occurrence copies its reviewed exercise
snapshot into the existing workout-session flow while preserving the
single-active-session rule.

### Exercise demonstrations

Exercise animations can be user-imported MP4, WebM, or GIF files. `core:media`
copies an approved document URI into app-private storage and stores only a
stable relative path in Room. The exercise editor can replace or remove an
attachment without changing exercise history.

Short videos should be preferred over GIF files because they are generally
smaller and more efficient to play. The personal exercise detail uses Android
Media3 for private video and Coil for private GIF playback.

The Exercises tab has a provenance rail for the personal library, a 40-item
Keepfit offline guide, and a live ExerciseDB prototype. The offline definitions
are deterministic bundled content and become ordinary editable Room exercises
only after an explicit add action. `feature:workouts` consumes a provider-
independent `ExerciseCatalogProvider`; the AscendAPI adapter owns network
requests, URL construction, response mapping, timeouts, and provider failures.
Coil renders live GIF demonstrations only after a deliberate search/open action.

Remote results are not authoritative fitness records. They remain in screen
memory, expose no import action, use disabled memory and disk caches for media,
and are absent from Room, app-private files, DataStore, logs, and backups. Do
not persist remote metadata or media unless the provider's plan explicitly
grants commercial, attribution, caching, and local-storage rights. Media URL
rotation and provider failure produce recoverable UI states. Only entered
catalogue search/filter terms leave the device; no workout history, profile
data, or other private fitness data is sent.

### Transformation photos

Transformation photos are imported or captured for a transformation cycle and a
fixed angle: front, left, right, or back. The first imported batch starts a
cycle. Later uploads can happen on any date in that cycle, and uploading the
same angle again on the same day replaces the earlier photo. Files live in
app-private storage. The comparison view loads the same angle from two selected
cycle days side by side. Missing angles show an empty state instead of blocking
comparison.

### Private file layout

```text
files/
  media/
    exercises/<exercise-media-id>.<extension>
    transformation/<cycle-id>/<photo-id>.<extension>
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

### Exercise catalogue

The private prototype uses the keyless AscendAPI ExerciseDB V1 endpoint. It
remains behind `ExerciseCatalogProvider` so provider selection, licensing, or
availability can change without changing workout domain behavior.

The integration must:

- require network access only while the user opens or searches the online
  catalogue;
- show provider attribution and media availability honestly;
- avoid embedding a shared provider credential in the Android package;
- avoid persistent caching or imports until the provider grants the required
  rights; and
- keep the owned offline starter catalogue, custom exercises, local media, and
  workout logging fully functional when disabled or unavailable.

Current provider caching rules do not establish sufficient rights for durable
storage or public release. Live results are view-only and a current rights
review is maintained in the
[exercise catalogue register](../references/exercise-catalogue-rights-register.md).

### Health Connect steps

`feature:steps` is a phase-2 adapter. It must:

- detect whether Health Connect is available on the device;
- request read permission for step data only;
- read daily totals and seven-day aggregates and expose an unavailable state
  when unsupported or denied;
- keep the dashboard functional when the integration is disabled.

Steps are a supplementary dashboard metric. They are not required for workout
or nutrition tracking.

### OpenRouter assistant

`feature:assistant` is an optional adapter over OpenRouter OAuth and chat APIs.
A dedicated credential store encrypts the user-controlled token and an active
PKCE transaction with an Android Keystore AES/GCM key. It is not part of
Keepfit's encrypted fitness backup, and Android platform backup remains
disabled. Connecting the account is the opt-in; there is no second enable
toggle or Keepfit-maintained request ledger.

The private Android build starts an ephemeral HTTP receiver on `127.0.0.1`,
generates a random verifier and state, and opens OpenRouter authorization in the
system browser. This follows OpenRouter's documented localhost flow for
local-first clients without adding a Keepfit backend. The one-time callback is
accepted only while the matching encrypted transaction is current. Public
distribution requires an owned HTTPS domain and verified Android App Link.

The shipped Coach UI is query-only. It answers ordinary questions and uses a
local context policy to add compact workout, nutrition, step, and progress
aggregates only when the question refers to the user's own history. Context use
is disclosed in the conversation. Mutation-capable coaching contracts remain
inaccessible from the UI for later evaluation.

Remote prompts contain short-lived aliases and bounded display labels rather
than Room identifiers. The validated proposal shows observed evidence, current
state, proposed state, and reason. Preview, edit, and dismiss write nothing;
only an explicit approval invokes a focused app-level command. Schedule and
multi-food changes validate all referenced local records before their atomic
repository write.

A local safety gate refuses diagnosis, rehabilitation, medication, extreme
dieting, and unsafe progression before quota reservation or network dispatch.
The assistant must present fitness suggestions as general guidance, not
diagnosis or medical advice.

No AI dependency is permitted in core tracking flows. The user must initiate
every remote request. The OpenRouter adapter uses the named free evaluation
model and requires zero-data-retention routing and denial of provider data
collection. Keepfit does not cap requests; OpenRouter and the selected provider
own account, rate, free-tier, and credit limits. Body
weight, height, BMI, photos, measurements, identifiers, notes, paths, and raw
records are not assembled into remote prompts. The typed task and latest
validated proposal are encrypted with a separate Android Keystore key so they
survive recreation; this local draft store is excluded from fitness backups.

The older Ollama adapter remains source-compatible for migration tests but is
not the bound runtime provider and receives no packaged credential.

## 7. Navigation

Use a bottom navigation bar with five destinations. Settings is reached from a
consistent profile action instead of competing with daily workflows:

| Destination | Main content |
| --- | --- |
| Today | One primary workout action, weekly-review entry, missed-workout recovery, mode-specific nutrition summary/action, reminders, and optional steps |
| Plan | Starter journey, weekly schedule, templates, exercises, offline guide, live demo prototype, and adjustments |
| Log | Workout history plus nutrition diary, foods, saved meals, and reuse actions |
| Progress | Weekly review, records, measurements, transformation cycles, photo comparison, and step context |
| Coach | General questions and read-only insights over selectively included local progress aggregates |

The active workout screen is a dedicated focused flow launched from Today or
Workouts. Room retains the active session, target snapshots, logged sets, and
session-only adaptations across interruption. Android saved state retains the
rest-timer deadline and open dialog choices where the platform permits. Each
successful set starts the configured local timer; failed validation or writes
do not. Previous values and deterministic progression suggestions are display
context only and never rewrite a template or insert future sets.

The weekly review is a dedicated focused flow launched from Today. It reviews
the most recently completed Monday-Sunday, compares the prior week, and shows a
seven-day ribbon plus local achievements and available trends. Deterministic
rules produce no more than two recovery, schedule, or volume drafts. Only an
explicit approval creates a dated occurrence in the coming week; the recurring
plan, reusable template, history, and later weeks remain unchanged.

Nutrition contributes only evidence appropriate to its active lens: numeric
logging consistency for detailed or calorie/protein modes, check-in
consistency for meal-quality mode, and no signal when nutrition is disabled.

## 8. Error Handling

- Reject unsupported or unreadable media imports with a clear message.
- Preserve existing media until a replacement file is copied successfully.
- Validate required numeric values before saving; allow zero weight for
  bodyweight exercises.
- Show recoverable empty states for missing photos, missing Health Connect
  support, unavailable exercise catalogues, and disabled AI.
- Validate a backup completely before replacing current data.
- Keep the previous database and media until restore succeeds.

## 9. Quality Strategy

- Unit test repositories, daily totals, plan scheduling, personal-record
  calculations, BMI calculations, and backup manifest validation.
- Use Room instrumentation tests for DAO queries and migrations.
- Use Compose UI tests for the primary logging and comparison workflows.
- Use fake adapters for Health Connect, OpenRouter, and Ollama compatibility so optional integrations do
  not make core tests depend on device services or a network.
- Use a fake exercise catalogue provider for search, attribution, malformed
  response, missing media, timeout, and offline tests.

## 10. References

- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Room persistence library](https://developer.android.com/training/data-storage/room)
- [App-specific storage](https://developer.android.com/training/data-storage/app-specific)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Health Connect availability](https://developer.android.com/health-and-fitness/health-connect/availability)
- [Read Health Connect data](https://developer.android.com/health-and-fitness/health-connect/read-data)
- [OpenRouter OAuth PKCE](https://openrouter.ai/docs/guides/overview/auth/oauth)
- [OpenRouter current-key endpoint](https://openrouter.ai/docs/api/api-reference/api-keys/get-current-key)
- [OpenRouter provider routing](https://openrouter.ai/docs/guides/routing/provider-selection)
- [AscendAPI ExerciseDB V1](https://docs.ascendapi.com/products/edb-v1/overview)
- [AscendAPI caching policy](https://docs.ascendapi.com/guides/caching)
- [Coil GIF support](https://coil-kt.github.io/coil/gifs/)
- [Android Media3](https://developer.android.com/media/media3/exoplayer/hello-world)
