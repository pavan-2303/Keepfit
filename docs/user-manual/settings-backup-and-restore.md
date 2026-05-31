# Settings, Reminders, Backup, and Restore

## Nutrition Goals

In `Settings`, you can save:

- calorie goal;
- protein goal;
- carbohydrate goal;
- fat goal.

These values are used for diary comparisons and summaries.

## Units

You can switch:

- weight unit between `KG` and `LB`;
- measurement unit between `CM` and `IN`.

## Timers and Reminders

Current options include:

- rest timer duration for workouts;
- daily workout reminder time;
- weekly progress reminder day and time.

Reminders are local device notifications.

## Backup Export

Keepfit supports encrypted local backup export.

Typical export flow:

1. Open `Settings`.
2. Go to the backup section.
3. Enter a passphrase.
4. Choose an export destination through the Android document picker.

The passphrase is required later for restore. Keepfit does not store it for
you.

## Restore

Restore replaces current local data only after the selected backup passes
validation.

Typical restore flow:

1. Select a backup file.
2. Enter the passphrase.
3. Preview the backup details.
4. Confirm restore only if the preview is correct.

If validation fails, existing local data should remain intact.

## Important Backup Notes

- Use a passphrase you will remember.
- Export backups regularly if the data matters to you.
- Test restore only when you understand it replaces local app data.
- Exercise media and transformation photos are included in valid backups.
