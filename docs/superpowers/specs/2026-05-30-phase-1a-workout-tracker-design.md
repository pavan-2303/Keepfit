# Keepfit Phase 1A Workout Tracker Design

## Summary

Phase 1A replaces a paper gym notebook with a fast offline workout workflow.
It adds an exercise library, optional private demonstration media, workout
templates, a recurring weekly plan, today scheduling, active workout logging,
previous values, completion history, calendar markers, personal records, and
an optional vibration rest timer.

## Architecture

Add `feature:workouts` for workout repositories, ViewModels, and Compose screens.
Add `core:media` for app-private exercise media import. Extend `core:database`
with workout entities and DAOs and migrate Room schema version `1` to `2`.

Feature screens call ViewModels, ViewModels call repositories, and repositories
call DAOs or the focused media store. `app` wires dependencies and places the
workout navigation graph into the existing shell.

## Workout Model

- Exercises are searchable and editable. Archive instead of deleting them when
  history may reference them.
- Exercise media is optional. Imported MP4, WebM, or GIF files are copied into
  `files/media/exercises/` under generated file names.
- Templates contain ordered exercises with target sets, optional target reps,
  and notes.
- One active weekly plan repeats by weekday until replaced.
- Repeated exercises inside one template are allowed.
- Only one incomplete workout session may exist at a time.
- Completed sets store repetitions and kilograms. Bodyweight exercises allow
  zero kilograms.
- Previous values come from the latest completed logs for that exercise.
- Personal records are derived from completed sets: highest weight and highest
  repetitions.

## User Experience

The Workouts destination becomes a compact hub with tabs for Exercises,
Templates, Weekly Plan, and History. Exercise and template editors use focused
forms. The weekly plan assigns a template to a weekday. Today shows the planned
workout and starts or resumes the active session.

The active-session screen prioritizes current set entry: repetitions, weight,
notes, previous values, add set, complete workout, and a 90-second vibration
timer. History lists completed sessions and visible completion-date markers.

## Error Handling

- Reject blank names, non-positive target sets, negative reps, and negative
  weight.
- Reject unsupported or unreadable media imports without modifying the current
  attachment.
- Keep archived exercises readable in templates and completed history.
- Resume an existing incomplete session instead of creating a second one.
- Show recoverable empty states when no exercises, templates, schedule, or
  history exist.

## Testing

- DAO instrumentation tests cover exercise persistence, search, archive,
  templates, weekly plan lookup, session completion, history, and records.
- Migration instrumentation tests prove Phase 0 profiles survive schema
  migration `1 -> 2`.
- JVM tests cover exercise input validation and session set validation.
- Emulator verification covers creating an exercise, template, weekly
  assignment, session completion, history, and persistence after restart.

