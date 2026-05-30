# Keepfit Agent Rules

This file contains binding instructions for AI agents working in this
repository. Follow these rules unless the user explicitly overrides them.

## 1. Read Before Editing

Before making implementation decisions, read:

1. [README.md](README.md)
2. [Architecture overview](docs/architecture/keepfit-overview.md)
3. [Data model](docs/architecture/keepfit-data-model.md)
4. [Delivery roadmap](docs/architecture/keepfit-roadmap.md)

Inspect the current repository state before editing. Do not assume a planned
module, dependency, script, or Android scaffold already exists.

## 2. Product Intent

Keepfit is a simple private Android fitness tracker for one local user. The MVP
must work without an account, backend, internet connection, or paid external
service.

Prefer the smallest complete workflow that is useful in daily life. Do not add
social feeds, subscriptions, cloud sync, analytics, remote food catalogs,
barcode lookup, or advanced lifting metrics unless the user requests a roadmap
change.

## 3. Architecture Rules

- Use native Android with Kotlin and Jetpack Compose.
- Support Android 12 and newer.
- Use Room as the authoritative store for structured fitness data.
- Use app-private storage for imported exercise media and transformation
  photos. Store stable relative paths in Room, not raw file contents.
- Use DataStore for simple preferences and Android secure credential storage
  for secrets.
- Keep screens state-driven: Compose UI calls ViewModels, ViewModels call
  repositories, repositories call DAOs or focused adapters.
- Expose observable data with coroutines and `Flow`.
- Keep feature modules independent. A `feature:*` module must not directly
  depend on another `feature:*` module.
- Put genuinely shared behavior in a focused `core:*` module or coordinate it
  through `app`.
- Keep Health Connect and Ollama behind optional adapters. Core workflows must
  build, run, and test without them.

When the Android scaffold exists, preserve the module boundaries defined in
[keepfit-overview.md](docs/architecture/keepfit-overview.md). Introduce modules
incrementally when a roadmap phase needs them; do not create empty placeholder
modules.

## 4. Privacy and Safety Rules

- Do not add telemetry, analytics, crash reporting, remote logging, or network
  calls without explicit user approval.
- Do not upload transformation photos, body measurements, nutrition logs, or
  workout history implicitly.
- Do not hardcode API tokens, endpoint credentials, passphrases, or private
  paths.
- Do not store backup passphrases.
- Validate backup archives and checksums before replacing local data.
- Preserve existing local data until restore succeeds.
- Preserve existing media until replacement imports succeed.
- Treat BMI and assistant responses as general fitness context, not medical
  advice.

## 5. Data Model Rules

- Use UUID strings for persisted identifiers.
- Store timestamps as UTC epoch milliseconds.
- Store calendar dates as ISO-8601 local-date values.
- Archive exercises, foods, and workout templates when history references
  them. Do not break historical logs through hard deletion.
- Derive totals, BMI, and personal records from source data where the data model
  specifies derived values.
- Increment the Room schema version for each schema change.
- Export Room schemas into version control and add migration tests.
- Version the encrypted-backup manifest independently of the Room schema.

If implementation needs a schema change that conflicts with the documented
model, update the architecture docs in the same change and explain why.

## 6. Delivery Rules

Follow [keepfit-roadmap.md](docs/architecture/keepfit-roadmap.md) in order:

1. Build the Phase 0 foundation.
2. Finish and verify each Phase 1 increment before expanding the MVP.
3. Add Health Connect steps only after the offline MVP is dependable.
4. Add Ollama only after tracking data is stable and only as an optional
   feature.

Each implementation change must leave the application runnable. Avoid
placeholder screens, speculative abstractions, and unused dependencies.

## 7. Testing Rules

Use test-driven development for features and bug fixes:

1. Add or update a test that demonstrates the expected behavior.
2. Run it and confirm it fails for the expected reason.
3. Implement the smallest complete change.
4. Run the focused test until it passes.
5. Run the broader relevant suite before reporting completion.

Minimum expectations:

- Unit tests for repositories, calculations, scheduling, and backup validation.
- Room instrumentation tests for DAO behavior and migrations.
- Compose UI tests for primary logging, restore, and comparison workflows.
- Fake adapters for Health Connect and Ollama.
- Manual emulator verification for user-facing Android flows.

Do not claim a build, test, or lint result unless the corresponding command was
run successfully in the current work session.

## 8. Documentation Rules

- Keep [README.md](README.md) accurate as setup and build commands are added.
- Update architecture docs when changing product boundaries, module
  responsibilities, storage behavior, external integrations, or persisted
  schema.
- Keep docs concise and use ASCII unless an existing file requires otherwise.
- Do not add speculative version numbers or dates to roadmap items without user
  approval.

## 9. Git and Editing Rules

- Inspect `git status` before editing and before reporting completion.
- Preserve user changes. Do not revert unrelated edits.
- Keep changes scoped to the requested phase or feature.
- Do not use destructive Git commands such as `git reset --hard`.
- Do not commit generated local files, secrets, media, backups, or Android
  signing material.
- Use small, descriptive commits when the user asks for commits.

When the Android scaffold is added, create and maintain a `.gitignore` for
Gradle output, IDE metadata, local SDK paths, signing files, imported media,
backups, and secrets.

## 10. Completion Checklist

Before reporting work complete:

1. Re-read the requested scope and relevant roadmap phase.
2. Review the final diff and confirm unrelated files were not changed.
3. Run the relevant tests, lint checks, and build commands.
4. Confirm documentation is still accurate.
5. Report the files changed, verification commands run, and any remaining
   limitations.
