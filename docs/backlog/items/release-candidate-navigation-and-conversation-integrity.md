# Backlog Initiative - Release-candidate Navigation and Conversation Integrity

> **Status:** Complete
> **Owner:** Keepfit
> **Last updated:** 2026-09-16

---

## Goal

Make every primary destination read as one purposeful workflow before the
first public build, and remove Coach states that look like user-created data
before a question is sent.

## User Value

A person can open Plan, Log, Progress, or Settings and immediately understand
the main task without duplicate headings, oversized gaps, or peer tabs that
expose implementation structure. Coach history contains only conversations the
person actually started.

## Why This Matters

The current release candidate is functionally broad but presents several
secondary tools as equally important primary content. This makes routine use
feel dense and unfinished, and the eager Coach persistence creates misleading
history.

## Scope

- Establish one compact shell header and consistent content inset.
- Make the weekly plan the default Plan surface.
- Move templates, the exercise catalogue, and workout history behind clear
  secondary navigation.
- Add template detail and edit flows with catalogue browsing.
- Keep the nutrition diary primary and move food and saved-meal libraries
  behind secondary navigation.
- Remove duplicate title blocks from Progress and Settings.
- Delay Coach conversation creation until the first question and separate
  retry state from the visible composer.
- Verify the primary destinations at normal and large text sizes.

## Non-Scope

- A new visual brand, custom font download, or animation system.
- New workout, nutrition, progress, or AI capabilities.
- A public GitHub release, store listing, or signed production publication.

## Dependencies and Constraints

- Preserve the existing five primary destinations and offline-first behavior.
- Preserve Room identifiers and history when editing templates.
- Keep v0.20.0 completion records immutable.

## Product and System Impact

- `app` shell title and content hierarchy.
- `feature:workouts`, `feature:nutrition`, `feature:transformation`,
  `feature:settings`, and `feature:assistant` presentation and state handling.
- Workout template repository update behavior without a schema change.

## Proposed Sprint Decomposition

| Planned Version | Sprint Goal | Estimated Effort | Notes |
|---|---|---|---|
| [v0.21.0](../../versions/v0.21.0.md) | Make primary screens release-ready and Coach history truthful | 8-12d | One complete release-candidate correction slice |

## Completion Criteria

- [x] Initiative outcome is fully delivered
- [x] v0.21.0 is implemented and verified
- [x] Follow-up work is closed or moved to a new initiative

## Open Questions

- Public repository and release automation remain a separate approved decision.
