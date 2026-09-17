# Settings, Reminders, Backup, and Restore

## Local Profiles

Tap the initial avatar in the app bar to switch profiles, add another person,
edit the active profile, or archive it. At least one profile must remain.
Plans, workout history, nutrition logs, goals, reviews, measurements, photos,
and ordinary preferences stay with the selected profile. Exercise and food
catalogues are intentionally shared on the device.

Encrypted export includes every profile, the active selection, and each
profile's settings. Restoring an older single-profile archive remains supported.

## Appearance and Accessibility

Open `Appearance and accessibility` to enable `Reduce motion`. The setting
removes nonessential navigation and state transitions without hiding progress,
completion, or any action. Android's system animation setting still applies to
native transitions. The preference applies to the device across local profiles
and is included in encrypted Keepfit backups.

## Nutrition Goals

Open the profile icon, choose `Goals and nutrition`, then save:

- calorie goal;
- protein goal;
- carbohydrate goal;
- fat goal.

These values are used as the midpoint of the flexible target ranges selected
from `Nutrition`.

## Units

Open `Training preferences` to switch:

- weight unit between `KG` and `LB`;
- measurement unit between `CM` and `IN`.

## Timers and Reminders

`Training preferences` also contains:

- rest timer duration for workouts;
- daily workout reminder time;
- weekly progress reminder day and time.

Reminders are local device notifications.

## Backup Export

Keepfit supports encrypted local backup export.

## Automatic Android Recovery

Android may back up and restore Keepfit's structured fitness records and
ordinary settings after reinstall or during a supported device transfer. This
is a convenience controlled by the device, backup service, network, quota, and
restore flow; Keepfit cannot guarantee when a backup happens or that Android
will offer it during reinstall.

Automatic recovery includes the Keepfit Room database and non-secret settings.
It excludes OpenRouter credentials and pending authorization, assistant drafts,
transformation photos, and imported exercise media. After automatic restore,
an excluded photo or demo is shown as unavailable rather than deleting its
record. A later encrypted full restore can recover that media.

If supported by Google Play services, `Connections` also offers a separate
`Recover access after reinstall` switch for OpenRouter. It is off by default,
does not put the token in the fitness backup, and verifies recovered access
before reuse. You can always leave it off and reconnect from Coach.

Typical export flow:

1. Open the profile icon and choose `Data and backup`.
2. Go to the encrypted backup section.
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
- Encrypted export is the complete recovery path; automatic Android recovery
  does not include private media or OpenRouter access.
- Nutrition lens, target flexibility, meal check-ins, foods, saved meals, and
  diary entries are included.
