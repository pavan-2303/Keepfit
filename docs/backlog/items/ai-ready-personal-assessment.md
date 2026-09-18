# Backlog Initiative - AI-Ready Personal Assessment

> **Status:** Complete
> **Owner:** Keepfit
> **Last updated:** 2026-09-18

---

## Goal

Give offline and AI planning enough relevant, safety-conscious context to build
a realistic first week without requiring a pre-existing exercise catalogue.

## User Value

A person answers a focused assessment about goals, starting point, activity,
recovery, schedule, equipment, and limitations. They can then choose a Coach
only after OpenRouter is connected and approve a complete plan whose missing
exercise definitions are added to their personal catalogue.

## Why This Matters

The current setup asks several configuration questions but omits important
adherence and safety context. AI planning also stops when the personal
catalogue is empty, even though creating a reviewed set of exercises is a
natural part of generating the first plan.

## Scope

- Replace the initial training setup with a focused, progressive assessment.
- Capture activity, sleep, current build, routine constraints, and physical
  limitations alongside the existing goal, experience, schedule, and
  equipment answers.
- Keep sensitive answers local until the user explicitly requests an AI plan.
- Include bounded assessment context in the AI planning request.
- Allow a reviewed AI draft to define and create new personal exercises.
- Hide Coach persona selection until OpenRouter is connected.
- Keep manual and deterministic offline planning available without AI.

## Non-Scope

- Medical diagnosis, rehabilitation prescription, or clinical risk scoring.
- Automatic application of an AI plan without review.
- Cloud profiles, analytics, or uploading photos and raw health records.
- Copying Fitify screens, copy, or proprietary plan logic.

## Dependencies and Constraints

- Fitify is used only as a public product reference for goal, experience,
  available time, equipment, and basic profile inputs.
- AI-created exercises must pass strict validation and be persisted only when
  the reviewed plan is approved.
- Existing journey answers and plans must migrate without data loss.

## Product and System Impact

- Room journey schema, migration, export, backup compatibility, and tests.
- Profile and guided-setup Compose flows.
- Assistant prompt, tool contract, validation, review, and transactional apply.
- Coach connection gating and Compose tests.
- Architecture, privacy, and user guidance.

## Proposed Sprint Decomposition

| Planned Version | Sprint Goal | Estimated Effort | Notes |
|---|---|---:|---|
| [v0.24.0](../../versions/v0.24.0.md) | Ship assessment-driven planning and AI-created personal exercises | 8-12d | One connected onboarding and planning correction |

## Completion Criteria

- [x] Initial setup captures the inputs needed for a realistic plan.
- [x] AI planning works with an empty personal catalogue.
- [x] Approved AI-created exercises appear in the personal catalogue.
- [x] Coach personas are unavailable until OpenRouter is connected.
- [x] v0.24.0 is implemented, verified, and closed with a done record.

## Open Questions

- A future settings surface may allow editing all assessment answers outside
  the guided planning flow.
