# Keepfit

[![Android CI](https://github.com/pavan-2303/Keepfit/actions/workflows/android-ci.yml/badge.svg?branch=main)](https://github.com/pavan-2303/Keepfit/actions/workflows/android-ci.yml)
[![Latest release](https://img.shields.io/github/v/release/pavan-2303/Keepfit)](https://github.com/pavan-2303/Keepfit/releases/latest)
[![License](https://img.shields.io/github/license/pavan-2303/Keepfit)](LICENSE)

Keepfit is a private, local-first fitness tracker for Android. It brings
workout planning, nutrition, progress photos, body measurements, and optional
AI coaching into one app without requiring a Keepfit account or backend.

> No account. No social feed. No analytics. Your fitness records stay on your
> device unless you explicitly export or share them.

## What You Can Do

### Plan and complete workouts

- Build a weekly schedule from reusable workout templates.
- Start with an offline plan based on your goal, experience, available days,
  session length, equipment, and exercises to avoid.
- Browse 1,316 bundled exercises offline or add your own exercises and private
  demonstration media.
- Log sets, repetitions, weight, notes, rest periods, and completion feedback.
- Adapt a single workout by shortening, substituting, rescheduling, or choosing
  a minimum session without changing the reusable plan.
- Review workout history, personal records, and practical progression ideas.

### Track nutrition at your preferred depth

- Choose detailed macros, calories and protein, simple meal-quality check-ins,
  or disable nutrition tracking.
- Create personal foods and reusable meals.
- Log breakfast, lunch, dinner, and snacks with favorites, recent foods,
  serving presets, and repeat shortcuts.
- Compare daily totals with flexible goal ranges.

### Follow your progress

- Record weight and optional body measurements, with BMI shown only as general
  context.
- Maintain transformation-photo cycles using four standard poses and up to
  eleven optional poses.
- Compare matching poses between dates using private, metadata-normalized
  photos.
- Use a short weekly review to understand consistency and approve small changes
  to the coming week.

### Share one device safely

- Create separate local profiles for family members or friends.
- Keep each profile's plans, logs, measurements, photos, and Coach
  conversations isolated.
- Switch profiles from the profile action without creating online accounts.

## How the App Is Organized

| Destination | Purpose |
| --- | --- |
| **Today** | See the next useful action, start or resume a workout, recover a missed session, and view daily nutrition or step context. |
| **Plan** | Maintain the weekly schedule, templates, exercise catalogue, and workout history. |
| **Log** | Record meals and manage foods or saved meals. |
| **Progress** | Review trends, measurements, records, and transformation photos. |
| **Coach** | Ask general fitness questions or request insights based on selected local progress summaries. |

## Install

Keepfit supports Android 12 and newer.

1. Download the signed APK from the
   [latest GitHub release](https://github.com/pavan-2303/Keepfit/releases/latest).
2. Open the APK on your phone.
3. Allow installation from your browser or file manager if Android asks.

Keepfit is not currently distributed through an app store. Release notes
include the package version and SHA-256 checksum. Back up important local data
before uninstalling the app or replacing it with a build signed by a different
certificate.

## Your Data and Privacy

Core tracking works offline. Structured fitness data is stored in Room, while
photos and imported exercise media remain in app-private storage. Keepfit does
not include telemetry, advertising, crash reporting, or remote logging.

You can create an encrypted backup containing profiles, records, settings, and
private media. Keepfit validates a backup before replacing current data and
never stores the backup passphrase. Android may separately restore supported
structured records and ordinary settings through device-dependent platform
backup; secrets and private media are excluded from that path.

Read the full [privacy overview](PRIVACY.md) and
[backup guide](docs/user-manual/settings-backup-and-restore.md).

## Optional Integrations

- **Health Connect:** reads step totals only after permission is granted. Step
  tracking is optional and does not affect core fitness workflows.
- **OpenRouter Coach:** connects through the user's own OpenRouter account.
  Credentials are encrypted with Android Keystore, and Keepfit does not package
  a provider key or impose its own usage quota. Remote requests occur only when
  the user initiates them.

Coach responses and BMI are general fitness context, not medical advice. See
the [integration guide](docs/user-manual/integrations.md) for privacy and setup
details.

## User Guide

- [Getting started](docs/user-manual/getting-started.md)
- [Workouts](docs/user-manual/workouts.md)
- [Nutrition](docs/user-manual/nutrition.md)
- [Weekly review](docs/user-manual/weekly-review.md)
- [Progress and transformation photos](docs/user-manual/progress.md)
- [Settings, backup, and restore](docs/user-manual/settings-backup-and-restore.md)
- [Optional integrations](docs/user-manual/integrations.md)

## For Contributors

Keepfit is a native Android application built with Kotlin, Jetpack Compose,
Room, Hilt, coroutines, Flow, DataStore, and WorkManager. Development requires
JDK 21, Android SDK platform 36, and build tools 36.0.0.

Create an ignored `local.properties` containing your Android SDK path, then use
the Gradle wrapper:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

New development targets `develop`; `main` represents the latest reviewed
release state. Start with the [contribution guide](CONTRIBUTING.md), then use
the focused technical references when needed:

- [Architecture overview](docs/architecture/keepfit-overview.md)
- [Data model](docs/architecture/keepfit-data-model.md)
- [Documentation lifecycle](docs/README.md)
- [Roadmap](docs/ROADMAP.md)
- [Exercise catalogue rights register](docs/references/exercise-catalogue-rights-register.md)

## Community, Security, and Licensing

- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Security policy](SECURITY.md)
- [Changelog](CHANGELOG.md)
- [Media licensing boundaries](MEDIA-LICENSE.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)

Keepfit source code is available under the [Apache License 2.0](LICENSE).
Third-party catalogue content, trademarks, user-imported media, and any
separately identified restricted media remain subject to their own terms.
