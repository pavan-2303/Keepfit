# Keepfit Android Architecture Overview

## 1. Purpose

Keepfit is a private Android fitness tracker for people sharing one device. It replaces several
complex subscription applications with a focused offline experience:

- create exercises with optional animated demonstrations;
- assemble workout templates and weekly plans;
- create an editable offline starter week from practical constraints;
- complete a progressive local intake and choose offline, manual, or validated
  review-first Coach planning support;
- log sets, repetitions, weight, notes, and personal progress;
- track meals, calories, and macronutrients;
- record measurements and transformation-cycle photos;
- compare transformation photos between selected cycle days;
- export and restore a private backup.

The first release must work without an account, backend, or internet connection.
Step tracking and an AI assistant are optional follow-up features.

## 2. Product Boundaries

### Included in the first release

- Multiple isolated local profiles for people sharing one device.
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
| `core:media` | Import, validate, store, retrieve, export, and restore private media; own rights-recorded exercise-guidance data |
| `core:preferences` | DataStore-backed app settings and reminder scheduling |
| `core:designsystem` | Field-guide theme, reusable Compose components, application icons, and reduced-motion policy |
| `feature:workouts` | Guided starter-week setup, unified offline exercise library, templates, plans, workout sessions, history, records, timer |
| `feature:nutrition` | Selectable nutrition depth, personal foods, saved meals, diary entries, meal-quality check-ins, ranges, and daily totals |
| `feature:review` | Offline weekly evidence, motivation rules, bounded coming-week drafts, review decisions, and focused Compose flow |
| `feature:transformation` | Measurements, transformation cycle photo capture/import, and comparison |
| `feature:settings` | User goals, reminder preferences, backup export, and restore |
| `feature:steps` | Phase-2 Health Connect availability, permission, and daily plus seven-day step summaries |
| `feature:assistant` | Optional OpenRouter authorization, profile-owned named Coach conversations, bounded memory, selective local-summary context, strict catalogue-backed plan contracts, and local safety/privacy controls |

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
core:database     -> bundled normalized exercise catalogue asset
```

Feature modules must not depend on each other directly. Shared behavior belongs
in a focused `core:*` module or is coordinated by `app`.

### Visual and motion system

`core:designsystem` owns a code-native field-guide identity built around one
field-green or action-colored pace line, strong left-aligned hierarchy, quiet
paper and white surfaces, and sentence-case status language. Shared pace-card,
section-header, and status-mark components are preferred over screen-local
hero treatments, gradients, remote fonts, or grids of equally prominent cards.

Short native fades may communicate destination or state changes. A completion
haptic is emitted only after the authoritative workout state changes to
completed. The shell provides the device-level reduced-motion preference to
feature content; reduced mode removes nonessential transitions while retaining
the complete static state. Android's system animation scale continues to govern
the remaining native transitions.

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
writes only the outcome. The pause toggle and device-level reduced-motion
choice are simple DataStore preferences.

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

Keepfit also includes a first owned guidance pack for 25 stable bundled
exercise UUIDs. `core:media` holds its movement, cue, rights, and normalized
start/finish pose data. `feature:workouts` draws the figures with Compose Canvas
and animates them only after a user requests replay. The code-native figures
remain legible without motion, add no bitmap or video asset, and are available
from exercise details and the active workout without network access.

The Exercises tab presents one Room-owned library. On first open after install
or migration, `core:database` transactionally seeds 1,316 normalized records
from a pinned bundled asset and records the source revision in a catalogue
ledger. Existing rows and user edits are never overwritten. Bundled and
user-created exercises share the same template, plan, history, archive, and
private-media workflows.

Search runs only in Room across names, body areas, equipment, target muscles,
secondary muscles, and English instructions. The upstream Gym images, GIFs,
media identifiers, paths, and URLs are excluded. No exercise search term or
fitness record leaves the device. Dataset provenance is shown in exercise
details and the full copyright and MIT notice is available in Settings.

### Transformation photos

Transformation photos belong to one profile and use stable keys from the
15-pose catalogue. Four relaxed poses are always enabled; each profile may
enable any additional standard, flexed, or detailed pose. The first import
starts a cycle, incomplete check-ins remain valid, and replacing a pose on the
same date retains the old file until the processed replacement and Room row
succeed. Imports are orientation-corrected, re-encoded without source EXIF
metadata, and bounded to a 2048-pixel maximum edge before entering app-private
storage. The comparison view loads the identical pose key from two selected
cycle days. A missing counterpart names the date and pose without blocking
other comparisons.

Reference figures and alignment guides are original Compose drawings bundled
with the application. They are framing aids rather than appearance targets;
the product-review poster is not redistributed.

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
the application sandbox. Android platform backup allowlists the Room database
and non-secret DataStore settings for device-dependent cloud backup and
device-to-device transfer. It excludes app-private media and all shared
preferences, including assistant credentials and authorization state. Platform
backup timing, availability, quota, and restoration are not guaranteed, so the
settings screen must keep encrypted backup export easy to find.

One installation can contain several local body profiles. The active profile
is observable app state, not an account or remote identity. Feature repositories
scope personal reads and mutations at their DAO boundary, while the exercise
and food reference catalogues remain shared. Switching profiles updates open
screens without restarting the process.

OpenRouter access recovery is a separate, disabled-by-default Google Block
Store option. It stores one provider token only after explicit consent, asks
for cloud recovery only when end-to-end encryption is available, and validates
retrieved bytes with OpenRouter before writing them into Keepfit's
Keystore-backed credential store. Unsupported services, missing data, invalid
tokens, and provider failures fall back to reconnecting and never block local
fitness workflows.

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

Exercise browsing is a core offline capability, not an optional integration.
The checked-in import inputs, deterministic transformation, audit report, and
rights decision are maintained in the
[exercise catalogue register](../references/exercise-catalogue-rights-register.md).
Refreshing the catalogue requires an explicit source audit and a new migration
or revision-aware seed; the application performs no runtime catalogue request.

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
Keepfit's encrypted fitness backup or Android platform backup. Connecting the
account is the opt-in; there is no second enable toggle or Keepfit-maintained
request ledger.

Settings may offer a separate `Recover access after reinstall` switch when
Google Block Store is supported. Consent is stored as a non-secret preference;
the token remains in Block Store rather than Room, DataStore, the fitness
archive, or the Android backup allowlist. Disconnect and opt-out erase local
consent and request deletion of the keyed recovery entry.

The private Android build starts an ephemeral HTTP receiver on `127.0.0.1`,
generates a random verifier and state, and opens OpenRouter authorization in the
system browser. This follows OpenRouter's documented localhost flow for
local-first clients without adding a Keepfit backend. The one-time callback is
accepted only while the matching encrypted transaction is current. Public
distribution requires an owned HTTPS domain and verified Android App Link.

Each profile-owned conversation selects
Mira (warm), Rook (direct), or Atlas (analytical). Room retains the visible
transcript, while each provider request is bounded to the selected local Coach
instruction, a deterministic capped recap, and the latest 12 messages after
the user's most recent clear-memory action. The user can switch, rename, clear
memory, or delete conversations. Deleting a profile cascades through its Coach
history.

A local context policy adds compact workout, nutrition, step, and progress
aggregates only when the question refers to the user's own history. Context use
is disclosed in the conversation. Coach also exposes review-first weekly-plan,
schedule, and saved-food contracts. These remain previews until an explicit
approval invokes the focused app-level command.

Remote coaching prompts contain short-lived aliases and bounded display labels
rather than private Room identifiers. AI plan creation is the narrow exception:
it sends at most 60 stable IDs and labels from the public bundled exercise
catalogue so returned selections can be resolved exactly. Custom-exercise,
profile, template, workout, nutrition, and progress identifiers are never sent.
The validated proposal shows observed evidence, current
state, proposed state, and reason. Preview, edit, and dismiss write nothing;
only an explicit approval invokes a focused app-level command. Schedule and
multi-food changes validate all referenced local records before their atomic
repository write.

Plan creation uses the active profile's locally saved goal, experience,
preferred days, session length, and equipment to build a bounded request. One
strict tool contract accepts only supplied bundled exercise IDs, unique selected
weekdays, and bounded targets. Keepfit resolves names locally and rejects extra
fields, duplicates, unknown IDs, and invalid targets. Approval rechecks every
catalogue record and replaces only the active profile's weekly plan, generated
templates, exact targets, and assignments in one Room transaction. A dismissed,
invalid, or failed draft leaves the existing plan unchanged.

A local safety gate refuses diagnosis, rehabilitation, medication, extreme
dieting, and unsafe progression before quota reservation or network dispatch.
The assistant must present fitness suggestions as general guidance, not
diagnosis or medical advice.

No AI dependency is permitted in core tracking flows. The user must initiate
every remote request. The OpenRouter adapter uses the named free evaluation
model and requires zero-data-retention routing and denial of provider data
collection. Keepfit does not cap requests; OpenRouter and the selected provider
own account, rate, free-tier, and credit limits. Body weight, height, BMI,
photos, measurements, private identifiers, notes, paths, and raw records are
not assembled into remote prompts. The planning exception is limited to public
bundled-catalogue identifiers and labels. The typed task and latest
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
| Plan | Starter journey, weekly schedule, templates, unified offline exercise library, and adjustments |
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
  support, empty exercise searches, and disabled AI.
- Validate a backup completely before replacing current data.
- Keep the previous database and media until restore succeeds.

## 9. Quality Strategy

- Unit test repositories, daily totals, plan scheduling, personal-record
  calculations, BMI calculations, and backup manifest validation.
- Use Room instrumentation tests for DAO queries and migrations.
- Use Compose UI tests for the primary logging and comparison workflows.
- Use fake adapters for Health Connect, OpenRouter, and Ollama compatibility so optional integrations do
  not make core tests depend on device services or a network.
- Verify catalogue import determinism, Room seeding, migration, metadata search,
  provenance disclosure, and lazy-list scale without a network.

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
- [Exercises Dataset](https://github.com/hasaneyldrm/exercises-dataset)
- [Coil GIF support](https://coil-kt.github.io/coil/gifs/)
- [Android Media3](https://developer.android.com/media/media3/exoplayer/hello-world)
