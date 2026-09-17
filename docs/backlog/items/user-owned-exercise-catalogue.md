# Backlog Initiative - User-Owned Exercise Catalogue

> **Status:** Complete
> **Owner:** Keepfit
> **Last updated:** 2026-09-17

---

## Goal

Replace the preloaded third-party exercise library with a clear personal
catalogue and make exercises and templates practical to build incrementally.

## User Value

A person starts with an uncluttered exercise space, records the details that
matter for their own movements, creates a template from only a name, and adds
or removes exercises later without artificial limits. Exercise guidance is
reachable wherever an exercise appears in planning.

## Why This Matters

The large bundled dataset is difficult to trust and browse, while the current
exercise form hides useful fields and gives long descriptions the same control
as short labels. Requiring exercise selection during template creation also
front-loads a decision that belongs inside the template.

## Scope

- Stop packaging and seeding the third-party exercise dataset.
- Remove untouched bundled rows during upgrade while preserving edited,
  referenced, or media-backed records as personal exercises.
- Show all supported exercise fields with input controls suited to their
  content.
- Create empty templates by name and add exercises from template details.
- Allow the final exercise to be removed and clear unusable schedule entries.
- Open exercise details from template and weekly-plan paths.
- Keep optional starter and Coach planning compatible with a personal
  catalogue without restoring the bulk dataset.

## Non-Scope

- Remote exercise search, barcode-style lookup, or community catalogues.
- Advanced programming fields such as tempo, RPE, supersets, or rest per set.
- Rewriting completed workout history.

## Dependencies and Constraints

- Existing templates, workout history, and private exercise media must survive
  the catalogue cleanup.
- An empty template may be saved but must not be scheduled until it contains
  an exercise.
- Core workout tracking remains offline and account-free.

## Product and System Impact

- `core:database` startup, migration, schema export, and tests.
- `feature:workouts` model, repository, ViewModel, Compose UI, and tests.
- Optional Coach plan context and application validation.
- Architecture, user guide, README, and release lifecycle documentation.

## Proposed Sprint Decomposition

| Planned Version | Sprint Goal | Estimated Effort | Notes |
|---|---|---:|---|
| [v0.23.0](../../versions/v0.23.0.md) | Ship an empty-by-default personal catalogue and progressive template workflow | 8-12d | One cohesive product correction |

## Completion Criteria

- [x] Fresh installs contain no preloaded exercises.
- [x] Upgrades preserve user-owned and historically referenced exercise data.
- [x] Exercise forms, progressive templates, and cross-plan details are usable.
- [x] v0.23.0 is implemented, verified, and closed with an immutable done record.

## Open Questions

- A future import format for sharing personal exercise definitions remains a
  separate decision.
