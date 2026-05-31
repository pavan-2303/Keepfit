# Keepfit Delivery Roadmap

## 1. Delivery Principles

Keepfit should become useful early and stay simple. Build the offline tracker
first, then add optional integrations without coupling them to core workflows.

Each increment must leave a runnable Android application with a focused,
testable improvement. Avoid placeholder screens for later features.

## 2. Phase 0: Android Foundation

Status: implemented and emulator-verified.

### Goal

Create the Android project and prove the local-first architecture.

### Deliverables

- Kotlin Android application with Android 12+ minimum support.
- Jetpack Compose, Compose Navigation, Material 3, Hilt, Room, DataStore,
  coroutines, `Flow`, and WorkManager.
- The module boundaries from
  [keepfit-overview.md](./keepfit-overview.md), introduced incrementally.
- Bottom navigation for Today, Workouts, Nutrition, Progress, and Settings.
- Room schema export and migration-test setup.
- A single local body profile created during first-run setup.

### Exit criteria

- App launches on an Android 12 emulator and a current Android emulator.
- First-run profile creation survives an app restart.
- Navigation destinations render working empty states.
- Unit and instrumentation test suites run in CI or from Gradle locally.

## 3. Phase 1A: Workout Tracker

Status: implemented and emulator-verified.

### Goal

Replace a basic gym notebook with a fast offline workout workflow.

### Deliverables

- Exercise library with create, edit, archive, search, and local animation
  import.
- Workout templates with ordered exercises, target sets, target reps, and
  notes.
- Active weekly plan with templates assigned to weekdays.
- Today screen showing the planned workout.
- Focused workout-session screen with sets, repetitions, weight, and notes.
- Previous logged values shown while entering a set.
- Completed workout calendar, exercise history, and simple personal records.
- Optional rest timer with vibration.

### Exit criteria

1. Create an exercise with an imported offline animation.
2. Add the exercise to a workout template and schedule it for a weekday.
3. Start the workout from Today, record sets, and complete the session.
4. Start the same workout again and see the previous values.
5. Open history and see the completed session and derived personal record.
6. Archive the exercise and confirm historical logs remain readable.

## 4. Phase 1B: Nutrition Diary

Status: implemented and emulator-verified.

### Goal

Track daily intake without depending on a remote food catalog.

### Deliverables

- Personal food library with serving label, calories, protein, carbohydrates,
  fat, favorites, recents, and archive.
- Daily diary sections for breakfast, lunch, dinner, and snacks.
- Add-food flow with favorites and recent foods.
- Reusable saved meals that expand into diary entries.
- Duplicate-yesterday action for quick repeated logging.
- Daily calorie and macro totals compared with user goals.
- Today dashboard nutrition summary backed by the same diary totals.

### Exit criteria

1. Create a personal food and log two servings for lunch.
2. Create a reusable meal with multiple foods and log it for dinner.
3. Confirm daily calorie and macro totals use the selected serving counts.
4. Duplicate yesterday's diary and edit the copied entries independently.
5. Archive a food and confirm historical diary entries remain readable.

## 5. Phase 1C: Transformation Tracking

Status: implemented on `develop` and emulator-verified.

### Goal

Make weekly physical changes easy to record and compare privately.

### Deliverables

- Weight entry and optional body measurements.
- Derived BMI when height and weight exist.
- Weekly transformation entries with notes.
- Import front, left, right, back, and legs photos into app-private storage.
- Compare two selected weeks side by side for a selected angle.
- Empty state when an angle is missing from either week.
- Weekly summary with workouts completed, average logged calories, macro
  averages, and weight change.

### Exit criteria

1. Add height, weight, and body measurements and confirm BMI is calculated.
2. Add all five photo angles for one week and a subset for a later week.
3. Compare the two weeks and switch between angles.
4. Confirm a missing photo displays an empty state without blocking other
   angles.
5. Confirm all photos remain private to the application sandbox.

## 6. Phase 1D: Backup, Restore, and Reminders

### Goal

Protect local data and finish the dependable personal-use MVP.

### Deliverables

- Encrypted archive export containing Room data, media, a versioned manifest,
  and checksums.
- Restore preview showing backup timestamp, record counts, and media size.
- Explicit replace-data confirmation before restore.
- Transactional restore that keeps existing data if validation or import
  fails.
- Local workout reminders and weekly transformation-photo reminders.
- Settings for goals, units, reminder preferences, and rest timer.

### Exit criteria

1. Populate workouts, nutrition entries, measurements, and photos.
2. Export an encrypted backup through Android's document picker.
3. Reject restore with an incorrect passphrase or failed checksum.
4. Restore a valid archive and verify structured records and media.
5. Confirm local data remains intact after a rejected restore.
6. Confirm reminders can be enabled, disabled, and rescheduled.

At this point Keepfit is a complete offline MVP.

## 7. Phase 2A: Health Connect Steps

### Goal

Add optional daily steps without turning device integration into a core
dependency.

### Deliverables

- Health Connect availability detection.
- Read-only steps permission request.
- Daily steps on Today and weekly summaries when available.
- Clear unavailable, disabled, and denied-permission states.

### Exit criteria

1. Read daily steps on a supported device after permission is granted.
2. Keep the dashboard usable when Health Connect is unsupported.
3. Keep the dashboard usable when permission is denied or revoked.
4. Confirm no Health Connect records are duplicated into Room.

## 8. Phase 2B: Optional Ollama Assistant

### Goal

Add private, reviewable AI assistance after reliable tracking data exists.

### Deliverables

- Optional Ollama settings: endpoint, model, secure token, enable toggle.
- Connection test with a clear failure message.
- Chat interface backed by Ollama's chat API.
- Data summaries assembled locally before an explicit assistant request.
- Weekly progress summaries.
- Draft workout-plan proposals that require user review and confirmation before
  they replace or create a plan.
- Visible guidance that assistant responses are general fitness suggestions,
  not medical advice.

### Exit criteria

1. Disable AI and confirm every core workflow remains unchanged.
2. Configure a reachable Ollama endpoint and complete a chat request.
3. Handle an unreachable endpoint without losing user-entered text.
4. Generate a progress summary from selected local data.
5. Generate a draft plan and confirm no weekly plan changes before approval.
6. Apply an approved draft as a new weekly plan.

## 9. Later Considerations

Only consider these after sustained personal use demonstrates a clear need:

- Optional encrypted private-server sync.
- Barcode scanning with a remote food database.
- Additional workout metrics such as RPE and supersets.
- Exportable progress reports.

Avoid social, subscription, and community features unless the product goal
changes.

## 10. Market Review Notes

The MVP borrows small, proven workflows without copying the complexity of
general-purpose fitness products:

| Product | Useful pattern for Keepfit | Deferred complexity |
| --- | --- | --- |
| FitNotes | Fast workout logging, notes, calendar, rest timer, body tracking | Broader gym-tool depth |
| Hevy | Routines, progress tracking, measurements, progress photos | Social feed and community features |
| MyFitnessPal | Food diary, quick entry, reusable foods, macro totals | Remote database, barcode lookup, premium ecosystem |

### References

- [FitNotes on Google Play](https://play.google.com/store/apps/details?id=com.github.jamesgay.fitnotes)
- [Hevy features](https://www.hevyapp.com/features/)
- [MyFitnessPal free features](https://support.myfitnesspal.com/hc/en-us/articles/15457546881805-What-is-included-in-the-free-version)
