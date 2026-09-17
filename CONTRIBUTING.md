# Contributing to Keepfit

Thank you for helping improve Keepfit. The project values small, complete
changes that preserve its offline-first privacy model.

## Before You Start

- Search existing issues before opening a new one.
- Use an issue for substantial behavior or architecture changes before writing
  the implementation.
- Do not include personal fitness records, API keys, signing files, backups,
  transformation photos, or proprietary exercise media in an issue or commit.
- Read [MEDIA-LICENSE.md](MEDIA-LICENSE.md) and
  [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) before contributing content.

## Development Setup

Keepfit requires JDK 21, Android SDK platform 36, and build tools 36.0.0. Put
your local SDK path in the ignored `local.properties` file.

Run the standard checks from PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Device tests require a connected Android emulator or device. Run the relevant
feature suite for the area you change, for example:

```powershell
.\gradlew.bat :feature:workouts:connectedDebugAndroidTest
```

## Change Workflow

1. Branch from `develop`.
2. Add or update a test that demonstrates the intended behavior.
3. Make the smallest complete implementation.
4. Run the focused tests and the standard checks.
5. Open a pull request into `develop` and complete the checklist.

The `main` branch is reserved for reviewed release states.

## Project Boundaries

- Core tracking must continue to work without an account, backend, or network.
- Do not add analytics, telemetry, crash reporting, or remote logging without
  an approved product decision.
- Keep optional network providers behind adapters and user initiation.
- Treat fitness guidance as general information, not medical advice.
- Preserve historical records and documented module boundaries.

## Licensing

By contributing source code, documentation, or other material, you agree that
your contribution is licensed under the repository's Apache-2.0 license unless
an accepted adjacent notice explicitly states otherwise. Do not submit content
you do not have permission to contribute.
