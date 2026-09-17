# Backlog Initiative - Practical Template Maintenance

> **Status:** Complete
> **Owner:** Keepfit
> **Last updated:** 2026-09-17

---

## Goal

Make routine workout-template maintenance direct enough to perform from the
template and exercise being changed.

## User Value

A person can rename a template, add exercises, adjust sets or repetition
targets, remove one exercise, or delete several templates without repeatedly
opening a full catalogue-selection editor.

## Why This Matters

The v0.21.0 template detail page made templates discoverable but still treated
small maintenance tasks as full-template reconstruction. That is slow and
error-prone during ordinary planning.

## Scope

- Put rename, add-exercise, and delete icon actions beside the template name.
- Put prescription edit and remove actions beside each exercise.
- Add an explicit multi-select mode and bulk delete to the template list.
- Preserve template identity, schedule assignments, targets, and historical
  workout snapshots.

## Non-Scope

- Exercise reordering, supersets, RPE, or advanced programming metrics.
- Editing completed workout history or dated workout occurrences.
- Changes to the Room schema.

## Dependencies and Constraints

- Continue using the existing v0.21.0 Plan information architecture.
- Keep destructive operations confirmed and bulk deletion all-or-nothing.

## Product and System Impact

- `feature:workouts` repository, ViewModel, Compose UI, and tests.
- Workout user guide and release lifecycle documentation.

## Proposed Sprint Decomposition

| Planned Version | Sprint Goal | Estimated Effort | Notes |
|---|---|---|---|
| [v0.22.0](../../versions/v0.22.0.md) | Make template maintenance direct and safely batchable | 4-6d | One focused correction increment |

## Completion Criteria

- [x] Initiative outcome is fully delivered.
- [x] v0.22.0 is implemented and verified.
- [x] Follow-up work is closed or moved to a new initiative.

## Open Questions

- Exercise reordering remains a later usability decision.
